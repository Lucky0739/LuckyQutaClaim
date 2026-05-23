package com.lucky.seckill.goods.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SkuInfoVO {
    private Long skuId;

    private String skuSpecName;

    private String skuImg;

    private BigDecimal marketPrice;

    private BigDecimal salePrice;

    private Integer stockNum;

    private Integer salesNum;
}