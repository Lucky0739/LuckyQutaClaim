package com.lucky.seckill.goods.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.model.dto.GoodsCollectRequest;
import com.lucky.seckill.goods.model.vo.GoodsCollectStatusVO;
import com.lucky.seckill.goods.model.vo.GoodsCollectVO;
import com.lucky.seckill.goods.service.GoodsCollectService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品收藏控制器
 */
@RestController
@RequestMapping("/goods/collect")
@Slf4j
public class GoodsCollectController {

    @Resource
    private GoodsCollectService goodsCollectService;

    /**
     * 收藏商品
     */
    @SaCheckLogin
    @PostMapping("/add")
    @ApiOperationLog(description = "收藏商品")
    public Response<Void> collectGoods(@Validated @RequestBody GoodsCollectRequest request) {
        log.info("==> 收藏商品请求, goodsId: {}", request.getGoodsId());
        return goodsCollectService.collectGoods(request);
    }

    /**
     * 取消收藏商品
     */
    @SaCheckLogin
    @PostMapping("/cancel")
    @ApiOperationLog(description = "取消收藏商品")
    public Response<Void> cancelCollect(@Validated @RequestBody GoodsCollectRequest request) {
        log.info("==> 取消收藏商品请求, goodsId: {}", request.getGoodsId());
        return goodsCollectService.cancelCollect(request);
    }

    /**
     * 查询商品收藏状态（含收藏数，登录用户返回是否已收藏）
     */
    @GetMapping("/status/{goodsId}")
    @ApiOperationLog(description = "查询商品收藏状态")
    public Response<GoodsCollectStatusVO> getCollectStatus(@PathVariable("goodsId") Long goodsId) {
        log.info("==> 查询商品收藏状态请求, goodsId: {}", goodsId);
        return goodsCollectService.getCollectStatus(goodsId);
    }

    /**
     * 查询我的商品收藏列表
     */
    @SaCheckLogin
    @GetMapping("/list")
    @ApiOperationLog(description = "查询我的商品收藏列表")
    public Response<List<GoodsCollectVO>> getMyCollectList() {
        log.info("==> 查询我的商品收藏列表请求");
        return goodsCollectService.getMyCollectList();
    }
}
