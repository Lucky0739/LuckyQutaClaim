package com.lucky.seckill.user.service;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.model.dto.RechargeBalanceRequest;
import com.lucky.seckill.user.model.vo.UserBalanceVO;

import java.math.BigDecimal;

public interface UserBalanceService {
    Response<UserBalanceVO> getBalance();

    Response<UserBalanceVO> recharge(RechargeBalanceRequest request);

    Response<Boolean> rechargeBalance(Long userId, BigDecimal amount);

    Response<Boolean> deductBalance(Long userId, BigDecimal amount);
}
