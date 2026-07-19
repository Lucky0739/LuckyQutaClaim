package com.lucky.seckill.cart.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteCartRequest {

    @NotEmpty(message = "购物车ID列表不能为空")
    private List<Long> cartIds;
}
