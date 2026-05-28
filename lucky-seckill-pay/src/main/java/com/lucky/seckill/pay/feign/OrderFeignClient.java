package com.lucky.seckill.pay.feign;

import com.lucky.seckill.common.utils.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "lucky-seckill-pay", path = "/pay")
public interface OrderFeignClient {

    @PostMapping("/pay/callback")
    Response<Boolean> payCallback(@RequestParam("orderNo") String orderNo);

    @PostMapping("/cancel")
    Response<Boolean> cancelOrder(@RequestParam("orderNo") String orderNo);
}
