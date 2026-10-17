package com.lucky.quota.activity.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaResultVO {

    private String orderNo;

    /** 申领状态：-1=处理中，0=待确认领取 */
    private Integer status;

    private String message;
}
