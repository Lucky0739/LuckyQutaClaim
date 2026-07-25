package com.lucky.seckill.activity.support;

import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.activity.enums.SeckillOrderStatusEnum;
import com.lucky.seckill.activity.mapper.SeckillGoodsMapper;
import com.lucky.seckill.activity.mapper.SeckillOrderMapper;
import com.lucky.seckill.activity.model.entity.SeckillGoods;
import com.lucky.seckill.activity.model.entity.SeckillOrder;
import com.lucky.seckill.activity.service.SeckillRedisService;
import com.lucky.seckill.common.constant.RedisKeyConstants;
import com.lucky.seckill.common.exception.NonRetryableException;
import com.lucky.seckill.common.exception.RetryableException;
import com.lucky.seckill.common.model.entity.SeckillOrderMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀订单创建处理器（主消费者与 DLQ 消费者共享逻辑）。
 * <p>
 * 异常分级策略：
 * <ul>
 *   <li><b>幂等跳过</b>：订单已存在 → 直接 return，不抛异常</li>
 *   <li><b>不可重试</b>（{@link NonRetryableException}）：商品不存在、数据校验失败
 *       → 回滚 Redis 库存 + 直接 ACK，不进 DLQ</li>
 *   <li><b>可重试</b>（{@link RetryableException}）：乐观锁冲突、DB 连接超时等
 *       → 乐观锁冲突不回滚 Redis（库存数据是正确的），其他异常需回滚后重试</li>
 * </ul>
 */
@Component
@Slf4j
public class SeckillOrderProcessor {

    @Resource
    private SeckillOrderMapper seckillOrderMapper;

    @Resource
    private SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private SeckillRedisService seckillRedisService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** Redis 消费标记前缀，TTL 与订单状态 key 一致 */
    private static final String ORDER_CONSUMED_PREFIX = "seckill:order:consumed:";

    /**
     * 创建秒杀订单，受事务保护。
     * <p>
     * 幂等策略（Redis → MySQL 两级检查）：
     * <ol>
     *   <li>先查 Redis 消费标记 key，命中直接跳过</li>
     *   <li>Redis miss → 查 MySQL orderNo 唯一索引，命中则回填 Redis 标记</li>
     *   <li>都 miss → 执行创建，成功后写 Redis 消费标记</li>
     * </ol>
     * 该方法可被重复调用（主消费者 / DLQ 消费者），通过 orderNo 幂等。
     *
     * @param message 秒杀订单消息
     * @throws NonRetryableException 不可恢复的错误（商品不存在等），消费者应直接 ACK
     * @throws RetryableException    可恢复的错误（乐观锁冲突等），消费者应投递 DLQ
     */
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(SeckillOrderMessage message) {
        String orderNo = message.getOrderNo();

        // ========== 1. 幂等检查：Redis 消费标记 → MySQL 兜底 ==========
        if (isOrderConsumed(message.getUserId(), orderNo)) {
            log.warn("==> 秒杀订单已消费，幂等跳过, orderNo={}", orderNo);
            return;
        }

        // ========== 2. 确定性失败：商品不存在 → 不可重试 ==========
        SeckillGoods seckillGoods = seckillGoodsMapper.selectByPrimaryKey(message.getSeckillGoodsId());
        if (seckillGoods == null) {
            log.error("==> 秒杀商品不存在，回滚 Redis 库存并标记不可重试, seckillGoodsId={}, orderNo={}",
                    message.getSeckillGoodsId(), orderNo);
            rollbackRedisStock(message);
            throw new NonRetryableException(
                    "秒杀商品不存在, seckillGoodsId=" + message.getSeckillGoodsId(), orderNo);
        }

        // ========== 3. 乐观锁扣减 DB 库存 → 冲突时可重试 ==========
        int deductRows = seckillGoodsMapper.deductStock(
                seckillGoods.getId(),
                message.getBuyNum(),
                seckillGoods.getVersion(),
                LocalDateTime.now()
        );

        if (deductRows <= 0) {
            // 乐观锁冲突：Redis 库存数据是正确的，不要回滚！
            // 等一段时间后重试，版本号更新后就能成功
            log.warn("==> 数据库库存扣减失败（乐观锁冲突），不进行 Redis 回滚，等待 DLQ 重试, orderNo={}", orderNo);
            throw new RetryableException(
                    "乐观锁冲突, orderNo=" + orderNo + ", seckillGoodsId=" + seckillGoods.getId(), orderNo);
        }

        // ========== 4. 创建订单 ==========
        LocalDateTime now = LocalDateTime.now();
        SeckillOrder order = SeckillOrder.builder()
                .id(IdUtil.getSnowflakeNextId())
                .userId(message.getUserId())
                .activityId(message.getActivityId())
                .goodsId(message.getGoodsId())
                .skuId(message.getSkuId())
                .orderNo(orderNo)
                .seckillPrice(message.getSeckillPrice())
                .goodsName(message.getGoodsName())
                .goodsImg(message.getGoodsImg())
                .status(SeckillOrderStatusEnum.PENDING_PAYMENT.getCode())
                .expireTime(message.getExpireTime())
                .isDeleted(0)
                .createTime(now)
                .updateTime(now)
                .build();

        try {
            seckillOrderMapper.insertSelective(order);
        } catch (Exception e) {
            // insert 失败通常是 DB 连接问题，可重试，但需要回滚 Redis
            log.error("==> 订单插入失败，回滚 Redis 库存, orderNo={}", orderNo, e);
            rollbackRedisStock(message);
            throw new RetryableException("订单插入失败: " + e.getMessage(), orderNo, e);
        }

        // 订单落库成功 → 写 Redis 消费标记，后续重复消费直接命中跳过
        markOrderConsumed(orderNo);

        log.info("==> 秒杀订单创建成功, orderNo={}, orderId={}", orderNo, order.getId());
    }

    /**
     * 回滚 Redis 库存（带幂等保护，重复回滚会被 Lua 脚本拦截）。
     */
    private void rollbackRedisStock(SeckillOrderMessage message) {
        try {
            seckillRedisService.rollbackStock(
                    message.getActivityId(),
                    message.getUserId(),
                    message.getGoodsId(),
                    message.getSkuId(),
                    message.getBuyNum(),
                    message.getOrderNo()
            );
        } catch (Exception e) {
            log.error("==> 回滚 Redis 库存异常, orderNo={}", message.getOrderNo(), e);
        }
    }

    // ==================== 消费幂等：Redis → MySQL 两级检查 ====================

    /**
     * 判断订单是否已被消费。
     * <ol>
     *   <li>Redis 消费标记命中 → 直接返回 true（热路径，O(1)）</li>
     *   <li>Redis miss → MySQL 查 orderNo 唯一索引（兜底，防 Redis 丢失）</li>
     *   <li>MySQL 命中 → 回填 Redis 标记，返回 true</li>
     *   <li>都 miss → 返回 false，执行创建流程</li>
     * </ol>
     */
    private boolean isOrderConsumed(Long userId, String orderNo) {
        // 第一级：Redis 消费标记
        String consumedKey = ORDER_CONSUMED_PREFIX + orderNo;
        if ("1".equals(stringRedisTemplate.opsForValue().get(consumedKey))) {
            return true;
        }

        // 第二级：MySQL 唯一索引兜底
        if (seckillOrderMapper.selectByOrderNo(orderNo) != null) {
            // MySQL 有但 Redis 没有 → 回填 Redis 标记（可能是 Redis 超时或重启导致丢失）
            markOrderConsumed(orderNo);
            return true;
        }

        return false;
    }

    /**
     * 标记订单已被消费（写入 Redis，TTL 30 分钟）。
     */
    private void markOrderConsumed(String orderNo) {
        try {
            String consumedKey = ORDER_CONSUMED_PREFIX + orderNo;
            stringRedisTemplate.opsForValue().set(
                    consumedKey, "1",
                    RedisKeyConstants.SECKILL_ORDER_STATUS_TTL_MINUTES,
                    TimeUnit.MINUTES);
        } catch (Exception e) {
            // Redis 写入失败不影响主流程，下次靠 MySQL 兜底
            log.warn("==> 写 Redis 消费标记失败, orderNo={}", orderNo, e);
        }
    }
}
