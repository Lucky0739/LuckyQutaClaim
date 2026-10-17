package com.lucky.quota.activity.support;

import cn.hutool.core.util.IdUtil;
import com.lucky.quota.activity.enums.QuotaOrderStatusEnum;
import com.lucky.quota.activity.mapper.QuotaGoodsMapper;
import com.lucky.quota.activity.mapper.QuotaOrderMapper;
import com.lucky.quota.activity.model.entity.QuotaGoods;
import com.lucky.quota.activity.model.entity.QuotaOrder;
import com.lucky.quota.activity.service.QuotaRedisService;
import com.lucky.quota.common.constant.RedisKeyConstants;
import com.lucky.quota.common.exception.NonRetryableException;
import com.lucky.quota.common.exception.RetryableException;
import com.lucky.quota.common.model.entity.QuotaOrderMessage;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.feign.GoodsFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 限量申领单创建处理器（主消费者与 DLQ 消费者共享逻辑）。
 * <p>
 * <b>事务边界（Seata AT 全局事务）</b>：
 * 限量申领库存扣减发生在 activity 本地库（t_seckill_goods.seckill_stock），
 * 物资总库存扣减走 Feign 调 goods 服务（t_goods_sku.stock_num）——两者是独立的库。
 * {@link GlobalTransactional} 把它们包成一个全局事务，任一环节失败整体回滚：
 * 本地分支靠 undo_log 反向补偿，goods 分支由那边的 undo_log 补偿。
 * 这样解决的是「限量申领卖出的货没占用物资总库存」导致的超卖问题。
 * <p>
 * 异常分级策略：
 * <ul>
 *   <li><b>幂等跳过</b>：申领单已消费 → 直接 return，不抛异常</li>
 *   <li><b>不可重试</b>（{@link NonRetryableException}）：限量物资不存在、物资总库存不足
 *       → 回滚 Redis 库存 + 直接 ACK，不进 DLQ</li>
 *   <li><b>可重试</b>（{@link RetryableException}）：乐观锁冲突、Feign 瞬时故障、DB 连接超时等
 *       → <b>一律不回滚 Redis 库存</b>。消息会重试，而重试成功的那次不会再扣 Redis，
 *         此时若已回滚就会造成 Redis 库存虚增（比 DB 多，闸门放行了实际卖不出的量）。
 *         Redis 库存在 DLQ 终态由 {@code QuotaOrderDlqConsumer#handleFinalFailure} 统一归还。</li>
 * </ul>
 */
@Component
@Slf4j
public class QuotaOrderProcessor {

    @Resource
    private QuotaOrderMapper seckillOrderMapper;

    @Resource
    private QuotaGoodsMapper seckillGoodsMapper;

    @Resource
    private QuotaRedisService seckillRedisService;

    /** 物资服务：扣减 t_goods_sku 的总库存，与限量申领库存保持同步 */
    @Resource
    private GoodsFeignClient goodsFeignClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** Redis 消费标记前缀，TTL 与申领状态 key 一致 */
    private static final String ORDER_CONSUMED_PREFIX = "seckill:order:consumed:";

    /**
     * 创建限量申领单，受事务保护。
     * <p>
     * 幂等策略（Redis → MySQL 两级检查）：
     * <ol>
     *   <li>先查 Redis 消费标记 key，命中直接跳过</li>
     *   <li>Redis miss → 查 MySQL orderNo 唯一索引，命中则回填 Redis 标记</li>
     *   <li>都 miss → 执行创建，成功后写 Redis 消费标记</li>
     * </ol>
     * 该方法可被重复调用（主消费者 / DLQ 消费者），通过 orderNo 幂等。
     *
     * @param message 限量申领单消息
     * @throws NonRetryableException 不可恢复的错误（限量物资不存在、物资总库存不足），消费者应直接 ACK
     * @throws RetryableException    可恢复的错误（乐观锁冲突、Feign 瞬时故障等），消费者应投递 DLQ
     */
    @GlobalTransactional(name = "seckill-order-create", rollbackFor = Exception.class)
    public void createOrder(QuotaOrderMessage message) {
        String orderNo = message.getOrderNo();

        // ========== 1. 幂等检查：Redis 消费标记 → MySQL 兜底 ==========
        if (isOrderConsumed(message.getUserId(), orderNo)) {
            log.warn("==> 限量申领单已消费，幂等跳过, orderNo={}", orderNo);
            return;
        }

        // ========== 2. 确定性失败：物资不存在 → 不可重试 ==========
        QuotaGoods seckillGoods = seckillGoodsMapper.selectByPrimaryKey(message.getSeckillGoodsId());
        if (seckillGoods == null) {
            log.error("==> 限量物资不存在，回滚 Redis 库存并标记不可重试, seckillGoodsId={}, orderNo={}",
                    message.getSeckillGoodsId(), orderNo);
            rollbackRedisStock(message);
            throw new NonRetryableException(
                    "限量物资不存在, seckillGoodsId=" + message.getSeckillGoodsId(), orderNo);
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

        // ========== 4. 扣减物资总库存（跨服务，纳入全局事务） ==========
        deductGoodsStock(message);

        // ========== 5. 创建申领单 ==========
        LocalDateTime now = LocalDateTime.now();
        QuotaOrder order = QuotaOrder.builder()
                .id(IdUtil.getSnowflakeNextId())
                .userId(message.getUserId())
                .activityId(message.getActivityId())
                .goodsId(message.getGoodsId())
                .skuId(message.getSkuId())
                .orderNo(orderNo)
                .seckillPrice(message.getSeckillPrice())
                .goodsName(message.getGoodsName())
                .goodsImg(message.getGoodsImg())
                .status(QuotaOrderStatusEnum.PENDING_PAYMENT.getCode())
                .expireTime(message.getExpireTime())
                .isDeleted(0)
                .createTime(now)
                .updateTime(now)
                .build();

        try {
            seckillOrderMapper.insertSelective(order);
        } catch (Exception e) {
            // insert 失败通常是 DB 连接问题，交给 DLQ 重试。
            // 这里刻意不回滚 Redis 库存：重试成功的那次不会再扣一次 Redis，
            // 若此处回滚，Redis 库存会比 DB 多，后续请求能过 Lua 闸门却建不了单。
            // 全局事务回滚会把本地的 seckill_stock 和 goods 的总库存一并撤销，
            // 重试时重新扣减，Redis 与 DB 始终对齐。
            log.error("==> 申领单插入失败，等待 DLQ 重试（不回滚 Redis 库存）, orderNo={}", orderNo, e);
            throw new RetryableException("申领单插入失败: " + e.getMessage(), orderNo, e);
        }

        // 申领单落库成功 → 写 Redis 消费标记，后续重复消费直接命中跳过
        markOrderConsumed(orderNo);

        log.info("==> 限量申领单创建成功, orderNo={}, orderId={}", orderNo, order.getId());
    }

    /**
     * 回滚 Redis 库存（带幂等保护，重复回滚会被 Lua 脚本拦截）。
     */
    private void rollbackRedisStock(QuotaOrderMessage message) {
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

    /**
     * 扣减物资总库存（远程调用 goods 服务）。
     * <p>
     * 限量申领库存 {@code t_seckill_goods.seckill_stock} 和物资总库存 {@code t_goods_sku.stock_num}
     * 是两套独立数据。不做这一步的话，限量申领卖出的货不会占用物资库存，
     * 同一个 SKU 同时在正常售卖时就会超卖。
     * <p>
     * 本方法运行在外层全局事务内，goods 侧会注册成一个分支事务——
     * 后续申领单落库失败时，全局回滚会连同这边的扣减一起撤销。
     *
     * @throws RetryableException    Feign 瞬时故障（网络超时等），消息进 DLQ 重试
     * @throws NonRetryableException 物资总库存不足，重试也不会成功，回滚 Redis 库存后直接 ACK
     */
    private void deductGoodsStock(QuotaOrderMessage message) {
        Response<Boolean> result;
        try {
            result = goodsFeignClient.deductStock(message.getSkuId(), message.getBuyNum());
        } catch (Exception e) {
            // 超时等瞬时故障：不确定 goods 那边扣没扣，但全局事务回滚会把它撤销，
            // 所以这里可以放心重试，不需要额外补偿。
            log.warn("==> 扣减物资总库存异常，等待 DLQ 重试, orderNo={}, skuId={}",
                    message.getOrderNo(), message.getSkuId(), e);
            throw new RetryableException("扣减物资库存异常: " + e.getMessage(), message.getOrderNo(), e);
        }

        if (result == null || !result.isSuccess() || !Boolean.TRUE.equals(result.getData())) {
            // goods 侧的条件更新 stock_num >= n 没有命中 → 总库存确实不够，
            // 这是确定性失败，重试不会成功，所以在这里归还 Redis 库存后直接终态。
            log.error("==> 物资总库存不足，回滚 Redis 库存, orderNo={}, skuId={}, buyNum={}",
                    message.getOrderNo(), message.getSkuId(), message.getBuyNum());
            rollbackRedisStock(message);
            throw new NonRetryableException(
                    "物资总库存不足, skuId=" + message.getSkuId() + ", buyNum=" + message.getBuyNum(),
                    message.getOrderNo());
        }

        log.info("==> 扣减物资总库存成功, orderNo={}, skuId={}, buyNum={}",
                message.getOrderNo(), message.getSkuId(), message.getBuyNum());
    }

    // ==================== 消费幂等：Redis → MySQL 两级检查 ====================

    /**
     * 判断申领单是否已被消费。
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
     * 标记申领单已被消费（写入 Redis，TTL 30 分钟）。
     */
    private void markOrderConsumed(String orderNo) {
        try {
            String consumedKey = ORDER_CONSUMED_PREFIX + orderNo;
            stringRedisTemplate.opsForValue().set(
                    consumedKey, "1",
                    RedisKeyConstants.QUOTA_ORDER_STATUS_TTL_MINUTES,
                    TimeUnit.MINUTES);
        } catch (Exception e) {
            // Redis 写入失败不影响主流程，下次靠 MySQL 兜底
            log.warn("==> 写 Redis 消费标记失败, orderNo={}", orderNo, e);
        }
    }
}
