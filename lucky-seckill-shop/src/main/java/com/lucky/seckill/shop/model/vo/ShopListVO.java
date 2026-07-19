package com.lucky.seckill.shop.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 普通用户查看店铺列表返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopListVO {

    /**
     * 店铺ID
     */
    private Long id;

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

    /**
     * 店铺状态 1-正常 0-关闭/禁用
     */
    private Integer shopStatus;
}
