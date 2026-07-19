package com.lucky.seckill.cart.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 购物车列表（含汇总）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartListVO {

    private List<CartVO> items;

    private CartSummaryVO summary;
}
