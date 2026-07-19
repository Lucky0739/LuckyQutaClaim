package com.lucky.seckill.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待清理图片任务
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageCleanTask {

    private Long id;

    private String imageUrl;

    private String businessType;

    private Integer status;

    private Integer expireMinute;

    private LocalDateTime deleteTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
