package com.lucky.quota.activity.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.lucky.quota.activity.model.dto.QuotaGoodsApplyQueryRequest;
import com.lucky.quota.activity.model.dto.QuotaGoodsApplyRequest;
import com.lucky.quota.activity.model.dto.QuotaGoodsAuditRequest;
import com.lucky.quota.activity.model.dto.QuotaRequest;
import com.lucky.quota.activity.model.vo.QuotaActivityVO;
import com.lucky.quota.activity.model.vo.QuotaGoodsApplyVO;
import com.lucky.quota.activity.model.vo.QuotaGoodsVO;
import com.lucky.quota.activity.model.vo.QuotaResultVO;
import com.lucky.quota.activity.service.QuotaActivityService;
import com.lucky.quota.activity.service.QuotaFailedOrderService;
import com.lucky.quota.activity.service.QuotaGoodsService;
import com.lucky.quota.activity.service.QuotaService;
import com.lucky.quota.common.aspect.ApiOperationLog;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.utils.Response;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/activity")
@Slf4j
public class QuotaController {

    @Resource
    private QuotaActivityService seckillActivityService;

    @Resource
    private QuotaService seckillService;

    @Resource
    private QuotaGoodsService seckillGoodsService;

    @Resource
    private QuotaFailedOrderService seckillFailedOrderService;
    @SaIgnore
    @GetMapping("/internal/ongoing-seckill")
    @ApiOperationLog(description = "查询规格是否处于进行中限量申领（服务间调用）")
    public Response<Boolean> isSkuInOngoingQuota(@RequestParam Long goodsId, @RequestParam Long skuId) {
        return seckillActivityService.isSkuInOngoingQuota(goodsId, skuId);
    }

    @SaIgnore
    @GetMapping("/list")
    @ApiOperationLog(description = "限量场次列表")
    public Response<List<QuotaActivityVO>> listActivities() {
        return seckillActivityService.listActivities();
    }

    @SaIgnore
    @GetMapping("/{activityId}")
    @ApiOperationLog(description = "限量场次详情")
    public Response<QuotaActivityVO> getActivityDetail(@PathVariable Long activityId) {
        return seckillActivityService.getActivityDetail(activityId);
    }

    @SaIgnore
    @GetMapping("/{activityId}/goods")
    @ApiOperationLog(description = "限量场次物资列表")
    public Response<List<QuotaGoodsVO>> listActivityGoods(@PathVariable Long activityId) {
        return seckillActivityService.listActivityGoods(activityId);
    }

    @SaCheckRole("user")
    @PostMapping("/seckill")
    @ApiOperationLog(description = "参与限量申领")
    public Response<QuotaResultVO> doQuota(@Validated @RequestBody QuotaRequest request) {
        log.info("==> 申领请求, seckillGoodsId={}", request.getSeckillGoodsId());
        return seckillService.doQuota(request);
    }

    @SaCheckRole("user")
    @GetMapping("/seckill/result")
    @ApiOperationLog(description = "查询限量申领单异步处理结果（前端轮询）")
    public Response<QuotaResultVO> getQuotaResult(@RequestParam("orderNo") String orderNo) {
        return seckillService.getQuotaResult(orderNo);
    }

    @SaCheckRole("user")
    @GetMapping(value = "/seckill/result/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperationLog(description = "SSE实时推送限量申领单处理结果（替代前端轮询）")
    public SseEmitter streamQuotaResult(@RequestParam("orderNo") String orderNo) {
        Long userId = StpUtil.getLoginIdAsLong();
        return seckillService.streamQuotaResult(orderNo, userId);
    }

    @SaCheckRole("merchant")
    @PostMapping("/seckill-goods/apply")
    @ApiOperationLog(description = "申请加入限量物资")
    public Response<Long> applyQuotaGoods(@Validated @RequestBody QuotaGoodsApplyRequest request) {
        log.info("==> 申请加入限量物资, activityId={}, goodsId={}, skuId={}",
                request.getActivityId(), request.getGoodsId(), request.getSkuId());
        return seckillGoodsService.applyQuotaGoods(request);
    }

    @SaCheckRole("admin")
    @PostMapping("/seckill-goods/audit")
    @ApiOperationLog(description = "审核限量投放申请")
    public Response<Void> auditQuotaGoods(@Validated @RequestBody QuotaGoodsAuditRequest request) {
        log.info("==> 审核限量投放申请, seckillGoodsId={}, auditStatus={}",
                request.getSeckillGoodsId(), request.getAuditStatus());
        return seckillGoodsService.auditQuotaGoods(request);
    }

    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @GetMapping("/seckill-goods/apply/page")
    @ApiOperationLog(description = "分页查询限量投放申请")
    public Response<PageResult<QuotaGoodsApplyVO>> listQuotaGoodsApply(
            @RequestParam(value = "activityId", required = false) Long activityId,
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        QuotaGoodsApplyQueryRequest query = QuotaGoodsApplyQueryRequest.builder()
                .activityId(activityId)
                .auditStatus(auditStatus)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        return seckillGoodsService.listApplyPage(query);
    }

    @SaIgnore
    @GetMapping("/seckill-goods/{seckillGoodsId}")
    @ApiOperationLog(description = "限量物资详情（单规格，无需选规格）")
    public Response<QuotaGoodsApplyVO> getQuotaGoodsDetail(@PathVariable Long seckillGoodsId) {
        return seckillGoodsService.getQuotaGoodsDetail(seckillGoodsId);
    }

    @SaCheckRole("admin")
    @PostMapping("/{activityId}/start")
    @ApiOperationLog(description = "开始限量场次")
    public Response<QuotaActivityVO> startActivity(@PathVariable Long activityId) {
        log.info("==> 手动开始限量场次, activityId={}", activityId);
        return seckillActivityService.startActivity(activityId);
    }

    @SaCheckRole("admin")
    @PostMapping("/{activityId}/end")
    @ApiOperationLog(description = "结束限量场次")
    public Response<Boolean> endActivity(@PathVariable Long activityId) {
        log.info("==> 手动结束限量场次, activityId={}", activityId);
        return seckillActivityService.endActivity(activityId);
    }

    // ======================== 死信队列人工处理（管理员） ========================

    @SaCheckRole("admin")
    @GetMapping("/admin/failed-orders")
    @ApiOperationLog(description = "查询DLQ最终失败的申领列表（人工处理）")
    public Response<PageResult<?>> listFailedOrders(
            @RequestParam(value = "handleStatus", required = false) Integer handleStatus,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return seckillFailedOrderService.listFailedOrders(handleStatus, pageNum, pageSize);
    }

    @SaCheckRole("admin")
    @PostMapping("/admin/failed-orders/{id}/retry")
    @ApiOperationLog(description = "手动重试DLQ失败申领单（重新投递到主队列）")
    public Response<Void> manualRetryFailedOrder(@PathVariable Long id) {
        String operator = String.valueOf(StpUtil.getLoginIdAsLong());
        log.info("==> 管理员手动重试失败申领单, id={}, operator={}", id, operator);
        return seckillFailedOrderService.manualRetry(id, operator);
    }

    @SaCheckRole("admin")
    @PostMapping("/admin/failed-orders/{id}/discard")
    @ApiOperationLog(description = "手动放弃DLQ失败申领单（标记为已处理）")
    public Response<Void> manualDiscardFailedOrder(@PathVariable Long id,
                                                    @RequestParam(value = "remark", defaultValue = "") String remark) {
        String operator = String.valueOf(StpUtil.getLoginIdAsLong());
        log.info("==> 管理员手动放弃失败申领单, id={}, operator={}, remark={}", id, operator, remark);
        return seckillFailedOrderService.manualDiscard(id, remark, operator);
    }
}
