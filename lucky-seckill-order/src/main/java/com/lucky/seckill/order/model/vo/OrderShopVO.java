package com.lucky.seckill.order.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单中展示的商品所属店铺信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShopVO {

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺头像/LOGO
     */
    private String shopAvatar;

    /**
     * 店铺简介
     */
    private String shopDesc;
}
