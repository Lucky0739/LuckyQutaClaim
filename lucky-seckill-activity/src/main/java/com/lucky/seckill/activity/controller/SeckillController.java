package com.lucky.seckill.activity.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.lucky.seckill.activity.model.dto.SeckillGoodsApplyQueryRequest;
import com.lucky.seckill.activity.model.dto.SeckillGoodsApplyRequest;
import com.lucky.seckill.activity.model.dto.SeckillGoodsAuditRequest;
import com.lucky.seckill.activity.model.dto.SeckillRequest;
import com.lucky.seckill.activity.model.vo.SeckillActivityVO;
import com.lucky.seckill.activity.model.vo.SeckillGoodsApplyVO;
import com.lucky.seckill.activity.model.vo.SeckillGoodsVO;
import com.lucky.seckill.activity.model.vo.SeckillResultVO;
import com.lucky.seckill.activity.service.SeckillActivityService;
import com.lucky.seckill.activity.service.SeckillFailedOrderService;
import com.lucky.seckill.activity.service.SeckillGoodsService;
import com.lucky.seckill.activity.service.SeckillService;
import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/activity")
@Slf4j
public class SeckillController {

    @Resource
    private SeckillActivityService seckillActivityService;

    @Resource
    private SeckillService seckillService;

    @Resource
    private SeckillGoodsService seckillGoodsService;

    @Resource
    private SeckillFailedOrderService seckillFailedOrderService;
    @SaIgnore
    @GetMapping("/internal/ongoing-seckill")
    @ApiOperationLog(description = "查询规格是否处于进行中秒杀（服务间调用）")
    public Response<Boolean> isSkuInOngoingSeckill(@RequestParam Long goodsId, @RequestParam Long skuId) {
        return seckillActivityService.isSkuInOngoingSeckill(goodsId, skuId);
    }

    @SaIgnore
    @GetMapping("/list")
    @ApiOperationLog(description = "秒杀活动列表")
    public Response<List<SeckillActivityVO>> listActivities() {
        return seckillActivityService.listActivities();
    }

    @SaIgnore
    @GetMapping("/{activityId}")
    @ApiOperationLog(description = "秒杀活动详情")
    public Response<SeckillActivityVO> getActivityDetail(@PathVariable Long activityId) {
        return seckillActivityService.getActivityDetail(activityId);
    }

    @SaIgnore
    @GetMapping("/{activityId}/goods")
    @ApiOperationLog(description = "秒杀活动商品列表")
    public Response<List<SeckillGoodsVO>> listActivityGoods(@PathVariable Long activityId) {
        return seckillActivityService.listActivityGoods(activityId);
    }

    @SaCheckRole("user")
    @PostMapping("/seckill")
    @ApiOperationLog(description = "参与秒杀")
    public Response<SeckillResultVO> doSeckill(@Validated @RequestBody SeckillRequest request) {
        log.info("==> 秒杀请求, seckillGoodsId={}", request.getSeckillGoodsId());
        return seckillService.doSeckill(request);
    }

    @SaCheckRole("user")
    @GetMapping("/seckill/result")
    @ApiOperationLog(description = "查询秒杀订单异步处理结果（前端轮询）")
    public Response<SeckillResultVO> getSeckillResult(@RequestParam("orderNo") String orderNo) {
        return seckillService.getSeckillResult(orderNo);
    }

    @SaCheckRole("merchant")
    @PostMapping("/seckill-goods/apply")
    @ApiOperationLog(description = "申请加入秒杀商品")
    public Response<Long> applySeckillGoods(@Validated @RequestBody SeckillGoodsApplyRequest request) {
        log.info("==> 申请加入秒杀商品, activityId={}, goodsId={}, skuId={}",
                request.getActivityId(), request.getGoodsId(), request.getSkuId());
        return seckillGoodsService.applySeckillGoods(request);
    }

    @SaCheckRole("admin")
    @PostMapping("/seckill-goods/audit")
    @ApiOperationLog(description = "审核秒杀商品申请")
    public Response<Void> auditSeckillGoods(@Validated @RequestBody SeckillGoodsAuditRequest request) {
        log.info("==> 审核秒杀商品申请, seckillGoodsId={}, auditStatus={}",
                request.getSeckillGoodsId(), request.getAuditStatus());
        return seckillGoodsService.auditSeckillGoods(request);
    }

    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @GetMapping("/seckill-goods/apply/page")
    @ApiOperationLog(description = "分页查询秒杀商品申请")
    public Response<PageResult<SeckillGoodsApplyVO>> listSeckillGoodsApply(
            @RequestParam(value = "activityId", required = false) Long activityId,
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        SeckillGoodsApplyQueryRequest query = SeckillGoodsApplyQueryRequest.builder()
                .activityId(activityId)
                .auditStatus(auditStatus)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        return seckillGoodsService.listApplyPage(query);
    }

    @SaIgnore
    @GetMapping("/seckill-goods/{seckillGoodsId}")
    @ApiOperationLog(description = "秒杀商品详情（单规格，无需选规格）")
    public Response<SeckillGoodsApplyVO> getSeckillGoodsDetail(@PathVariable Long seckillGoodsId) {
        return seckillGoodsService.getSeckillGoodsDetail(seckillGoodsId);
    }

    @SaCheckRole("admin")
    @PostMapping("/{activityId}/start")
    @ApiOperationLog(description = "开始秒杀活动")
    public Response<SeckillActivityVO> startActivity(@PathVariable Long activityId) {
        log.info("==> 手动开始秒杀活动, activityId={}", activityId);
        return seckillActivityService.startActivity(activityId);
    }

    @SaCheckRole("admin")
    @PostMapping("/{activityId}/end")
    @ApiOperationLog(description = "结束秒杀活动")
    public Response<Boolean> endActivity(@PathVariable Long activityId) {
        log.info("==> 手动结束秒杀活动, activityId={}", activityId);
        return seckillActivityService.endActivity(activityId);
    }

    // ======================== 死信队列人工处理（管理员） ========================

    @SaCheckRole("admin")
    @GetMapping("/admin/failed-orders")
    @ApiOperationLog(description = "查询DLQ最终失败的订单列表（人工处理）")
    public Response<PageResult<?>> listFailedOrders(
            @RequestParam(value = "handleStatus", required = false) Integer handleStatus,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return seckillFailedOrderService.listFailedOrders(handleStatus, pageNum, pageSize);
    }

    @SaCheckRole("admin")
    @PostMapping("/admin/failed-orders/{id}/retry")
    @ApiOperationLog(description = "手动重试DLQ失败订单（重新投递到主队列）")
    public Response<Void> manualRetryFailedOrder(@PathVariable Long id) {
        String operator = String.valueOf(StpUtil.getLoginIdAsLong());
        log.info("==> 管理员手动重试失败订单, id={}, operator={}", id, operator);
        return seckillFailedOrderService.manualRetry(id, operator);
    }

    @SaCheckRole("admin")
    @PostMapping("/admin/failed-orders/{id}/discard")
    @ApiOperationLog(description = "手动放弃DLQ失败订单（标记为已处理）")
    public Response<Void> manualDiscardFailedOrder(@PathVariable Long id,
                                                    @RequestParam(value = "remark", defaultValue = "") String remark) {
        String operator = String.valueOf(StpUtil.getLoginIdAsLong());
        log.info("==> 管理员手动放弃失败订单, id={}, operator={}, remark={}", id, operator, remark);
        return seckillFailedOrderService.manualDiscard(id, remark, operator);
    }
}
