package com.lucky.seckill.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SeckillGoods {

    private Long id;

    private Long activityId;

    private Long goodsId;

    private Long skuId;

    private String skuSpecName;

    private String seckillTitle;

    private String seckillImg;

    private BigDecimal seckillPrice;

    private Integer seckillTotal;

    private Integer seckillStock;

    private Integer seckillLimit;

    private Integer sort;

    private Integer auditStatus;

    private String auditRemark;

    private Integer version;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
