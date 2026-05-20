package com.lucky.seckill.shop.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 店铺表
 * @TableName t_shop
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Shop implements Serializable {

    /**
     * 店铺主键ID
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
     * 所属创建用户ID
     */
    private Long userId;
    /**
     * 审核状态 0-待审核 1-审核通过 2-审核拒绝
     */
    private Integer auditStatus;
    /**
     * 审核备注/拒绝原因
     */
    private String auditRemark;
    /**
     * 店铺状态 1-正常 0-关闭/禁用
     */
    private Integer shopStatus;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除状态 0-正常 1-删除
     */
    private Integer isDelete;

    /**
     * 用户名
     */
    private String username;

}
