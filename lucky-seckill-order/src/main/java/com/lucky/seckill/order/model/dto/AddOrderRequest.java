package com.lucky.seckill.order.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddOrderRequest {

    private Long goodsId;

    private Long skuId;
    /**
     * 购买的商品数量
     */
    private Integer buyGoodsNum;
}