package com.lucky.seckill.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.mapper.UserBalanceMapper;
import com.lucky.seckill.user.model.dto.RechargeBalanceRequest;
import com.lucky.seckill.user.model.entity.UserBalance;
import com.lucky.seckill.user.model.vo.UserBalanceVO;
import com.lucky.seckill.user.service.UserBalanceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
public class UserBalanceServiceImpl implements UserBalanceService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Resource
    private UserBalanceMapper userBalanceMapper;

    @Override
    public Response<UserBalanceVO> getBalance() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Response.success(buildBalanceVO(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<UserBalanceVO> recharge(RechargeBalanceRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        BigDecimal amount = checkAmount(request.getAmount(), "充值金额必须大于0");
        rechargeBalance(userId, amount);
        log.info("==> 用户余额充值成功, userId: {}, amount: {}", userId, amount);
        return Response.success(buildBalanceVO(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> rechargeBalance(Long userId, BigDecimal amount) {
        checkUserId(userId);
        BigDecimal validAmount = checkAmount(amount, "充值金额必须大于0");
        int rows = userBalanceMapper.recharge(userId, validAmount);
        if (rows <= 0) {
            throw new BizException(ResponseCodeEnum.BALANCE_RECHARGE_FAIL, "余额充值失败");
        }
        return Response.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> deductBalance(Long userId, BigDecimal amount) {
        checkUserId(userId);
        BigDecimal validAmount = checkAmount(amount, "扣减金额必须大于0");
        int rows = userBalanceMapper.deduct(userId, validAmount);
        if (rows <= 0) {
            throw new BizException(ResponseCodeEnum.BALANCE_NOT_ENOUGH, "账户余额不足");
        }
        log.info("==> 用户余额扣减成功, userId: {}, amount: {}", userId, validAmount);
        return Response.success(true);
    }

    private UserBalanceVO buildBalanceVO(Long userId) {
        UserBalance userBalance = userBalanceMapper.selectByUserId(userId);
        BigDecimal balance = userBalance == null ? ZERO : userBalance.getBalance();
        return UserBalanceVO.builder()
                .userId(userId)
                .balance(balance)
                .build();
    }

    private void checkUserId(Long userId) {
        if (userId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "用户ID不能为空");
        }
    }

    private BigDecimal checkAmount(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, message);
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
