package com.lucky.seckill.cart.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Cart {
    private Long id;

    private Long userId;

    private Long goodsId;

    private Long skuId;

    private Integer goodsCount;

    private Integer selected;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
