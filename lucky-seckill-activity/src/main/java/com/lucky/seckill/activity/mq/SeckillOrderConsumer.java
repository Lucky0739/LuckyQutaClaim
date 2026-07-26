package com.lucky.seckill.activity.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucky.seckill.activity.enums.SeckillOrderStatusEnum;
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
 * 秒杀下单消息消费者（手动ACK模式）：异步落库并扣减数据库库存。
 * <p>
 * <b>手动ACK策略（逐条消息独立确认）</b>：
 * 不再依赖 RocketMQ 的 {@link ConsumeConcurrentlyStatus#RECONSUME_LATER} 做批量重投，
 * 而是对每条消息单独裁决：
 * <ul>
 *   <li><b>处理成功</b> → 更新 Redis 状态为待支付，该条消息 ACK</li>
 *   <li><b>反序列化失败</b> → 消息损坏，直接跳过（ACK）</li>
 *   <li><b>{@link NonRetryableException}</b> → 确定性失败，跳过（ACK），Redis 库存已在 Processor 中回滚</li>
 *   <li><b>{@link RetryableException}</b>（瞬时失败）→ 直接升级到 DLQ 延迟重试，本条消息 ACK</li>
 *   <li><b>未预期异常</b> → 同 RetryableException 处理（保守策略）</li>
 * </ul>
 * <p>
 * 这样做的优势：
 * <ol>
 *   <li>批量消费中不会因为一条失败而重投整批（已成功的消息幂等跳过，但浪费算力）</li>
 *   <li>每条消息的命运明确可追溯（ACK / DLQ），不会出现"半批成功半批重投"的模糊状态</li>
 *   <li>DLQ 自带延迟级别退避，替代 RocketMQ 内置的重试机制</li>
 * </ol>
 * <p>
 * <b>DLQ 延迟退避策略</b>：
 * <ul>
 *   <li>第1次：10s  第2次：30s  第3次：1m  第4次：2m  第5次：3m+</li>
 * </ul>
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = MqConstants.SECKILL_ORDER_TOPIC,
        selectorExpression = MqConstants.SECKILL_ORDER_TAG,
        consumerGroup = "seckill-order-consumer-group"
)
public class SeckillOrderConsumer implements MessageListenerConcurrently {

    @Resource
    private SeckillOrderProcessor seckillOrderProcessor;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 主队列快速重试次数上限（超过后升级到 DLQ） */
    private static final int MAX_MAIN_RETRY = 2;

    /** 订单过期检查延迟级别：14 = 10m（RocketMQ 预设） */
    private static final int EXPIRE_CHECK_DELAY_LEVEL = 14;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                     ConsumeConcurrentlyContext context) {
        // 逐条手动ACK：每条消息独立裁决，处理完毕后统一返回 CONSUME_SUCCESS
        for (MessageExt msgExt : msgs) {
            processOneMessage(msgExt);
        }
        // 所有消息已手动处理（成功ACK / 失败升级DLQ），统一返回成功
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    /**
     * 处理单条消息：成功则更新Redis状态，失败则按类型分别处理。
     */
    private void processOneMessage(MessageExt msgExt) {
        // ========== 反序列化失败 → 消息损坏，直接 ACK ==========
        SeckillOrderMessage message = deserialize(msgExt);
        if (message == null) {
            log.error("==> 消息反序列化失败，直接ACK丢弃, msgId={}", msgExt.getMsgId());
            return;
        }

        int reconsumeTimes = msgExt.getReconsumeTimes();

        log.info("==> 收到秒杀下单消息, msgId={}, orderNo={}, userId={}, reconsumeTimes={}",
                msgExt.getMsgId(), message.getOrderNo(),
                message.getUserId(), reconsumeTimes);

        try {
            seckillOrderProcessor.createOrder(message);

            // 成功 → 更新 Redis 状态为待支付
            updateOrderStatus(message.getUserId(), message.getOrderNo(),
                    SeckillOrderStatusEnum.PENDING_PAYMENT.getCode());

            // 发送延迟消息，到期检查订单是否过期
            sendExpireCheckMessage(message.getOrderNo());

            log.info("==> 订单创建成功(主队列), orderNo={}", message.getOrderNo());

        } catch (NonRetryableException e) {
            // ===== 确定性失败 → 直接ACK，绝不重试 =====
            // Redis 库存已在 createOrder 中回滚
            log.error("==> 订单创建失败(不可重试), orderNo={}, reason={}",
                    message.getOrderNo(), e.getMessage());

        } catch (RetryableException e) {
            // ===== 瞬时失败 → 主队列快速重试 → 耗尽后升级 DLQ =====
            handleRetryableFailure(message, e.getMessage());

        } catch (Exception e) {
            // ===== 未知异常 → 保守策略，同可重试处理 =====
            handleRetryableFailure(message, e.getMessage());
        }
    }

    /**
     * 可重试失败处理：先在主队列快速重试 2 次（10s、30s），
     * 还失败再升级到 DLQ 长延迟重试。
     */
    private void handleRetryableFailure(SeckillOrderMessage message, String reason) {
        int mainRetryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();

        if (mainRetryCount < MAX_MAIN_RETRY) {
            log.warn("==> 主队列第{}次重试, orderNo={}, reason={}",
                    mainRetryCount + 1, message.getOrderNo(), reason);
            retryInMainQueue(message);
        } else {
            log.warn("==> 主队列重试耗尽，升级到DLQ, orderNo={}, retryCount={}, reason={}",
                    message.getOrderNo(), mainRetryCount, reason);
            escalateToDlq(message);
        }
    }

    /**
     * 主队列快速重试：递增重试次数，带回延迟重新投递到主队列。
     */
    private void retryInMainQueue(SeckillOrderMessage message) {
        try {
            int retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
            message.setRetryCount(retryCount + 1);

            String destination = String.format("%s:%s",
                    MqConstants.SECKILL_ORDER_TOPIC, MqConstants.SECKILL_ORDER_TAG);
            int delayLevel = selectMainDelayLevel(message.getRetryCount());

            Message<SeckillOrderMessage> msg = MessageBuilder.withPayload(message).build();
            SendResult result = rocketMQTemplate.syncSend(destination, msg, 3000, delayLevel);

            log.info("==> 主队列重试已投递, orderNo={}, retryCount={}, delayLevel={}, result={}",
                    message.getOrderNo(), message.getRetryCount(), delayLevel, result);
        } catch (Exception e) {
            log.error("==> 主队列重试投递失败，降级到DLQ, orderNo={}", message.getOrderNo(), e);
            escalateToDlq(message);
        }
    }

    /**
     * 主队列快速重试延迟级别（短退避）。
     */
    private int selectMainDelayLevel(int retryCount) {
        return switch (retryCount) {
            case 1 -> 3;   // 10s
            case 2 -> 4;   // 30s
            default -> 3;
        };
    }

    /**
     * 将失败消息升级到死信队列（延迟级别按 retryCount 递增）。
     * <p>
     * 投递 DLQ 本身失败时记录日志并放弃（避免无限递归），
     * 此时消息会丢失，但原始请求信息已记录在日志中可供追溯。
     */
    private void escalateToDlq(SeckillOrderMessage message) {
        try {
            int retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
            message.setRetryCount(retryCount + 1);

            String destination = String.format("%s:%s",
                    MqConstants.SECKILL_ORDER_DLQ_TOPIC, MqConstants.SECKILL_ORDER_DLQ_TAG);
            int delayLevel = selectDelayLevel(message.getRetryCount());

            Message<SeckillOrderMessage> msg = MessageBuilder.withPayload(message).build();
            SendResult result = rocketMQTemplate.syncSend(destination, msg, 3000, delayLevel);

            log.info("==> 已升级到死信队列, orderNo={}, retryCount={}, delayLevel={}, result={}",
                    message.getOrderNo(), message.getRetryCount(), delayLevel, result);
        } catch (Exception e) {
            log.error("==> 投递死信队列失败(消息可能丢失！), orderNo={}",
                    message.getOrderNo(), e);
        }
    }

    private SeckillOrderMessage deserialize(MessageExt msgExt) {
        try {
            String json = new String(msgExt.getBody(), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, SeckillOrderMessage.class);
        } catch (Exception e) {
            log.error("==> 反序列化失败, msgId={}", msgExt.getMsgId(), e);
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
     * 发送订单过期检查延迟消息（10 分钟后首次检查，替代定时任务扫表）。
     */
    private void sendExpireCheckMessage(String orderNo) {
        try {
            String destination = MqConstants.ORDER_EXPIRE_CHECK_TOPIC + ":"
                    + MqConstants.ORDER_EXPIRE_CHECK_TAG;
            org.springframework.messaging.Message<String> msg =
                    org.springframework.messaging.support.MessageBuilder.withPayload(orderNo).build();
            rocketMQTemplate.syncSend(destination, msg, 3000, EXPIRE_CHECK_DELAY_LEVEL);
            log.info("==> 已发送过期检查延迟消息, orderNo={}, delayLevel=10m", orderNo);
        } catch (Exception e) {
            log.error("==> 发送过期检查延迟消息失败, orderNo={}", orderNo, e);
        }
    }

    private void updateOrderStatus(Long userId, String orderNo, Integer status) {
        try {
            String redisKey = RedisKeyConstants.seckillOrderStatusKey(userId, orderNo);
            stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(status),
                    RedisKeyConstants.SECKILL_ORDER_STATUS_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("==> 更新Redis订单状态失败, orderNo={}, status={}", orderNo, status, e);
        }
    }
}
