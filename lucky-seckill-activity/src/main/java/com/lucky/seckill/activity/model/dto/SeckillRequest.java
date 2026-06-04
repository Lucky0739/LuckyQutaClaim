package com.lucky.seckill.activity.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillRequest {

    @NotNull(message = "秒杀商品ID不能为空")
    private Long seckillGoodsId;
}
