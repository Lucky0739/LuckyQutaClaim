package com.lucky.seckill.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 待清理图片业务类型
 */
@Getter
@AllArgsConstructor
public enum ImageCleanBusinessTypeEnum {

    GOODS("goods"),
    SECKILL("seckill"),
    AVATAR("avatar");

    private final String code;
}
