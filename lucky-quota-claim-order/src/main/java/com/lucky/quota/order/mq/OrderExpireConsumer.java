package com.lucky.quota.order.mq;

import com.lucky.quota.common.mq.MqConstants;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.order.enums.OrderStatusEnum;
import com.lucky.quota.order.model.entity.Order;
import com.lucky.quota.order.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 申领过期检查消费者（延迟队列替代定时任务扫表）。
 * <p>
 * 申领单创建后由 {@link OrderExpireProducer} 发送延迟消息到此队列，到期检查：
 * <ul>
 *   <li>非待确认领取 → 直接返回（已确认/已取消/已关闭）</li>
 *   <li>待确认 + 已过期 → 关闭申领单 + 释放库存（Seata 全局事务）</li>
 *   <li>待确认 + 未过期 → 重新发送延迟消息（1 分钟后再次检查）</li>
 * </ul>
 * <p>
 * 延迟策略：初始延迟 30 秒（RocketMQ level 4），后续每次 1 分钟递进，直至 10 分钟超时关单。
 * 相比定时任务扫表的优势：
 * <ul>
 *   <li>按单精准触发，避免逐表扫描</li>
 *   <li>无状态，天然支持多实例</li>
 * </ul>
 * <p>
 * <b>关于接口选择</b>：这里用的是 Spring 风格 {@link RocketMQListener}，
 * 与 {@code @RocketMQMessageListener} 注解配套。注意不能实现原生 RocketMQ 的
 * {@code MessageListenerConcurrently} —— 那个接口只适用于手工 new 出来的
 * {@code DefaultMQPushConsumer}，而 {@code RocketMQMessageListenerBeanPostProcessor}
 * 在注册容器时会强校验 {@code instanceof RocketMQListener || instanceof RocketMQReplyListener}，
 * 不满足就直接抛 {@code IllegalStateException}，应用启动失败。
 * 限量申领那两个消费者因为需要「逐条手动 ACK」才用原生接口，
 * 所以它们走的是手工注册 {@code DefaultMQPushConsumer} 的路子，见 activity 模块的
 * {@code QuotaMqConsumerConfig}。
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = MqConstants.ORDER_EXPIRE_CHECK_TOPIC,
        selectorExpression = MqConstants.ORDER_EXPIRE_CHECK_TAG,
        consumerGroup = "order-expire-check-consumer-group"
)
public class OrderExpireConsumer implements RocketMQListener<String> {

    @Resource
    private OrderService orderService;

    /** 延迟消息发送已抽到独立 Bean，避免与 OrderServiceImpl 形成循环依赖 */
    @Resource
    private OrderExpireProducer orderExpireProducer;

    @Override
    public void onMessage(String orderNo) {
        log.info("==> 【过期检查】收到延迟消息, orderNo={}", orderNo);

        Order order = getOrder(orderNo);
        if (order == null) {
            log.warn("==> 【过期检查】申领单不存在，跳过, orderNo={}", orderNo);
            return;
        }

        if (!OrderStatusEnum.PENDING_PAYMENT.getCode().equals(order.getStatus())) {
            log.info("==> 【过期检查】申领单已确认或关闭，跳过, orderNo={}, status={}",
                    orderNo, order.getStatus());
            return;
        }

        // 已过期 → 关闭申领单
        if (order.getExpireTime() != null && !LocalDateTime.now().isBefore(order.getExpireTime())) {
            log.info("==> 【过期检查】申领单已过期，执行关闭, orderNo={}, expireTime={}",
                    orderNo, order.getExpireTime());
            try {
                orderService.closeExpiredOrder(orderNo);
                log.info("==> 【过期检查】申领过期关闭成功, orderNo={}", orderNo);
            } catch (Exception e) {
                // 关单失败不回抛：抛出去会走 RocketMQ 内置重试，时机不可控。记日志并 ACK。
                log.error("==> 【过期检查】申领过期关闭失败, orderNo={}", orderNo, e);
            }
            return;
        }

        // 未过期 → 继续延迟检查
        log.info("==> 【过期检查】申领单尚未过期，继续延迟等待, orderNo={}, expireTime={}",
                orderNo, order.getExpireTime());
        orderExpireProducer.resendDelayMessage(orderNo);
    }

    private Order getOrder(String orderNo) {
        try {
            Response<Order> response = orderService.getOrderByOrderNo(orderNo);
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.error("==> 【过期检查】查询申领单失败, orderNo={}", orderNo, e);
        }
        return null;
    }
}
