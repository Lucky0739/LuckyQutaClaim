package com.lucky.seckill.notify.controller;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.notify.model.entity.Notify;
import com.lucky.seckill.notify.service.NotifyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内消息服务接口（供其他微服务通过 OpenFeign 调用）
 */
@RestController
@RequestMapping("/notify")
@Slf4j
public class NotifyController {

    @Resource
    private NotifyService notifyService;

    /**
     * 保存通知
     */
    @PostMapping("/save")
    public Response<Boolean> saveNotify(@RequestBody Notify notify) {
       Boolean result = notifyService.saveNotify(notify);
        return Response.success(result);
    }

    /**
     * 保存商户开店申请通知（MQ 消费等场景）
     */
    @PostMapping("/merchant-apply")
    public Response<Void> saveMerchantApplyNotify(@RequestBody Notify notify) {
        notifyService.saveMerchantApplyNotify(notify);
        return Response.success();
    }


    /**
     * 将开店申请通知标记为已读
     */
    @PutMapping("/apply/read")
    public Response<Void> markApplyNotifyRead(@RequestParam("notifyType") Integer notifyType,
                                              @RequestParam("businessId") Long businessId) {
        notifyService.markApplyNotifyRead(notifyType, businessId);
        return Response.success();
    }
    /**
     * 获取消息列表
     */

}
