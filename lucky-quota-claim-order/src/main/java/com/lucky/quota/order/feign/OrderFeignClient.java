package com.lucky.quota.order.feign;

import com.lucky.quota.common.utils.Response;
import com.lucky.quota.order.model.entity.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(contextId = "orderFeignClient", name = "lucky-quota-claim-order", path = "/order")
public interface OrderFeignClient {

    @GetMapping("/query")
    Response<Order> getOrderByOrderNo(@RequestParam("orderNo") String orderNo);

    @PostMapping("/updateStatus")
    Response<Boolean> updateOrderStatus(@RequestParam("orderNo") String orderNo,
                                        @RequestParam("status") Integer status);
}