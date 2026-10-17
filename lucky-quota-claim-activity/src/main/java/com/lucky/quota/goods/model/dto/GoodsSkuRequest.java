package com.lucky.quota.goods.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoodsSkuRequest {
    private String skuSpecName;

    private String skuImg;

    private BigDecimal marketPrice;

    private BigDecimal salePrice;

    private Integer stockNum;
}
