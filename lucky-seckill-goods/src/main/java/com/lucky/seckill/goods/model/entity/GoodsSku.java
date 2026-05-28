package com.lucky.seckill.goods.model.entity;

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
public class GoodsSku {
    private Long id;

    private Long goodsId;

    private String skuSpecName;

    private String skuImg;

    private BigDecimal marketPrice;

    private BigDecimal salePrice;

    private Integer stockNum;

    private Integer salesNum;

    private Integer status;

    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}