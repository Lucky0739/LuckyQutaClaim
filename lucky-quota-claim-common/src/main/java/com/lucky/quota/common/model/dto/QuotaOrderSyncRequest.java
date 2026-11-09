package com.lucky.quota.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 限量申领单落库后同步到 t_order（供「我的申领」与支付/关单链路使用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaOrderSyncRequest {

    private Long userId;

    private Long goodsId;

    private Long skuId;

    private Long shopId;

    private String orderNo;

    private BigDecimal seckillPrice;

    private String goodsName;

    private String goodsImg;

    private Integer buyNum;

    private LocalDateTime expireTime;
}
