package com.lucky.seckill.order.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddOrderRequest {

    private Long goodsId;

    private Long skuId;

    /**
     * 用户收货地址ID
     */
    private Long userAddressId;

    /**
     * 购买的商品数量
     */
    private Integer buyGoodsNum;
}