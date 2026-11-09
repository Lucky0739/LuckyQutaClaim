package com.lucky.quota.activity.feign;

import com.lucky.quota.common.model.dto.QuotaOrderSyncRequest;
import com.lucky.quota.common.utils.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(contextId = "orderSyncFeignClient", name = "lucky-quota-claim-order", path = "/order")
public interface OrderSyncFeignClient {

    /**
     * 将限量申领单镜像写入订单库 t_order（幂等）。
     */
    @PostMapping("/syncFromQuota")
    Response<Boolean> syncFromQuota(@RequestBody QuotaOrderSyncRequest request);
}
