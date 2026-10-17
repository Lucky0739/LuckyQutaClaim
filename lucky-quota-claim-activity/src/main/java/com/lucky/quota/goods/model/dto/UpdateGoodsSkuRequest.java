package com.lucky.quota.goods.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新 SKU：skuId 为空表示新增规格
 */
@Data
public class UpdateGoodsSkuRequest {

    private Long skuId;

    private String skuSpecName;

    private String skuImg;

    private BigDecimal marketPrice;

    private BigDecimal salePrice;

    private Integer stockNum;
}
