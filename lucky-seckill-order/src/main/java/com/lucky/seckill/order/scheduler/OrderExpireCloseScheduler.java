package com.lucky.seckill.order.scheduler;

import com.lucky.seckill.order.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 待支付订单超时自动关闭定时任务（已废弃，改为 RocketMQ 延迟队列实现）。
 * <p>
 * 保留作为兜底方案：极端情况下如果延迟消息全部丢失，可重新启用此定时任务批量修复。
 *
 * @deprecated 已被 {@link com.lucky.seckill.order.mq.OrderExpireConsumer} 替代
 */
@Deprecated
@Component
@Slf4j
public class OrderExpireCloseScheduler {

    @Resource
    private OrderService orderService;

    /**
     * 定时扫表已禁用，改为 RocketMQ 延迟队列按单精准触发。
     * 如需重新启用：取消下面注释并将 fixedDelay 调整回 30000。
     */
    // @Scheduled(fixedDelay = 30000)
    public void closeExpiredOrders() {
        try {
            orderService.closeExpiredOrders();
        } catch (Exception e) {
            log.error("==> 定时任务：关闭超时未支付订单失败", e);
        }
    }
}
