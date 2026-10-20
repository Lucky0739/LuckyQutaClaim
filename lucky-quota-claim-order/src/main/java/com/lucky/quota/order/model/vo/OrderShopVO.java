package com.lucky.quota.order.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 申领单中展示的物资所属发放单位信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShopVO {

    /**
     * 发放单位ID
     */
    private Long shopId;

    /**
     * 发放单位名称
     */
    private String shopName;

    /**
     * 单位头像/LOGO
     */
    private String shopAvatar;

    /**
     * 单位简介
     */
    private String shopDesc;
}
