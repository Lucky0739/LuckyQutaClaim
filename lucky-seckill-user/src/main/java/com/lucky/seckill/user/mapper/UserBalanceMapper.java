package com.lucky.seckill.user.mapper;

import com.lucky.seckill.user.model.entity.UserBalance;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface UserBalanceMapper {
    UserBalance selectByUserId(@Param("userId") Long userId);

    int recharge(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int deduct(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
