package com.lucky.seckill.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单发货消息实体类
 * 用于商家发货后，通过 RocketMQ 通知买家
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
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
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
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品图片
     */
    private String goodsImg;

    /**
     * 发货时间
     */
    private LocalDateTime deliveryTime;
}
