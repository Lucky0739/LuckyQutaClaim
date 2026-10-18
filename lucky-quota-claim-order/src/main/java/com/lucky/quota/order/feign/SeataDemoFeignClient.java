package com.lucky.quota.order.feign;

import com.lucky.quota.common.utils.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(contextId = "seataDemoFeignClient", name = "lucky-quota-claim-user", path = "/user/seata/demo")
public interface SeataDemoFeignClient {

    @PostMapping("/credit")
    Response<Boolean> credit(@RequestParam("accountId") Long accountId,
                             @RequestParam("amount") BigDecimal amount);
}
