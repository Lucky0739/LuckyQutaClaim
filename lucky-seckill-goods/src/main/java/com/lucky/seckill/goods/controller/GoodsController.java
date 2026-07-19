package com.lucky.seckill.goods.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.model.dto.AddGoodsRequest;
import com.lucky.seckill.goods.model.dto.GoodsQueryRequest;
import com.lucky.seckill.goods.model.entity.Goods;
import com.lucky.seckill.goods.model.entity.GoodsSku;
import com.lucky.seckill.goods.model.vo.GoodsDetailVO;
import com.lucky.seckill.goods.model.vo.GoodsListVO;
import com.lucky.seckill.goods.service.GoodsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/goods")
@Slf4j
public class GoodsController {

    @Resource
    private GoodsService goodsService;

    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @PostMapping("/add")
    @ApiOperationLog(description = "新增商品")
    public Response<Long> addGoods(@Validated @RequestBody AddGoodsRequest request) {
        log.info("==> 新增商品请求, goodsName: {}, skuCount: {}",
                request.getGoodsName(),
                request.getSkuList() != null ? request.getSkuList().size() : 0);
        return goodsService.addGoods(request);
    }

    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @PostMapping("/img/upload")
    @ApiOperationLog(description = "上传商品图片")
    public Response<UploadPictureResult> uploadGoodsImg(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("==> 上传商品图片失败，文件为空");
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "请上传图片");
        }
        log.info("==> 上传商品图片请求");
        return goodsService.uploadGoodsImg(file);
    }

    @GetMapping("/get/{id}")
    @ApiOperationLog(description = "查询商品信息")
    public Response<Goods> getGoodsById(@PathVariable("id") Long id) {
        log.info("==> 查询商品信息请求, id: {}", id);
        return goodsService.getGoodsById(id);
    }

    @GetMapping("/detail/{goodsId}")
    @ApiOperationLog(description = "查看商品详情")
    public Response<GoodsDetailVO> getGoodsDetail(@PathVariable("goodsId") Long goodsId) {
        log.info("==> 查看商品详情请求, goodsId: {}", goodsId);
        return goodsService.getGoodsDetail(goodsId);
    }

    /**
     * 分页查询商品（分类/店铺/名称筛选，按创建时间倒序）
     */
    @GetMapping("/page")
    @ApiOperationLog(description = "分页查询商品列表")
    public Response<PageResult<GoodsListVO>> getGoodsPage(
            @RequestParam(value = "parentCategoryId", required = false) Long parentCategoryId,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "goodsName", required = false) String goodsName,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus,
            @RequestParam(value = "shopId", required = false) Long shopId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        GoodsQueryRequest request = GoodsQueryRequest.builder()
                .parentCategoryId(parentCategoryId)
                .categoryId(categoryId)
                .goodsName(goodsName)
                .status(status)
                .auditStatus(auditStatus)
                .shopId(shopId)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        log.info("==> 分页查询商品, parentCategoryId: {}, categoryId: {}, shopId: {}",
                parentCategoryId, categoryId, shopId);
        return goodsService.getGoodsPage(request);
    }

    /**
     * 按店铺 ID 分页查询商品（店铺详情页，与 GET /goods/page?shopId= 等价）
     */
    @GetMapping("/shop/{shopId}")
    @ApiOperationLog(description = "按店铺分页查询商品")
    public Response<PageResult<GoodsListVO>> getGoodsPageByShopId(
            @PathVariable("shopId") Long shopId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        GoodsQueryRequest request = GoodsQueryRequest.builder()
                .shopId(shopId)
                .status(status)
                .auditStatus(auditStatus)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        log.info("==> 按店铺分页查询商品, shopId: {}, status: {}, auditStatus: {}",
                shopId, status, auditStatus);
        return goodsService.getGoodsPage(request);
    }

    @SaCheckRole("merchant")
    @PostMapping("/applyOnShelf")
    @ApiOperationLog(description = "申请上架商品")
    public Response<Void> applyOnShelf(@RequestParam Long goodsId) {
        log.info("==> 申请上架商品请求, goodsId: {}", goodsId);
        return goodsService.applyOnShelf(goodsId);
    }

    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @PostMapping("/offShelf/{goodsId}")
    @ApiOperationLog(description = "下架商品")
    public Response<Void> offShelf(@PathVariable("goodsId") Long goodsId) {
        log.info("==> 下架商品请求, goodsId: {}", goodsId);
        return goodsService.offShelf(goodsId);
    }

    @GetMapping("/sku/{skuId}")
    @ApiOperationLog(description = "查询SKU信息")
    public Response<GoodsSku> getSkuById(@PathVariable("skuId") Long skuId) {
        log.info("==> 查询SKU信息请求, skuId: {}", skuId);
        return goodsService.getSkuById(skuId);
    }

    @PostMapping("/sku/lockStock")
    @ApiOperationLog(description = "预占库存")
    public Response<Boolean> lockStock(@RequestParam Long skuId, @RequestParam Integer buyGoodsNum) {
        log.info("==> 预占库存请求, skuId: {}, stockNum: {}", skuId, buyGoodsNum);
        return goodsService.lockStock(skuId, buyGoodsNum);
    }

    @PostMapping("/sku/releaseStock")
    @ApiOperationLog(description = "释放库存")
    public Response<Boolean> releaseStock(@RequestParam Long skuId, @RequestParam Integer stockNum) {
        log.info("==> 释放库存请求, skuId: {}, stockNum: {}", skuId, stockNum);
        return goodsService.releaseStock(skuId, stockNum);
    }

    @PostMapping("/sku/deductStock")
    @ApiOperationLog(description = "扣减库存")
    public Response<Boolean> deductStock(@RequestParam Long skuId, @RequestParam Integer stockNum) {
        log.info("==> 扣减库存请求, skuId: {}, stockNum: {}", skuId, stockNum);
        return goodsService.deductStock(skuId, stockNum);
    }
}
