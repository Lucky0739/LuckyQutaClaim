package com.lucky.seckill.goods.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 添加商品请求DTO
 */
@Data
public class AddGoodsRequest {

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品主图
     */
    private String goodsImg;

    /**
     * 商品价格
     */
    private BigDecimal goodsPrice;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 店铺ID（可选，商户入驻后才有）
     */
    private Long shopId;

    /**
     * 商品描述
     */
    private String goodsDesc;

    /**
     * 商品详情内容（富文本）
     */
    private String detailContent;

    /**
     * 商品图片列表
     */
    private List<String> goodsImgList;

    /**
     * SKU列表
     */
    private List<GoodsSkuRequest> skuList;
}
