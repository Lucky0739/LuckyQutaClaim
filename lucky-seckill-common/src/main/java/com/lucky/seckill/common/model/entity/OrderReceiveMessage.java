package com.lucky.seckill.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单确认收货消息实体类
 * 用于买家确认收货后，通过 RocketMQ 通知商家
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReceiveMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String messageId;

    private Long orderId;

    private String orderNo;

    private Long buyerUserId;

    private Long sellerUserId;

    private Long goodsId;

    private String goodsName;

    private LocalDateTime receiveTime;
}
