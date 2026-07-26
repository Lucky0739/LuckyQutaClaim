package com.lucky.seckill.activity.model.vo;

import com.lucky.seckill.activity.model.entity.SeckillGoods;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillGoodsApplyVO {

    private Long id;

    private Long activityId;

    private String activityName;

    private Long goodsId;

    private Long skuId;

    private String skuSpecName;

    private String seckillTitle;

    private String seckillImg;

    private BigDecimal seckillPrice;

    private Integer seckillTotal;

    private Integer seckillStock;

    private Integer seckillLimit;

    private Integer auditStatus;

    private String auditRemark;

    private Long userId;

    /**
     * 当前是否可参与秒杀购买（活动已开始、进行中且库存已预热）
     */
    private Boolean seckillPurchasable;

    private LocalDateTime createTime;

    public static SeckillGoodsApplyVO fromEntity(SeckillGoods goods) {
        if (goods == null) {
            return null;
        }
        return SeckillGoodsApplyVO.builder()
                .id(goods.getId())
                .activityId(goods.getActivityId())
                .goodsId(goods.getGoodsId())
                .skuId(goods.getSkuId())
                .skuSpecName(goods.getSkuSpecName())
                .seckillTitle(goods.getSeckillTitle())
                .seckillImg(goods.getSeckillImg())
                .seckillPrice(goods.getSeckillPrice())
                .seckillTotal(goods.getSeckillTotal())
                .seckillStock(goods.getSeckillStock())
                .seckillLimit(goods.getSeckillLimit())
                .auditStatus(goods.getAuditStatus())
                .auditRemark(goods.getAuditRemark())
                .createTime(goods.getCreateTime())
                .build();
    }
}
