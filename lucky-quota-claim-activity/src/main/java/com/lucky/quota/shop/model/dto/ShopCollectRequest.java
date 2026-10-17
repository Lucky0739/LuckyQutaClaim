package com.lucky.quota.shop.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShopCollectRequest {

    @NotNull(message = "发放单位ID不能为空")
    private Long shopId;
}
