package com.lucky.quota.goods.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商户更新自己发布的物资
 */
@Data
public class UpdateGoodsRequest {

    @NotNull(message = "物资ID不能为空")
    private Long goodsId;

    private String goodsName;

    private String goodsImg;

    private BigDecimal goodsPrice;

    private Long categoryId;

    private Long shopId;

    private String detailContent;

    private List<String> goodsImgList;

    private List<UpdateGoodsSkuRequest> skuList;
}
