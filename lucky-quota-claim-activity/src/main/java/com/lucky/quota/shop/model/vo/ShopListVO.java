package com.lucky.quota.shop.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 普通用户查看发放单位列表返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopListVO {

    /**
     * 发放单位ID
     */
    private Long id;

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

    /**
     * 发放单位状态 1-正常 0-关闭/禁用
     */
    private Integer shopStatus;
}
