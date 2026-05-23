package com.lucky.seckill.cart.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartVO {
    private Long id;

    private Long goodsId;

    private Long skuId;

    private String goodsName;

    private String goodsImg;

    private String skuSpecName;

    private BigDecimal salePrice;

    private Integer goodsCount;

    private Integer selected;

    private LocalDateTime createTime;
}
