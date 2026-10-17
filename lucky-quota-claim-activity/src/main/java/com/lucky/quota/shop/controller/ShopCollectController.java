package com.lucky.quota.shop.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.lucky.quota.common.aspect.ApiOperationLog;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.shop.model.dto.ShopCollectRequest;
import com.lucky.quota.shop.model.vo.ShopCollectStatusVO;
import com.lucky.quota.shop.model.vo.ShopCollectVO;
import com.lucky.quota.shop.service.ShopCollectService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 发放单位收藏控制器
 */
@RestController
@RequestMapping("/shop/collect")
@Slf4j
public class ShopCollectController {

    @Resource
    private ShopCollectService shopCollectService;

    /**
     * 收藏发放单位
     */
    @SaCheckLogin
    @PostMapping("/add")
    @ApiOperationLog(description = "收藏发放单位")
    public Response<Void> collectShop(@Validated @RequestBody ShopCollectRequest request) {
        log.info("==> 收藏发放单位请求, shopId: {}", request.getShopId());
        return shopCollectService.collectShop(request);
    }

    /**
     * 取消收藏发放单位
     */
    @SaCheckLogin
    @PostMapping("/cancel")
    @ApiOperationLog(description = "取消收藏发放单位")
    public Response<Void> cancelCollect(@Validated @RequestBody ShopCollectRequest request) {
        log.info("==> 取消收藏发放单位请求, shopId: {}", request.getShopId());
        return shopCollectService.cancelCollect(request);
    }

    /**
     * 查询发放单位收藏状态（含收藏数，登录用户返回是否已收藏）
     */
    @GetMapping("/status/{shopId}")
    @ApiOperationLog(description = "查询发放单位收藏状态")
    public Response<ShopCollectStatusVO> getCollectStatus(@PathVariable("shopId") Long shopId) {
        log.info("==> 查询发放单位收藏状态请求, shopId: {}", shopId);
        return shopCollectService.getCollectStatus(shopId);
    }

    /**
     * 查询我的发放单位收藏列表
     */
    @SaCheckLogin
    @GetMapping("/list")
    @ApiOperationLog(description = "查询我的发放单位收藏列表")
    public Response<List<ShopCollectVO>> getMyCollectList() {
        log.info("==> 查询我的发放单位收藏列表请求");
        return shopCollectService.getMyCollectList();
    }
}
