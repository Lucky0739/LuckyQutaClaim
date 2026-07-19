package com.lucky.seckill.order.controller;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.order.service.SentinelDemoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sentinel 演示接口（毕设答辩用）
 * <p>
 * 正常：GET /order/sentinel/demo
 * 模拟异常降级：GET /order/sentinel/demo?fail=true （连续调用 5 次以上触发熔断）
 * 模拟慢调用降级：GET /order/sentinel/demo?slow=2000
 * 模拟限流：30 秒内连续请求超过 3 次（第 4 次返回 SENTINEL_BLOCK）
 */
@RestController
@RequestMapping("/order/sentinel")
@Slf4j
public class SentinelDemoController {

    @Resource
    private SentinelDemoService sentinelDemoService;

    @GetMapping("/demo")
    public Response<String> demo(
            @RequestParam(value = "fail", defaultValue = "false") boolean fail,
            @RequestParam(value = "slow", defaultValue = "0") long slowMs) throws InterruptedException {
        log.info("==> Sentinel 演示请求, fail={}, slowMs={}", fail, slowMs);
        return sentinelDemoService.demo(fail, slowMs);
    }
}
