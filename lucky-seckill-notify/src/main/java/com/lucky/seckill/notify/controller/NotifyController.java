package com.lucky.seckill.notify.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.notify.model.dto.NotifyQueryRequest;
import com.lucky.seckill.notify.model.entity.Notify;
import com.lucky.seckill.notify.service.NotifyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 站内消息服务接口（供其他微服务通过 OpenFeign 调用，以及前端查询）
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
     * 将消息标记为已读（接收人查看后写入 Redis ZSet）
     */
    @SaCheckLogin
    @PutMapping("/read")
    public Response<Void> markNotifyRead(@RequestParam("notifyId") Long notifyId) {
        log.info("==> 标记消息为已读, notifyId: {}", notifyId);
        notifyService.markNotifyRead(notifyId);
        return Response.success();
    }

    /**
     * 将商户申请消息标记为已读
     */
    @SaCheckLogin
    @PutMapping("/apply/read")
    public Response<Void> markApplyNotifyRead(@RequestParam("notifyId") Long notifyId,
                                              @RequestParam("notifyType") Integer notifyType) {
        log.info("==> 标记商户申请消息为已读, notifyId: {}, notifyType: {}", notifyId, notifyType);
        notifyService.markNotifyRead(notifyId);
        return Response.success();
    }

    /**
     * 管理员分页查询消息列表（按消息类型，供 Tab 切换）
     */
    @SaCheckLogin
    @SaCheckRole("admin")
    @PostMapping("/admin/list")
    public Response<PageResult<Notify>> getAdminNotifyList(@RequestBody NotifyQueryRequest request) {
        log.info("==> 管理员查询消息列表, notifyType: {}, pageNum: {}, pageSize: {}",
                request.getNotifyType(), request.getPageNum(), request.getPageSize());
        return Response.success(notifyService.selectAdminNotifyPage(request));
    }

    /**
     * 普通用户分页查询自己的消息列表（按消息类型，供 Tab 切换）
     */
    @SaCheckLogin
    @PostMapping("/user/list")
    public Response<PageResult<Notify>> getUserNotifyList(@RequestBody NotifyQueryRequest request) {
        log.info("==> 用户查询消息列表, notifyType: {}, pageNum: {}, pageSize: {}",
                request.getNotifyType(), request.getPageNum(), request.getPageSize());
        return Response.success(notifyService.selectUserNotifyPage(request));
    }
}
