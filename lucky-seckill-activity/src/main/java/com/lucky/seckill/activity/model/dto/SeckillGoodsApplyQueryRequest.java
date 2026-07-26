package com.lucky.seckill.activity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillGoodsApplyQueryRequest {

    private Long activityId;

    private Integer auditStatus;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
