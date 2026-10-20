package com.lucky.quota.order.model.dto;

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
     * 申领单号（可选，精确查询）
     */
    private String orderNo;

    /**
     * 申领状态（可选）
     */
    private Integer status;

    /**
     * 物资名称（模糊查询）
     */
    private String goodsName;
}
