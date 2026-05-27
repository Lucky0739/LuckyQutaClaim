package com.lucky.seckill.notify.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.lucky.seckill.common.model.entity.Notify;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.notify.model.dto.NotifyQueryRequest;
import com.lucky.seckill.notify.service.NotifyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
     * 将消息标记为已读
     */
    @PutMapping("/read")
    public Response<Void> markNotifyRead(@RequestParam("notifyId") Long notifyId) {
        log.info("==> 标记消息为已读, notifyId: {}", notifyId);
        notifyService.markNotifyRead(notifyId);
        return Response.success();
    }

    /**
     * 管理员查询消息列表
     */
    @PostMapping("/list")
    public Response<PageResult<Notify>> getNotifyList(@RequestBody NotifyQueryRequest request) {
        PageResult<Notify> result = notifyService.selectByNotifyTypePage(
                request.getNotifyType(), request.getPageNum(), request.getPageSize());
        return Response.success(result);
    }

}
