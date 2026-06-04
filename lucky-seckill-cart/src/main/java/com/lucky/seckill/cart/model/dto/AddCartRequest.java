package com.lucky.seckill.cart.model.dto;

import com.lucky.seckill.cart.constant.CartConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddCartRequest {

    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    @NotNull(message = "规格ID不能为空")
    private Long skuId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    @Max(value = CartConstants.MAX_GOODS_COUNT_PER_ITEM, message = "单件商品购买数量不能超过999")
    private Integer goodsCount;
}
