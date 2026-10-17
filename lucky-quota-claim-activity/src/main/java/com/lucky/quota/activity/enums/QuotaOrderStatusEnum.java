package com.lucky.quota.activity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuotaOrderStatusEnum {

    PENDING_PAYMENT(0, "待确认领取"),
    PAID(1, "待发放"),
    SHIPPED(2, "已发放"),
    RECEIVED(3, "已到货"),
    CANCELLED(4, "已取消"),
    REFUNDED(5, "已退款"),
    CLOSED(6, "已关闭");

    private final Integer code;
    private final String desc;
}
