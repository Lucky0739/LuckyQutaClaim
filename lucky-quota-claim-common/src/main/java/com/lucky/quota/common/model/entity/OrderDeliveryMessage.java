package com.lucky.quota.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 申领单发放消息实体类
 * 用于商家发放后，通过 RocketMQ 通知买家
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveryMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID（用于追踪）
     */
    private String messageId;

    /**
     * 申领单ID
     */
    private Long orderId;

    /**
     * 申领单号
     */
    private String orderNo;

    /**
     * 买家用户ID
     */
    private Long buyerUserId;

    /**
     * 商家用户ID
     */
    private Long sellerUserId;

    /**
     * 物资ID
     */
    private Long goodsId;

    /**
     * 物资名称
     */
    private String goodsName;

    /**
     * 物资图片
     */
    private String goodsImg;

    /**
     * 发放时间
     */
    private LocalDateTime deliveryTime;
}
