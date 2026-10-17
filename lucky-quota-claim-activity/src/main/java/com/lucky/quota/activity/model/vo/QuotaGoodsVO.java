package com.lucky.quota.activity.model.vo;

import com.lucky.quota.activity.model.entity.QuotaGoods;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaGoodsVO {

    private Long id;

    private Long activityId;

    private Long goodsId;

    private Long skuId;

    private String skuSpecName;

    private String seckillTitle;

    private String seckillImg;

    private BigDecimal seckillPrice;

    private Integer seckillStock;

    private Integer seckillLimit;

    private Integer sort;

    public static QuotaGoodsVO fromEntity(QuotaGoods goods) {
        if (goods == null) {
            return null;
        }
        return QuotaGoodsVO.builder()
                .id(goods.getId())
                .activityId(goods.getActivityId())
                .goodsId(goods.getGoodsId())
                .skuId(goods.getSkuId())
                .skuSpecName(goods.getSkuSpecName())
                .seckillTitle(goods.getSeckillTitle())
                .seckillImg(goods.getSeckillImg())
                .seckillPrice(goods.getSeckillPrice())
                .seckillStock(goods.getSeckillStock())
                .seckillLimit(goods.getSeckillLimit())
                .sort(goods.getSort())
                .build();
    }
}
