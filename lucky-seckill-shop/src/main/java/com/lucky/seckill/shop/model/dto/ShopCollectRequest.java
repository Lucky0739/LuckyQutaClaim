package com.lucky.seckill.shop.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShopCollectRequest {

    @NotNull(message = "店铺ID不能为空")
    private Long shopId;
}
