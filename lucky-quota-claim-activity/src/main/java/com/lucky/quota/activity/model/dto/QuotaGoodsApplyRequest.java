package com.lucky.quota.activity.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuotaGoodsApplyRequest {

    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "物资ID不能为空")
    private Long goodsId;

    @NotNull(message = "规格SKU ID不能为空")
    private Long skuId;

    private String seckillTitle;

    private String seckillImg;

    @NotNull(message = "限量申领价不能为空")
    @DecimalMin(value = "0.01", message = "限量申领价必须大于0")
    private BigDecimal seckillPrice;

    @NotNull(message = "限量申领库存不能为空")
    @Min(value = 1, message = "限量申领库存必须大于0")
    private Integer seckillTotal;

    @Min(value = 1, message = "限购数量必须大于0")
    private Integer seckillLimit = 1;

    private Integer sort = 0;
}
