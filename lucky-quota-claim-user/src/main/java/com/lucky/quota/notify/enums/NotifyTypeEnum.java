package com.lucky.quota.notify.enums;

import com.lucky.quota.common.constant.NotifyTypeConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotifyTypeEnum {

    MERCHANT_APPLY(NotifyTypeConstants.MERCHANT_APPLY, "发放单位申请", false),
    LIKE(NotifyTypeConstants.LIKE, "点赞", true),
    AUDIT(NotifyTypeConstants.AUDIT, "收藏", true),
    AUDIT_NOTIFY(NotifyTypeConstants.AUDIT_NOTIFY, "审核通知", true),
    PAY_SUCCESS(NotifyTypeConstants.PAY_SUCCESS, "申领确认", true),
    ORDER_DELIVERY(NotifyTypeConstants.ORDER_DELIVERY, "发放通知", true),
    ORDER_RECEIVE(NotifyTypeConstants.ORDER_RECEIVE, "确认到货", true),
    QUOTA_GOODS_APPLY(NotifyTypeConstants.QUOTA_GOODS_APPLY, "限量投放申请", false);

    private final Integer code;
    private final String name;
    private final boolean userVisible;

    public static String getNameByCode(Integer code) {
        if (code == null) {
            return "未知类型";
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .map(NotifyTypeEnum::getName)
                .findFirst()
                .orElse("未知类型");
    }

    public static List<NotifyTypeEnum> userVisibleTypes() {
        return Arrays.stream(values())
                .filter(NotifyTypeEnum::isUserVisible)
                .collect(Collectors.toList());
    }

    /**
     * 管理员审核类消息（发放单位申请、限量投放申请等）
     */
    public static List<NotifyTypeEnum> adminApplyTypes() {
        return Arrays.stream(values())
                .filter(item -> !item.isUserVisible())
                .collect(Collectors.toList());
    }

    public static boolean isUserVisibleType(Integer code) {
        if (code == null) {
            return false;
        }
        return Arrays.stream(values())
                .anyMatch(item -> item.code.equals(code) && item.isUserVisible());
    }

    public static boolean isAdminApplyType(Integer code) {
        if (code == null) {
            return false;
        }
        return Arrays.stream(values())
                .anyMatch(item -> item.code.equals(code) && !item.isUserVisible());
    }
}
