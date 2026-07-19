package com.lucky.seckill.order.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.order.config.SentinelDemoConfig;
import com.lucky.seckill.order.support.SentinelDemoRateLimitWindow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Sentinel 降级 / 限流演示
 */
@Service
@Slf4j
public class SentinelDemoService {

    @SentinelResource(
            value = "orderSentinelDemo",
            fallback = "demoFallback",
            blockHandler = "demoBlockHandler"
    )
    public Response<String> demo(boolean fail, long slowMs) throws InterruptedException {
        long windowMs = SentinelDemoConfig.FLOW_WINDOW_SECONDS * 1000L;
        if (!SentinelDemoRateLimitWindow.tryPass(SentinelDemoConfig.FLOW_MAX_COUNT, windowMs)) {
            return demoBlockHandler(fail, slowMs, new FlowException("orderSentinelDemo"));
        }
        if (fail) {
            throw new RuntimeException("模拟订单服务异常");
        }
        if (slowMs > 0) {
            Thread.sleep(slowMs);
        }
        return Response.success("订单服务正常");
    }

    @SuppressWarnings("unused")
    public Response<String> demoFallback(boolean fail, long slowMs, Throwable t) {
        log.warn("[Sentinel] 触发降级, reason: {}", t.getMessage());
        return Response.fail("SENTINEL_DEGRADE", "订单服务已降级，请稍后重试（Sentinel 演示）");
    }

    @SuppressWarnings("unused")
    public Response<String> demoBlockHandler(boolean fail, long slowMs, BlockException ex) {
        log.warn("[Sentinel] 触发限流, rule: {}", ex.getRule());
        return Response.fail(
                "SENTINEL_BLOCK",
                "请求过于频繁，" + SentinelDemoConfig.FLOW_WINDOW_SECONDS + " 秒内最多 "
                        + SentinelDemoConfig.FLOW_MAX_COUNT + " 次（演示）");
    }
}
