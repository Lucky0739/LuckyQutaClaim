package com.lucky.quota.order.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddOrderRequest {

    private Long goodsId;

    private Long skuId;

    /**
     * 用户领取地址ID
     */
    private Long userAddressId;

    /**
     * 申领的物资数量
     */
    private Integer buyGoodsNum;
}