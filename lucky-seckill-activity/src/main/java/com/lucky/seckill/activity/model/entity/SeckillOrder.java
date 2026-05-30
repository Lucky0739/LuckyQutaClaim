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
public class SeckillOrder {

    private Long id;

    private Long userId;

    private Long activityId;

    private Long goodsId;

    private Long skuId;

    private String orderNo;

    private BigDecimal seckillPrice;

    private String goodsName;

    private String goodsImg;

    /**
     * 订单状态 0=待支付 1=已支付 2=已发货 3=已收货 4=已退款 5=已取消 6=已关闭
     */
    private Integer status;

    private LocalDateTime expireTime;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
