package com.lucky.quota.goods.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 添加物资请求DTO
 */
@Data
public class AddGoodsRequest {

    /**
     * 物资名称
     */
    private String goodsName;

    /**
     * 物资主图
     */
    private String goodsImg;

    /**
     * 物资价格
     */
    private BigDecimal goodsPrice;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 发放单位ID（可选，商户入驻后才有）
     */
    private Long shopId;

    /**
     * 物资描述
     */
    private String goodsDesc;

    /**
     * 物资详情内容（富文本）
     */
    private String detailContent;

    /**
     * 物资图片列表
     */
    private List<String> goodsImgList;

    /**
     * SKU列表
     */
    private List<GoodsSkuRequest> skuList;
}
