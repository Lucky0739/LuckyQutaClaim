package com.lucky.seckill.order.scheduler;

import com.lucky.seckill.order.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 待支付订单超时自动关闭定时任务
 */
@Component
@Slf4j
public class OrderExpireCloseScheduler {

    @Resource
    private OrderService orderService;

    @Scheduled(fixedDelay = 30000)
    public void closeExpiredOrders() {
        try {
            orderService.closeExpiredOrders();
        } catch (Exception e) {
            log.error("==> 定时任务：关闭超时未支付订单失败", e);
        }
    }
}
