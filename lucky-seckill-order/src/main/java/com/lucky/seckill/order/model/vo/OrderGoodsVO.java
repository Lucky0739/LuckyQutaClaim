package com.lucky.seckill.order.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单中的商品信息（含所属店铺）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderGoodsVO {

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品图片
     */
    private String goodsImg;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 商品所属店铺
     */
    private OrderShopVO shop;
}
