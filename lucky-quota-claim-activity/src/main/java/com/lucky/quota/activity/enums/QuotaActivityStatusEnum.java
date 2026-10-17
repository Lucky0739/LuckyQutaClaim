package com.lucky.quota.activity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuotaActivityStatusEnum {

    NOT_STARTED(0, "未开始"),
    IN_PROGRESS(1, "进行中"),
    ENDED(2, "已结束");

    private final Integer code;
    private final String desc;
}
