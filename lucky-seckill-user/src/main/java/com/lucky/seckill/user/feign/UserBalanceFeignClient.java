package com.lucky.seckill.user.feign;

import com.lucky.seckill.common.utils.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(contextId = "userBalanceFeignClient", name = "lucky-seckill-user", path = "/user/balance")
public interface UserBalanceFeignClient {

    @PostMapping("/internal/recharge")
    Response<Boolean> rechargeBalance(@RequestParam("userId") Long userId,
                                      @RequestParam("amount") BigDecimal amount);

    @PostMapping("/internal/deduct")
    Response<Boolean> deductBalance(@RequestParam("userId") Long userId,
                                    @RequestParam("amount") BigDecimal amount);
}
