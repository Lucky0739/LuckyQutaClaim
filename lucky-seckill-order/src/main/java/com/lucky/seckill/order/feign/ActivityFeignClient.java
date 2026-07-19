package com.lucky.seckill.order.feign;

import com.lucky.seckill.common.utils.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(contextId = "activityFeignClient", name = "lucky-seckill-activity", path = "/activity")
public interface ActivityFeignClient {

    @GetMapping("/internal/ongoing-seckill")
    Response<Boolean> isSkuInOngoingSeckill(@RequestParam("goodsId") Long goodsId,
                                              @RequestParam("skuId") Long skuId);
}
