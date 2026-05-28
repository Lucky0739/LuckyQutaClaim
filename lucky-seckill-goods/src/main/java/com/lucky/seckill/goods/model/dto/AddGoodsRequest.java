package com.lucky.seckill.goods.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddGoodsRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称不能超过200个字符")
    private String goodsName;

    @NotNull(message = "商品价格不能为空")
    private BigDecimal goodsPrice;

    private Long categoryId;

    private Long shopId;

    private List<String> goodsImgList;

    private String detailContent;

    private List<GoodsSkuRequest> skuList;
}

