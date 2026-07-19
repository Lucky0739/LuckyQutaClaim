package com.lucky.seckill.order.model.dto;

import lombok.Data;

@Data
public class OrderQueryRequest {

    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 订单号（可选，精确查询）
     */
    private String orderNo;

    /**
     * 订单状态（可选）
     */
    private Integer status;

    /**
     * 商品名称（模糊查询）
     */
    private String goodsName;
}
