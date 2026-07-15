package com.lucky.seckill.activity.mq;

import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.activity.enums.SeckillOrderStatusEnum;
import com.lucky.seckill.activity.mapper.SeckillGoodsMapper;
import com.lucky.seckill.activity.mapper.SeckillOrderMapper;
import com.lucky.seckill.activity.model.entity.SeckillGoods;
import com.lucky.seckill.activity.model.entity.SeckillOrder;
import com.lucky.seckill.activity.service.SeckillRedisService;
import com.lucky.seckill.common.model.entity.SeckillOrderMessage;
import com.lucky.seckill.common.mq.MqConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 秒杀订单死信队列消费者
 * <p>
 * 消费主队列处理失败的消息，进行延迟重试。超过最大重试次数后记录失败日志供人工介入。
 * <p>
 * 延迟策略（RocketMQ delay level）：
 * <ul>
 *   <li>第1次重试：10s 后</li>
 *   <li>第2次重试：30s 后</li>
 *   <li>第3次重试：1m 后</li>
 *   <li>第4次重试：2m 后</li>
 *   <li>第5次重试：3m 后（最后一次）</li>
 * </ul>
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = MqConstants.SECKILL_ORDER_DLQ_TOPIC,
        selectorExpression = MqConstants.SECKILL_ORDER_DLQ_TAG,
        consumerGroup = "seckill-order-dlq-consumer-group"
)
public class SeckillOrderDlqConsumer implements RocketMQListener<SeckillOrderMessage> {

    /** 最大重试次数（DLQ 自身重试） */
    private static final int MAX_DLQ_RETRY = 5;

    @Resource
    private SeckillOrderMapper seckillOrderMapper;

    @Resource
    private SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private SeckillRedisService seckillRedisService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(SeckillOrderMessage message) {
        int retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();

        log.warn("==> 【死信队列】收到重试消息, orderNo={}, userId={}, retryCount={}, maxRetry={}",
                message.getOrderNo(), message.getUserId(), retryCount, MAX_DLQ_RETRY);

        if (retryCount > MAX_DLQ_RETRY) {
            log.error("==> 【死信队列】重试次数已达上限({})，放弃处理, orderNo={}, userId={}, goodsId={}",
                    MAX_DLQ_RETRY, message.getOrderNo(), message.getUserId(), message.getGoodsId());
            handleFinalFailure(message);
            return;
        }

        try {
            processDlqOrder(message);
            log.info("==> 【死信队列】重试成功, orderNo={}, retryCount={}",
                    message.getOrderNo(), retryCount);
        } catch (Exception e) {
            log.error("==> 【死信队列】重试失败, orderNo={}, retryCount={}",
                    message.getOrderNo(), retryCount, e);
            retryAgain(message);
        }
    }

    /**
     * 订单创建逻辑（与主消费者一致），受事务保护
     */
    @Transactional(rollbackFor = Exception.class)
    public void processDlqOrder(SeckillOrderMessage message) {
        // 幂等：检查订单是否已存在
        SeckillOrder existOrder = seckillOrderMapper.selectByOrderNo(message.getOrderNo());
        if (existOrder != null) {
            log.warn("==> 【死信队列】秒杀订单已存在，无需重试, orderNo={}", message.getOrderNo());
            return;
        }

        SeckillGoods seckillGoods = seckillGoodsMapper.selectByPrimaryKey(message.getSeckillGoodsId());
        if (seckillGoods == null) {
            log.error("==> 【死信队列】秒杀商品不存在，回滚 Redis 库存, seckillGoodsId={}", message.getSeckillGoodsId());
            rollbackRedis(message);
            throw new RuntimeException("秒杀商品不存在");
        }

        int deductRows = seckillGoodsMapper.deductStock(
                seckillGoods.getId(),
                message.getBuyNum(),
                seckillGoods.getVersion(),
                LocalDateTime.now()
        );
        if (deductRows <= 0) {
            log.error("==> 【死信队列】数据库库存扣减失败，回滚 Redis 库存, orderNo={}", message.getOrderNo());
            rollbackRedis(message);
            throw new RuntimeException("数据库库存扣减失败");
        }

        LocalDateTime now = LocalDateTime.now();
        SeckillOrder order = SeckillOrder.builder()
                .id(IdUtil.getSnowflakeNextId())
                .userId(message.getUserId())
                .activityId(message.getActivityId())
                .goodsId(message.getGoodsId())
                .skuId(message.getSkuId())
                .orderNo(message.getOrderNo())
                .seckillPrice(message.getSeckillPrice())
                .goodsName(message.getGoodsName())
                .goodsImg(message.getGoodsImg())
                .status(SeckillOrderStatusEnum.PENDING_PAYMENT.getCode())
                .expireTime(message.getExpireTime())
                .isDeleted(0)
                .createTime(now)
                .updateTime(now)
                .build();

        seckillOrderMapper.insertSelective(order);

        log.info("==> 【死信队列】秒杀订单创建成功, orderNo={}, orderId={}", message.getOrderNo(), order.getId());
    }

    /**
     * 重新投递到死信队列，递增重试次数和延迟级别
     */
    private void retryAgain(SeckillOrderMessage message) {
        try {
            int retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
            message.setRetryCount(retryCount + 1);

            String destination = String.format("%s:%s",
                    MqConstants.SECKILL_ORDER_DLQ_TOPIC, MqConstants.SECKILL_ORDER_DLQ_TAG);

            int delayLevel = selectDelayLevel(message.getRetryCount());

            Message<SeckillOrderMessage> msg = MessageBuilder.withPayload(message).build();
            SendResult result = rocketMQTemplate.syncSend(destination, msg, 3000, delayLevel);

            log.warn("==> 【死信队列】重新投递重试消息, orderNo={}, retryCount={}, delayLevel={}, result={}",
                    message.getOrderNo(), message.getRetryCount(), delayLevel, result);
        } catch (Exception e) {
            log.error("==> 【死信队列】重新投递失败，消息将丢失, orderNo={}", message.getOrderNo(), e);
            handleFinalFailure(message);
        }
    }

    /**
     * 最终失败处理：记录日志，等待人工介入
     */
    private void handleFinalFailure(SeckillOrderMessage message) {
        int retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
        log.error("""
                        ========================================
                        ==  【死信队列】秒杀订单创建最终失败
                        ==  orderNo:       {}
                        ==  userId:        {}
                        ==  activityId:    {}
                        ==  goodsId:       {}
                        ==  skuId:         {}
                        ==  seckillGoodsId:{}
                        ==  buyNum:        {}
                        ==  seckillPrice:  {}
                        ==  retryCount:    {}
                        ==  请人工核查并处理！
                        ========================================""",
                message.getOrderNo(),
                message.getUserId(),
                message.getActivityId(),
                message.getGoodsId(),
                message.getSkuId(),
                message.getSeckillGoodsId(),
                message.getBuyNum(),
                message.getSeckillPrice(),
                retryCount);
    }

    /**
     * 根据重试次数选择 RocketMQ 延迟级别
     * <ul>
     *   <li>1: 1s    2: 5s    3: 10s   4: 30s   5: 1m</li>
     *   <li>6: 2m    7: 3m    8: 4m    9: 5m   10: 6m</li>
     *   <li>11: 7m  12: 8m   13: 9m   14: 10m  15: 20m</li>
     *   <li>16: 30m 17: 1h   18: 2h</li>
     * </ul>
     */
    private int selectDelayLevel(int retryCount) {
        return switch (retryCount) {
            case 1 -> 3;   // 10s
            case 2 -> 4;   // 30s
            case 3 -> 5;   // 1m
            case 4 -> 6;   // 2m
            default -> 7;  // 3m+
        };
    }

    private void rollbackRedis(SeckillOrderMessage message) {
        seckillRedisService.rollbackStock(
                message.getActivityId(),
                message.getUserId(),
                message.getGoodsId(),
                message.getSkuId(),
                message.getBuyNum(),
                message.getOrderNo()
        );
    }
}
