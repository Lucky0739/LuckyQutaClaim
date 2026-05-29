package com.lucky.seckill.order.feign;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.order.model.entity.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(contextId = "orderFeignClient", name = "lucky-seckill-order", path = "/order")
public interface OrderFeignClient {

    @GetMapping("/query")
    Response<Order> getOrderByOrderNo(@RequestParam("orderNo") String orderNo);

    @PostMapping("/updateStatus")
    Response<Boolean> updateOrderStatus(@RequestParam("orderNo") String orderNo,
                                        @RequestParam("status") Integer status);
}