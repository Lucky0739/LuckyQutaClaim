package com.lucky.quota.goods.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GoodsCollectRequest {

    @NotNull(message = "物资ID不能为空")
    private Long goodsId;
}
