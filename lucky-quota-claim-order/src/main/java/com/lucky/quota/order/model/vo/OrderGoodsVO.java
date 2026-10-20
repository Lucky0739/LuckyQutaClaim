package com.lucky.quota.order.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 申领单中的物资信息（含所属发放单位）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderGoodsVO {

    /**
     * 物资ID
     */
    private Long goodsId;

    /**
     * 物资名称
     */
    private String goodsName;

    /**
     * 物资图片
     */
    private String goodsImg;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 物资所属发放单位
     */
    private OrderShopVO shop;
}
