package com.lucky.quota.activity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaGoodsApplyQueryRequest {

    private Long activityId;

    private Integer auditStatus;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
