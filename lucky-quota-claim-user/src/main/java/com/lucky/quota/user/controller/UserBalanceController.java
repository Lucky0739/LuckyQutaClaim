package com.lucky.quota.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.lucky.quota.common.aspect.ApiOperationLog;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.user.model.dto.RechargeBalanceRequest;
import com.lucky.quota.user.model.vo.UserBalanceVO;
import com.lucky.quota.user.service.UserBalanceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/user/balance")
@Slf4j
public class UserBalanceController {
    @Resource
    private UserBalanceService userBalanceService;

    /**
     * 查询当前用户余额
     */
    @SaCheckLogin
    @GetMapping("/get")
    @ApiOperationLog(description = "查询用户余额")
    public Response<UserBalanceVO> getBalance() {
        return userBalanceService.getBalance();
    }

    /**
     * 当前用户余额充值
     */
    @SaCheckLogin
    @PostMapping("/recharge")
    @ApiOperationLog(description = "用户余额充值")
    public Response<UserBalanceVO> recharge(@Validated @RequestBody RechargeBalanceRequest request) {
        return userBalanceService.recharge(request);
    }

    /**
     * 内部服务充值余额，支付补偿等场景使用
     */
    @PostMapping("/internal/recharge")
    @ApiOperationLog(description = "内部充值余额")
    public Response<Boolean> rechargeBalance(@RequestParam("userId") Long userId,
                                             @RequestParam("amount") BigDecimal amount) {
        log.info("==> 内部充值余额请求, userId: {}, amount: {}", userId, amount);
        return userBalanceService.rechargeBalance(userId, amount);
    }

    /**
     * 内部服务扣减余额，余额支付场景使用
     */
    @PostMapping("/internal/deduct")
    @ApiOperationLog(description = "内部扣减余额")
    public Response<Boolean> deductBalance(@RequestParam("userId") Long userId,
                                           @RequestParam("amount") BigDecimal amount) {
        log.info("==> 内部扣减余额请求, userId: {}, amount: {}", userId, amount);
        return userBalanceService.deductBalance(userId, amount);
    }
}
