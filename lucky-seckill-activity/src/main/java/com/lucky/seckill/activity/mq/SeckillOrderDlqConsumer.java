package com.lucky.seckill.activity.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucky.seckill.activity.enums.SeckillOrderStatusEnum;
import com.lucky.seckill.activity.service.SeckillFailedOrderService;
import com.lucky.seckill.activity.service.SeckillRedisService;
import com.lucky.seckill.activity.support.SeckillOrderProcessor;
import com.lucky.seckill.common.constant.RedisKeyConstants;
import com.lucky.seckill.common.exception.NonRetryableException;
import com.lucky.seckill.common.exception.RetryableException;
import com.lucky.seckill.common.model.entity.SeckillOrderMessage;
import com.lucky.seckill.common.mq.MqConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀订单死信队列消费者（手动ACK）。
 * <p>
 * 消费主队列处理失败的消息，进行延迟重试。超过最大重试次数后记录失败日志供人工介入。
 * <p>
 * 异常分级（与主消费者一致）：
 * <ul>
 *   <li>反序列化失败 → 直接 ACK（消息本身损坏）</li>
 *   <li>{@link NonRetryableException} → 终态处理 + ACK（重试永远不会成功）</li>
 *   <li>{@link RetryableException} → 检查 retryCount：
 *       <ul>
 *         <li>超过上限 → 终态处理 + ACK</li>
 *         <li>未超上限 → 重新投递 DLQ（递增 retryCount + 延迟级别）</li>
 *       </ul>
 *   </li>
 *   <li>未预期异常 → 保守策略：重新投递 DLQ</li>
 *   <li>投递 DLQ 失败 → {@link ConsumeConcurrentlyStatus#RECONSUME_LATER}</li>
 * </ul>
 * <p>
 * 延迟退避策略（RocketMQ delay level）：
 * <ul>
 *   <li>第1次：10s  第2次：30s  第3次：1m  第4次：2m  第5次：3m+</li>
 * </ul>
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = MqConstants.SECKILL_ORDER_DLQ_TOPIC,
        selectorExpression = MqConstants.SECKILL_ORDER_DLQ_TAG,
        consumerGroup = "seckill-order-dlq-consumer-group"
)
public class SeckillOrderDlqConsumer implements MessageListenerConcurrently {

    /** 最大重试次数（从 0 开始计数，>5 即至少重试了 6 次） */
    private static final int MAX_DLQ_RETRY = 5;

    @Resource
    private SeckillOrderProcessor seckillOrderProcessor;

    @Resource
    private SeckillRedisService seckillRedisService;

    @Resource
    private SeckillFailedOrderService seckillFailedOrderService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                     ConsumeConcurrentlyContext context) {
        // 逐条手动ACK：每条消息独立裁决
        boolean needReconsume = false;

        for (MessageExt msgExt : msgs) {
            ConsumeResult result = processOneMessage(msgExt);
            if (result == ConsumeResult.NEED_RECONSUME) {
                needReconsume = true;
            }
        }

        // 只有当 DLQ 重新投递失败时才返回 RECONSUME_LATER
        return needReconsume
                ? ConsumeConcurrentlyStatus.RECONSUME_LATER
                : ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    /**
     * 处理单条 DLQ 消息。
     *
     * @return NEED_RECONSUME 表示 DLQ 重新投递失败，需要 RocketMQ 重试
     */
    private ConsumeResult processOneMessage(MessageExt msgExt) {
        // ========== 反序列化失败 → 消息本身已损坏，直接 ACK ==========
        SeckillOrderMessage message = deserialize(msgExt);
        if (message == null) {
            log.error("==> 【死信队列】消息反序列化失败，直接ACK丢弃, msgId={}", msgExt.getMsgId());
            return ConsumeResult.ACK;
        }

        int retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();

        log.warn("==> 【死信队列】收到重试消息, msgId={}, orderNo={}, userId={}, retryCount={}/{}, dlqReconsumeTimes={}",
                msgExt.getMsgId(), message.getOrderNo(), message.getUserId(),
                retryCount, MAX_DLQ_RETRY, msgExt.getReconsumeTimes());

        try {
            // 委托给共享处理器
            seckillOrderProcessor.createOrder(message);

            // 重试成功 → 更新 Redis 状态为待支付(0)
            updateOrderStatus(message.getUserId(), message.getOrderNo(),
                    SeckillOrderStatusEnum.PENDING_PAYMENT.getCode());
            log.info("==> 【死信队列】重试成功, orderNo={}, retryCount={}",
                    message.getOrderNo(), retryCount);
            return ConsumeResult.ACK;

        } catch (NonRetryableException e) {
            // ===== A类：确定性失败 → 直接终态处理，不继续重试 =====
            log.error("==> 【死信队列】遇到不可重试异常，直接终态处理, orderNo={}, reason={}",
                    message.getOrderNo(), e.getMessage());
            handleFinalFailure(message, e.getMessage());
            return ConsumeResult.ACK;

        } catch (RetryableException e) {
            // ===== B类：瞬时失败 → 检查是否超过重试上限 =====
            if (retryCount >= MAX_DLQ_RETRY) {
                log.error("==> 【死信队列】重试次数已达上限({})，终态处理, orderNo={}, lastError={}",
                        MAX_DLQ_RETRY, message.getOrderNo(), e.getMessage());
                handleFinalFailure(message, e.getMessage());
                return ConsumeResult.ACK;
            }

            log.warn("==> 【死信队列】重试失败，将继续投递DLQ重试, orderNo={}, retryCount={}, reason={}",
                    message.getOrderNo(), retryCount, e.getMessage());
            return retryAgain(message);

        } catch (Exception e) {
            // ===== 未预期异常 → 保守策略 =====
            if (retryCount >= MAX_DLQ_RETRY) {
                log.error("==> 【死信队列】未知异常且已达重试上限，终态处理, orderNo={}",
                        message.getOrderNo(), e);
                handleFinalFailure(message, e.getMessage());
                return ConsumeResult.ACK;
            }
            return retryAgain(message);
        }
    }

    /** 单条消息的处理结果 */
    private enum ConsumeResult {
        ACK,
        NEED_RECONSUME
    }

    /**
     * 重新投递到死信队列，递增重试次数和延迟级别。
     */
    private ConsumeResult retryAgain(SeckillOrderMessage message) {
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
            return ConsumeResult.ACK;
        } catch (Exception e) {
            log.error("==> 【死信队列】重新投递失败, orderNo={}, 将触发RocketMQ重试",
                    message.getOrderNo(), e);
            return ConsumeResult.NEED_RECONSUME;
        }
    }

    /**
     * 最终失败处理（DLQ 重试全部耗尽）：
     * <ol>
     *   <li>更新 Redis 订单状态为 -2（创建失败），前端轮询时展示具体失败原因</li>
     *   <li>回滚 Redis 库存（终态兜底，带幂等保护不会重复回滚）</li>
     *   <li><b>持久化到 t_seckill_failed_order 表，等待人工介入</b></li>
     *   <li>记录详细日志 + 告警</li>
     * </ol>
     */
    private void handleFinalFailure(SeckillOrderMessage message, String lastError) {
        int retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();

        // 1. 更新 Redis 状态为失败，让前端能感知（不再展示"订单创建中"）
        updateOrderStatus(message.getUserId(), message.getOrderNo(),
                RedisKeyConstants.ORDER_STATUS_FAILED);

        // 2. 终态回滚 Redis 库存，确保不丢库存（Lua 脚本带幂等保护）
        try {
            seckillRedisService.rollbackStock(
                    message.getActivityId(),
                    message.getUserId(),
                    message.getGoodsId(),
                    message.getSkuId(),
                    message.getBuyNum(),
                    message.getOrderNo());
            log.info("==> 【死信队列】终态回滚Redis库存完成, orderNo={}", message.getOrderNo());
        } catch (Exception e) {
            log.error("==> 【死信队列】终态回滚Redis库存失败, orderNo={}", message.getOrderNo(), e);
        }

        // 3. 持久化到失败订单表，等待管理员手动处理
        seckillFailedOrderService.saveFailedOrder(message, lastError);

        // 4. 详细日志 + 告警
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
                        ==  lastError:     {}
                        ==  Redis状态已置为-2（创建失败），库存已回滚
                        ==  已落库至 t_seckill_failed_order，请管理员后台手动处理！
                        ========================================""",
                message.getOrderNo(),
                message.getUserId(),
                message.getActivityId(),
                message.getGoodsId(),
                message.getSkuId(),
                message.getSeckillGoodsId(),
                message.getBuyNum(),
                message.getSeckillPrice(),
                retryCount,
                lastError);
    }

    /**
     * 从 MessageExt 反序列化 SeckillOrderMessage。
     */
    private SeckillOrderMessage deserialize(MessageExt msgExt) {
        try {
            String json = new String(msgExt.getBody(), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, SeckillOrderMessage.class);
        } catch (Exception e) {
            log.error("==> 【死信队列】反序列化 SeckillOrderMessage 失败, msgId={}", msgExt.getMsgId(), e);
            return null;
        }
    }

    private int selectDelayLevel(int retryCount) {
        return switch (retryCount) {
            case 1 -> 3;   // 10s
            case 2 -> 4;   // 30s
            case 3 -> 5;   // 1m
            case 4 -> 6;   // 2m
            default -> 7;  // 3m+
        };
    }

    /**
     * 更新 Redis 中的订单处理状态（仅 Redis，不落库）。
     */
    private void updateOrderStatus(Long userId, String orderNo, Integer status) {
        try {
            String redisKey = RedisKeyConstants.seckillOrderStatusKey(userId, orderNo);
            stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(status),
                    RedisKeyConstants.SECKILL_ORDER_STATUS_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("==> 【死信队列】更新Redis订单状态失败, orderNo={}, status={}", orderNo, status, e);
        }
    }
}
