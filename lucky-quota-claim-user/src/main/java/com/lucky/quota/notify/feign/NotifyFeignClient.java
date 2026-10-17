package com.lucky.quota.notify.feign;

import com.lucky.quota.common.utils.Response;
import com.lucky.quota.notify.model.entity.Notify;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(contextId = "notifyCenterFeignClient", name = "lucky-quota-claim-user", path = "/notify")
public interface NotifyFeignClient {

    @PostMapping("/save")
    @Headers("Content-Type: application/json")
    Response<Boolean> saveNotify(@RequestBody Notify notify);

    @PostMapping("/merchant-apply")
    @Headers("Content-Type: application/json")
    Response<Void> saveMerchantApplyNotify(@RequestBody Notify notify);

    @PutMapping("/read")
    Response<Void> markNotifyRead(@RequestParam("notifyId") Long notifyId);

    @PutMapping("/apply/read")
    Response<Void> markApplyNotifyRead(@RequestParam("notifyType") Integer notifyType,
                                       @RequestParam("businessId") Long businessId);
}