package com.lucky.seckill.activity.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import com.lucky.seckill.activity.model.dto.SeckillRequest;
import com.lucky.seckill.activity.model.vo.SeckillActivityVO;
import com.lucky.seckill.activity.model.vo.SeckillGoodsVO;
import com.lucky.seckill.activity.model.vo.SeckillResultVO;
import com.lucky.seckill.activity.service.SeckillActivityService;
import com.lucky.seckill.activity.service.SeckillService;
import com.lucky.seckill.common.aspect.ApiOperationLog;
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
        log.info("==> 秒杀请求, activityId={}, goodsId={}, skuId={}",
                request.getActivityId(), request.getGoodsId(), request.getSkuId());
        return seckillService.doSeckill(request);
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
}
