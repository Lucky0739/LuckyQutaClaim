package com.lucky.seckill.order.model.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeataDemoAccount {
    private Long id;
    private String name;
    private BigDecimal balance;
}
