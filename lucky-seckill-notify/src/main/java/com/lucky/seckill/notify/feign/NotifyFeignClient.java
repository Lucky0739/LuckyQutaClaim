package com.lucky.seckill.notify.feign;

import com.lucky.seckill.common.model.entity.Notify;
import com.lucky.seckill.common.utils.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "lucky-seckill-notify", path = "/notify")
public interface NotifyFeignClient {

    @PostMapping("/save")
    Response<Boolean> saveNotify(@RequestBody Notify notify);

    @PostMapping("/merchant-apply")
    Response<Void> saveMerchantApplyNotify(@RequestBody Notify notify);

    @PutMapping("/apply/read")
    Response<Void> markApplyNotifyRead(@RequestParam("notifyType") Integer notifyType,
                                       @RequestParam("businessId") Long businessId);
}