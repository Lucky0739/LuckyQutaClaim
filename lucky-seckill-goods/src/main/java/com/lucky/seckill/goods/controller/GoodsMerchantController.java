package com.lucky.seckill.goods.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.model.dto.GoodsQueryRequest;
import com.lucky.seckill.goods.model.dto.UpdateGoodsRequest;
import com.lucky.seckill.goods.model.vo.MerchantGoodsDetailVO;
import com.lucky.seckill.goods.model.vo.MerchantGoodsListVO;
import com.lucky.seckill.goods.service.GoodsService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 商户管理自己发布的商品
 */
@RestController
@RequestMapping("/goods/my")
@Slf4j
@Validated
public class GoodsMerchantController {

    @Resource
    private GoodsService goodsService;

    @SaCheckRole("merchant")
    @GetMapping("/page")
    @ApiOperationLog(description = "分页查询我发布的商品")
    public Response<PageResult<MerchantGoodsListVO>> getMyGoodsPage(
            @RequestParam(value = "goodsName", required = false) String goodsName,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        GoodsQueryRequest request = GoodsQueryRequest.builder()
                .goodsName(goodsName)
                .status(status)
                .auditStatus(auditStatus)
                .categoryId(categoryId)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        return goodsService.getMyGoodsPage(request);
    }

    @SaCheckRole("merchant")
    @GetMapping("/detail/{goodsId}")
    @ApiOperationLog(description = "查看我发布的商品详情")
    public Response<MerchantGoodsDetailVO> getMyGoodsDetail(@PathVariable Long goodsId) {
        return goodsService.getMyGoodsDetail(goodsId);
    }

    @SaCheckRole("merchant")
    @PutMapping("/update")
    @ApiOperationLog(description = "更新我发布的商品")
    public Response<Void> updateMyGoods(@Valid @RequestBody UpdateGoodsRequest request) {
        return goodsService.updateMyGoods(request);
    }

    @SaCheckRole("merchant")
    @DeleteMapping("/{goodsId}")
    @ApiOperationLog(description = "删除我发布的商品")
    public Response<Void> deleteMyGoods(@PathVariable Long goodsId) {
        return goodsService.deleteMyGoods(goodsId);
    }
}
