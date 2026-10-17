package com.lucky.quota.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.user.model.entity.UserBalance;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface UserBalanceMapper extends BaseMapper<UserBalance> {
    UserBalance selectByUserId(@Param("userId") Long userId);

    int recharge(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int deduct(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
