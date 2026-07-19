package com.lucky.seckill.order.model.dto;

import lombok.Data;

@Data
public class UpdateOrderRequest {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 支付时间（支付成功时填写）
     */
    private String payTime;
}
