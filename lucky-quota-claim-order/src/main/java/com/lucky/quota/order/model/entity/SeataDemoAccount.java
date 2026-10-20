package com.lucky.quota.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("seata_demo_account")
public class SeataDemoAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private BigDecimal balance;
}
