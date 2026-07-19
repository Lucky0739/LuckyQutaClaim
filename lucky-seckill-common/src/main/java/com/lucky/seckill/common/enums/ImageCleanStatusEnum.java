package com.lucky.seckill.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 图片清理任务状态
 */
@Getter
@AllArgsConstructor
public enum ImageCleanStatusEnum {

    PENDING(0),
    CLEANED(1),
    FAILED(2);

    private final int code;
}
