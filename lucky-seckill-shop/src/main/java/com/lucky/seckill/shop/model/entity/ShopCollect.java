package com.lucky.seckill.shop.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户店铺收藏记录
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShopCollect {

    private Long id;

    private Long userId;

    private Long shopId;

    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
