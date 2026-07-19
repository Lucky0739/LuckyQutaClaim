package com.lucky.seckill.order.support;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 演示用滑动窗口限流（FlowRule 仅支持按秒 QPS，无法配置「N 秒 M 次」，故毕设演示单独实现）
 */
public final class SentinelDemoRateLimitWindow {

    private static final Deque<Long> ACCESS_TIMES = new ArrayDeque<>();

    private SentinelDemoRateLimitWindow() {
    }

    /**
     * @param maxCount  窗口内允许的最大请求数
     * @param windowMs  统计窗口时长（毫秒）
     * @return 是否放行
     */
    public static synchronized boolean tryPass(int maxCount, long windowMs) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;
        while (!ACCESS_TIMES.isEmpty() && ACCESS_TIMES.peekFirst() <= windowStart) {
            ACCESS_TIMES.pollFirst();
        }
        if (ACCESS_TIMES.size() >= maxCount) {
            return false;
        }
        ACCESS_TIMES.addLast(now);
        return true;
    }
}
