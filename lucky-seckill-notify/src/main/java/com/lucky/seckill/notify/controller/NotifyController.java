package com.lucky.seckill.notify.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.notify.model.dto.NotifyQueryRequest;
import com.lucky.seckill.notify.model.entity.Notify;
import com.lucky.seckill.notify.model.vo.NotifyListVO;
import com.lucky.seckill.notify.model.vo.NotifyTypeSummaryVO;
import com.lucky.seckill.notify.model.vo.NotifyUnreadStatVO;
import com.lucky.seckill.notify.model.vo.NotifyVO;
import com.lucky.seckill.notify.service.NotifyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * 将消息标记为已读
     */
    @SaCheckLogin
    @PutMapping("/read")
    public Response<Void> markNotifyRead(@RequestParam("notifyId") Long notifyId) {
        log.info("==> 标记消息为已读, notifyId: {}", notifyId);
        notifyService.markNotifyRead(notifyId);
        return Response.success();
    }

    /**
     * 将商户申请消息标记为已读（微服务内部调用，按 businessId 定位）
     */
    @PutMapping("/apply/read")
    public Response<Void> markApplyNotifyRead(@RequestParam("notifyType") Integer notifyType,
                                              @RequestParam(value = "businessId", required = false) Long businessId,
                                              @RequestParam(value = "notifyId", required = false) Long notifyId) {
        Long resolvedBusinessId = businessId != null ? businessId : notifyId;
        if (resolvedBusinessId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "businessId 不能为空");
        }
        log.info("==> 标记商户申请消息为已读, notifyType: {}, businessId: {}", notifyType, resolvedBusinessId);
        notifyService.markApplyNotifyRead(notifyType, resolvedBusinessId);
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
     * 用户分页查询自己收到的消息（receive_user_id = 当前登录用户ID）
     */
    @SaCheckLogin
    @PostMapping("/user/list")
    public Response<NotifyListVO> getUserNotifyList(@RequestBody NotifyQueryRequest request) {
        log.info("==> 用户查询消息列表, notifyType: {}, isRead: {}, pageNum: {}, pageSize: {}",
                request.getNotifyType(), request.getIsRead(),
                request.getPageNum(), request.getPageSize());
        return Response.success(notifyService.selectUserNotifyPage(request));
    }

    /**
     * 用户分页查询自己收到的消息（GET）
     */
    @SaCheckLogin
    @GetMapping("/user/list")
    public Response<NotifyListVO> getUserNotifyListByGet(@RequestParam(value = "notifyType", required = false) Integer notifyType,
                                                         @RequestParam(value = "isRead", required = false) Integer isRead,
                                                         @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                         @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        NotifyQueryRequest request = new NotifyQueryRequest();
        request.setNotifyType(notifyType);
        request.setIsRead(isRead);
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        return Response.success(notifyService.selectUserNotifyPage(request));
    }

    /**
     * 查询当前登录用户的消息详情
     */
    @SaCheckLogin
    @GetMapping("/user/detail")
    public Response<NotifyVO> getUserNotifyDetail(@RequestParam("notifyId") Long notifyId) {
        log.info("==> 用户查询消息详情, notifyId: {}", notifyId);
        return Response.success(notifyService.getUserNotifyDetail(notifyId));
    }

    /**
     * 查询当前登录用户未读消息数（用于消息角标）
     */
    @SaCheckLogin
    @GetMapping("/user/unread/count")
    public Response<Long> countUserUnreadNotify(@RequestParam(value = "notifyType", required = false) Integer notifyType) {
        log.info("==> 查询用户未读消息数, notifyType: {}", notifyType);
        return Response.success(notifyService.countUserUnreadNotify(notifyType));
    }

    /**
     * 按用户 ID、消息类型统计未读消息数量（userId 不传则统计当前登录用户）
     */
    @SaCheckLogin
    @GetMapping("/user/unread/stats")
    public Response<List<NotifyUnreadStatVO>> listUserUnreadCountByType(
            @RequestParam(value = "userId", required = false) Long userId) {
        log.info("==> 按类型按用户统计未读消息数, userId: {}", userId);
        return Response.success(notifyService.listUserUnreadCountByType(userId));
    }

    /**
     * 查询当前登录用户各消息类型 Tab 汇总（未读数/总数）
     */
    @SaCheckLogin
    @GetMapping("/user/summary")
    public Response<List<NotifyTypeSummaryVO>> getUserNotifyTypeSummary() {
        log.info("==> 查询用户消息类型汇总");
        return Response.success(notifyService.getUserNotifyTypeSummary());
    }

    /**
     * 将当前登录用户消息全部标记为已读
     */
    @SaCheckLogin
    @PutMapping("/user/read/all")
    public Response<Void> markAllNotifyRead(@RequestParam(value = "notifyType", required = false) Integer notifyType) {
        log.info("==> 用户全部消息标记已读, notifyType: {}", notifyType);
        notifyService.markAllNotifyRead(notifyType);
        return Response.success();
    }
}
