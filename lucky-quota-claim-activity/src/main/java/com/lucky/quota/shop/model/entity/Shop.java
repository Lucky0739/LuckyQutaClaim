package com.lucky.quota.shop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 发放单位
 * @TableName t_shop
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_shop")
public class Shop implements Serializable {

    /**
     * 发放单位主键ID
     */
    @TableId(type = IdType.AUTO)
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
     * 单位状态 1-正常 0-关闭/禁用
     */
    private Integer shopStatus;

    /**
     * 收藏数
     */
    private Integer collectNum;
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
    @TableLogic
    private Integer isDelete;

    /**
     * 用户名
     */
    private String username;

}
