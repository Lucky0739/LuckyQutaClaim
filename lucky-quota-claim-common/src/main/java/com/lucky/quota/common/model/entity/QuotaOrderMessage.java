package com.lucky.quota.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 限量申领异步消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaOrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String messageId;

    private Long userId;

    private Long activityId;

    private Long goodsId;

    private Long skuId;

    private Long seckillGoodsId;

    private String orderNo;

    private BigDecimal seckillPrice;

    private String goodsName;

    private String goodsImg;

    private Integer buyNum;

    private LocalDateTime expireTime;

    /** 重试次数，初始为0 */
    private Integer retryCount;
}
