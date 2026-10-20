package com.lucky.quota.order.constant;

public final class OrderConstants {

    private OrderConstants() {
    }

    /**
     * 待确认领取超时时间（分钟）。到期由 RocketMQ 延迟消息按单关闭，不扫表。
     */
    public static final int PAY_TIMEOUT_MINUTES = 10;
}
