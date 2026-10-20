package com.lucky.quota.order.model.dto;

import lombok.Data;

@Data
public class UpdateOrderRequest {

    /**
     * 申领单号
     */
    private String orderNo;

    /**
     * 申领状态
     */
    private Integer status;

    /**
     * 确认领取时间（确认领取时填写）
     */
    private String payTime;
}
