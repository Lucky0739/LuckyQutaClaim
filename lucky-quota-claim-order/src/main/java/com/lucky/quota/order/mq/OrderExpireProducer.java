package com.lucky.quota.order.mq;

import com.lucky.quota.common.mq.MqConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 申领过期检查的延迟消息生产者（延迟队列替代定时任务扫表）。
 * <p>
 * 申领成功后发送初始延迟消息，到期由 {@link OrderExpireConsumer} 检查申领单是否过期。
 * <p>
 * <b>为什么单独抽成 Bean</b>：这两个发送方法原先写在 {@link OrderExpireConsumer} 里，
 * 而消费者本身又依赖 {@code OrderService}，导致
 * {@code OrderServiceImpl → OrderExpireConsumer → OrderService} 形成循环依赖，
 * Spring Boot 2.6+ 默认禁止循环引用，应用直接启动失败。
 * 把生产端职责拆出来，依赖方向变成单向：
 * <pre>
 *   OrderServiceImpl      → OrderExpireProducer （只发消息）
 *   OrderExpireConsumer   → OrderService + OrderExpireProducer （消费并回调）
 * </pre>
 * 这也和项目里 {@code QuotaOrderProducer}、{@code MerchantApplyProducer} 的组织方式一致。
 */
@Component
@Slf4j
public class OrderExpireProducer {

    /** 初始延迟级别：4 = 30s（RocketMQ 预设：1s 5s 10s 30s 1m … 10m） */
    private static final int INITIAL_DELAY_LEVEL = 4;

    /** 递进延迟级别：5 = 1m，直到 10 分钟超时窗口到期 */
    private static final int PROGRESSIVE_DELAY_LEVEL = 5;

    private static final String DESTINATION =
            MqConstants.ORDER_EXPIRE_CHECK_TOPIC + ":" + MqConstants.ORDER_EXPIRE_CHECK_TAG;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 发送初始过期检查延迟消息（申领成功后 30s 首次检查，未到期则按 1m 递进至 10min）。
     */
    public void sendExpireCheckMessage(String orderNo) {
        send(orderNo, INITIAL_DELAY_LEVEL, "30s");
    }

    /**
     * 重新发送延迟消息，1 分钟后再次检查（申领单尚未过期时由消费者调用）。
     */
    public void resendDelayMessage(String orderNo) {
        send(orderNo, PROGRESSIVE_DELAY_LEVEL, "1m");
    }

    private void send(String orderNo, int delayLevel, String delayDesc) {
        try {
            Message<String> msg = MessageBuilder.withPayload(orderNo).build();
            rocketMQTemplate.syncSend(DESTINATION, msg, 3000, delayLevel);
            log.info("==> 【过期检查】已发送延迟消息, orderNo={}, delayLevel={}", orderNo, delayDesc);
        } catch (Exception e) {
            // 发送失败只记日志，避免影响申领主链路；可按单号人工补发延迟消息
            log.error("==> 【过期检查】发送延迟消息失败, orderNo={}, delayLevel={}", orderNo, delayDesc, e);
        }
    }
}
