package com.lucky.seckill.activity.model.vo;

import com.lucky.seckill.activity.model.entity.SeckillGoods;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillGoodsVO {

    private Long id;

    private Long activityId;

    private Long goodsId;

    private Long skuId;

    private String seckillTitle;

    private String seckillImg;

    private BigDecimal seckillPrice;

    private Integer seckillStock;

    private Integer seckillLimit;

    private Integer sort;

    public static SeckillGoodsVO fromEntity(SeckillGoods goods) {
        if (goods == null) {
            return null;
        }
        return SeckillGoodsVO.builder()
                .id(goods.getId())
                .activityId(goods.getActivityId())
                .goodsId(goods.getGoodsId())
                .skuId(goods.getSkuId())
                .seckillTitle(goods.getSeckillTitle())
                .seckillImg(goods.getSeckillImg())
                .seckillPrice(goods.getSeckillPrice())
                .seckillStock(goods.getSeckillStock())
                .seckillLimit(goods.getSeckillLimit())
                .sort(goods.getSort())
                .build();
    }
}
