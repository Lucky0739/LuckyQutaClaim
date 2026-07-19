package com.lucky.seckill.cart.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 购物车汇总信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartSummaryVO {

    /** 购物车行数 */
    private Integer totalLines;

    /** 商品总件数 */
    private Integer totalQuantity;

    /** 已选行数 */
    private Integer selectedLines;

    /** 已选商品件数 */
    private Integer selectedQuantity;

    /** 已选商品金额 */
    private BigDecimal selectedAmount;

    /** 是否全选（无商品时为 false） */
    private Boolean allSelected;
}
