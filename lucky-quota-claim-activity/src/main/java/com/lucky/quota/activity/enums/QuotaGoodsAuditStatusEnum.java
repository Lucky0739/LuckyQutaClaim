package com.lucky.quota.activity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuotaGoodsAuditStatusEnum {

    PENDING(1, "待审核"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝");

    private final Integer code;
    private final String desc;
}
