package com.lucky.quota.activity.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 限量申领失败申领单 VO（后台人工处理列表）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaFailedOrderVO {

    private Long id;

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

    private String lastError;

    private Integer retryCount;

    /** 处理状态：0=待人工处理，1=已手动重试成功，2=已手动放弃 */
    private Integer handleStatus;

    /** 处理状态文本 */
    private String handleStatusText;

    private String handleRemark;

    private String handledBy;

    private LocalDateTime handledTime;

    private LocalDateTime createTime;
}
