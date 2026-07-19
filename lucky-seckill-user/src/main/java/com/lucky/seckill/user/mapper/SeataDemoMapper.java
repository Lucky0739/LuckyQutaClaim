package com.lucky.seckill.user.mapper;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface SeataDemoMapper {

    int addBalance(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);
}
