package com.lucky.quota.activity.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuotaRequest {

    @NotNull(message = "限量物资ID不能为空")
    private Long seckillGoodsId;
}
