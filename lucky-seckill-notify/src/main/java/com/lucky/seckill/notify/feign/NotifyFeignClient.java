package com.lucky.seckill.notify.feign;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.notify.model.entity.Notify;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(contextId = "notifyCenterFeignClient", name = "lucky-seckill-notify", path = "/notify")
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
                                       @RequestParam("notifyId") Long notifyId);
}