package com.lucky.quota.order.enums;

/**
 * 申领状态枚举
 */
public enum OrderStatusEnum {

    PENDING_PAYMENT(0, "待确认领取"),
    PAID(1, "待发放"),
    SHIPPED(2, "已发放"),
    RECEIVED(3, "已到货"),
    CANCELLED(4, "已取消"),
    REFUNDED(5, "已退款"),
    EXPIRED(6, "已关闭");

    private final Integer code;
    private final String desc;

    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) {
            return "未知";
        }
        for (OrderStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status.desc;
            }
        }
        return "未知";
    }
}
