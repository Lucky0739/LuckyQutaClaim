package com.lucky.quota.order.feign;

import com.lucky.quota.common.utils.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(contextId = "activityFeignClient", name = "lucky-quota-claim-goods", path = "/activity")
public interface ActivityFeignClient {

    @GetMapping("/internal/ongoing-seckill")
    Response<Boolean> isSkuInOngoingQuota(@RequestParam("goodsId") Long goodsId,
                                              @RequestParam("skuId") Long skuId);
}
