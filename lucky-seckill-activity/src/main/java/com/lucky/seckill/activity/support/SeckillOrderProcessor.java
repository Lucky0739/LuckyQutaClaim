package com.lucky.seckill.activity.support;

import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.activity.enums.SeckillOrderStatusEnum;
import com.lucky.seckill.activity.mapper.SeckillGoodsMapper;
import com.lucky.seckill.activity.mapper.SeckillOrderMapper;
import com.lucky.seckill.activity.model.entity.SeckillGoods;
import com.lucky.seckill.activity.model.entity.SeckillOrder;
import com.lucky.seckill.activity.service.SeckillRedisService;
import com.lucky.seckill.common.exception.NonRetryableException;
import com.lucky.seckill.common.exception.RetryableException;
import com.lucky.seckill.common.model.entity.SeckillOrderMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    /**
     * 创建秒杀订单，受事务保护。
     * <p>
     * 该方法可被重复调用（主消费者 / DLQ 消费者），通过 orderNo 幂等。
     *
     * @param message 秒杀订单消息
     * @throws NonRetryableException 不可恢复的错误（商品不存在等），消费者应直接 ACK
     * @throws RetryableException    可恢复的错误（乐观锁冲突等），消费者应投递 DLQ
     */
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(SeckillOrderMessage message) {
        String orderNo = message.getOrderNo();

        // ========== 1. 幂等检查：订单已存在则直接返回 ==========
        SeckillOrder existOrder = seckillOrderMapper.selectByOrderNo(orderNo);
        if (existOrder != null) {
            log.warn("==> 秒杀订单已存在，幂等跳过, orderNo={}", orderNo);
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
}
