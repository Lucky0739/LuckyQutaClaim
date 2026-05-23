package com.lucky.seckill.order.model.entity;

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
public class Order {
    private Long id;

    private Long userId;

    private Long goodsId;

    private String orderNo;

    private BigDecimal goodsPrice;

    private Integer goodsNum;

    private BigDecimal totalAmount;

    private String goodsName;

    private String goodsImg;

    private Integer status;

    private LocalDateTime payTime;

    private LocalDateTime deliveryTime;

    private LocalDateTime receiveTime;

    private LocalDateTime expireTime;

    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
