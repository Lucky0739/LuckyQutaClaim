package com.lucky.quota.goods.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户物资收藏记录（t_user_goods_collect）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_user_goods_collect")
public class GoodsCollect {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long goodsId;

    private LocalDateTime createTime;
}
