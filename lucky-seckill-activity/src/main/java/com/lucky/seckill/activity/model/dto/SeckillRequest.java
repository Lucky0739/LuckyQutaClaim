package com.lucky.seckill.activity.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillRequest {

    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    @NotNull(message = "SKU ID不能为空")
    private Long skuId;
}
