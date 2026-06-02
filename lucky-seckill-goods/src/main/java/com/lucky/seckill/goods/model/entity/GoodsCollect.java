package com.lucky.seckill.goods.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户商品收藏记录（t_user_goods_collect）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoodsCollect {

    private Long id;

    private Long userId;

    private Long goodsId;

    private LocalDateTime createTime;
}
