package com.lucky.quota.order.constant;

public final class OrderConstants {

    private OrderConstants() {
    }

    /**
     * 待确认领取超时时间（分钟）。到期由 RocketMQ 延迟消息按单关闭，不扫表。
     */
    public static final int PAY_TIMEOUT_MINUTES = 10;

    /** 限量申领单号前缀（库存已在活动侧扣减，订单侧不再预占/重复扣减） */
    public static final String QUOTA_ORDER_NO_PREFIX = "SK";

    public static boolean isQuotaOrderNo(String orderNo) {
        return orderNo != null && orderNo.startsWith(QUOTA_ORDER_NO_PREFIX);
    }
}
