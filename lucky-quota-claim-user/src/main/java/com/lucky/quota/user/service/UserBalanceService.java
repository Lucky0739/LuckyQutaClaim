package com.lucky.quota.user.service;

import com.lucky.quota.common.utils.Response;
import com.lucky.quota.user.model.dto.RechargeBalanceRequest;
import com.lucky.quota.user.model.vo.UserBalanceVO;

import java.math.BigDecimal;

public interface UserBalanceService {
    Response<UserBalanceVO> getBalance();

    Response<UserBalanceVO> recharge(RechargeBalanceRequest request);

    Response<Boolean> rechargeBalance(Long userId, BigDecimal amount);

    Response<Boolean> deductBalance(Long userId, BigDecimal amount);
}
