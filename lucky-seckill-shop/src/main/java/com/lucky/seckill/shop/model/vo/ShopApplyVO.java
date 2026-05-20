        package com.lucky.seckill.shop.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商家申请列表返回VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopApplyVO {

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
     * 所属用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 审核状态 0-待审核 1-审核通过 2-审核拒绝
     */
    private Integer auditStatus;

    /**
     * 审核状态描述
     */
    private String auditStatusDesc;

    /**
     * 审核备注/拒绝原因
     */
    private String auditRemark;

    /**
     * 店铺状态 1-正常 0-关闭/禁用
     */
    private Integer shopStatus;

    /**
     * 申请时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
