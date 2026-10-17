package com.lucky.quota.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 收藏类型
 */
@Getter
@AllArgsConstructor
public enum CollectTypeEnum {

    GOODS("goods"),
    SHOP("shop");

    private final String code;
}
