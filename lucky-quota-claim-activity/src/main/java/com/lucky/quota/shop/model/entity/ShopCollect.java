package com.lucky.quota.shop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户发放单位收藏记录
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_user_shop_collect")
public class ShopCollect {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long shopId;

    @TableField(exist = false)
    private Integer isDelete;

    private LocalDateTime createTime;

    @TableField(exist = false)
    private LocalDateTime updateTime;
}
