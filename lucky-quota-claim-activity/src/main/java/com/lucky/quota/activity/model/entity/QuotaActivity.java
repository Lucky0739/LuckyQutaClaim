package com.lucky.quota.activity.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_seckill_activity")
public class QuotaActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String activityName;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    /**
     * 活动状态 0=未开始 1=进行中 2=已结束
     */
    private Integer status;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
