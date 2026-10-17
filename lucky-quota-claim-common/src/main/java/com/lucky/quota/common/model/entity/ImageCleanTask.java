package com.lucky.quota.common.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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
@TableName("t_image_clean_task")
public class ImageCleanTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String imageUrl;

    private String businessType;

    private Integer status;

    private Integer expireMinute;

    private LocalDateTime deleteTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
