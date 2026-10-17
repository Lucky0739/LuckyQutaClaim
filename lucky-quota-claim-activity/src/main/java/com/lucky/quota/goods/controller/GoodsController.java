package com.lucky.quota.goods.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.lucky.quota.common.aspect.ApiOperationLog;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.model.file.UploadPictureResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.model.dto.AddGoodsRequest;
import com.lucky.quota.goods.model.dto.GoodsQueryRequest;
import com.lucky.quota.goods.model.entity.Goods;
import com.lucky.quota.goods.model.entity.GoodsSku;
import com.lucky.quota.goods.model.vo.GoodsDetailVO;
import com.lucky.quota.goods.model.vo.GoodsListVO;
import com.lucky.quota.goods.service.GoodsService;
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
    @ApiOperationLog(description = "新增物资")
    public Response<Long> addGoods(@Validated @RequestBody AddGoodsRequest request) {
        log.info("==> 新增物资请求, goodsName: {}, skuCount: {}",
                request.getGoodsName(),
                request.getSkuList() != null ? request.getSkuList().size() : 0);
        return goodsService.addGoods(request);
    }

    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @PostMapping("/img/upload")
    @ApiOperationLog(description = "上传物资图片")
    public Response<UploadPictureResult> uploadGoodsImg(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("==> 上传物资图片失败，文件为空");
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "请上传图片");
        }
        log.info("==> 上传物资图片请求");
        return goodsService.uploadGoodsImg(file);
    }

    @GetMapping("/get/{id}")
    @ApiOperationLog(description = "查询物资信息")
    public Response<Goods> getGoodsById(@PathVariable("id") Long id) {
        log.info("==> 查询物资信息请求, id: {}", id);
        return goodsService.getGoodsById(id);
    }

    @GetMapping("/detail/{goodsId}")
    @ApiOperationLog(description = "查看物资详情")
    public Response<GoodsDetailVO> getGoodsDetail(@PathVariable("goodsId") Long goodsId) {
        log.info("==> 查看物资详情请求, goodsId: {}", goodsId);
        return goodsService.getGoodsDetail(goodsId);
    }

    /**
     * 分页查询物资（分类/发放单位/名称筛选，按创建时间倒序）
     */
    @GetMapping("/page")
    @ApiOperationLog(description = "分页查询物资列表")
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
        log.info("==> 分页查询物资, parentCategoryId: {}, categoryId: {}, shopId: {}",
                parentCategoryId, categoryId, shopId);
        return goodsService.getGoodsPage(request);
    }

    /**
     * 按发放单位 ID 分页查询物资（发放单位详情页，与 GET /goods/page?shopId= 等价）
     */
    @GetMapping("/shop/{shopId}")
    @ApiOperationLog(description = "按发放单位分页查询物资")
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
        log.info("==> 按发放单位分页查询物资, shopId: {}, status: {}, auditStatus: {}",
                shopId, status, auditStatus);
        return goodsService.getGoodsPage(request);
    }

    @SaCheckRole("merchant")
    @PostMapping("/applyOnShelf")
    @ApiOperationLog(description = "申请上架物资")
    public Response<Void> applyOnShelf(@RequestParam Long goodsId) {
        log.info("==> 申请上架物资请求, goodsId: {}", goodsId);
        return goodsService.applyOnShelf(goodsId);
    }

    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @PostMapping("/offShelf/{goodsId}")
    @ApiOperationLog(description = "下架物资")
    public Response<Void> offShelf(@PathVariable("goodsId") Long goodsId) {
        log.info("==> 下架物资请求, goodsId: {}", goodsId);
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
