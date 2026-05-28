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
import com.lucky.seckill.goods.model.vo.SkuImgVO;
import com.lucky.seckill.goods.service.GoodsService;
import jakarta.annotation.Resource;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/goods")
@Slf4j
public class GoodsController {

    @Resource
    private GoodsService goodsService;

    /**
     * 新增商品
     * @param request
     * @return
     */
    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @PostMapping("/add")
    @ApiOperationLog(description = "新增商品")
    public Response<Long> addGoods(@Validated @RequestBody AddGoodsRequest request) {
        log.info("==> 新增商品请求, goodsName: {}, skuCount: {}",
                request.getGoodsName(),
                request.getSkuList() != null ? request.getSkuList().size() : 0);
        return goodsService.addGoods(request);
    }

    /**
     * 上传商品图片
     * @param file
     * @return
     */
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

    /**
     * 根据商品ID查询商品信息
     * @param id 商品ID
     * @return 商品信息
     */
    @GetMapping("/get/{id}")
    @ApiOperationLog(description = "查询商品信息")
    public Response<Goods> getGoodsById(@PathVariable("id") Long id) {
        log.info("==> 查询商品信息请求, id: {}", id);
        return goodsService.getGoodsById(id);
    }

    /**
     * 查看商品详情
     * @param goodsId
     * @return
     */
    @GetMapping("/detail/{goodsId}")
    @ApiOperationLog(description = "查看商品详情")
    public Response<GoodsDetailVO> getGoodsDetail(@PathVariable("goodsId") Long goodsId) {
        log.info("==> 查看商品详情请求, goodsId: {}", goodsId);
        return goodsService.getGoodsDetail(goodsId);
    }

    /**
     * 查看规格对应的图片
     * @param goodsId
     * @return
     */
    @GetMapping("/sku/images/{goodsId}")
    @ApiOperationLog(description = "查看规格图片")
    public Response<List<SkuImgVO>> getSkuImages(@PathVariable("goodsId") Long goodsId) {
        log.info("==> 查看规格图片请求, goodsId: {}", goodsId);
        return goodsService.getSkuImages(goodsId);
    }

    /**
     * 分页查询商品列表
     * @param goodsQueryRequest
     * @return
     */
    @PostMapping("/page")
    @ApiOperationLog(description = "分页查询商品列表")
    public Response<PageResult<GoodsListVO>> getGoodsPage(@RequestBody GoodsQueryRequest goodsQueryRequest) {
        log.info("==> 分页查询商品列表请求, pageNum: {}, pageSize: {}",
                goodsQueryRequest.getPageNum(), goodsQueryRequest.getPageSize());
        return goodsService.getGoodsPage(goodsQueryRequest);
    }
    /**
     * 申请上架商品
     * @param goodsId
     * @return
     */
    @SaCheckRole("merchant")
    @PostMapping("/applyOnShelf")
    @ApiOperationLog(description = "申请上架商品")
    public Response<Void> applyOnShelf(@RequestParam Long goodsId) {
        log.info("==> 申请上架商品请求, goodsId: {}", goodsId);
        return goodsService.applyOnShelf(goodsId);
    }

    /**
     * 下架商品
     * @param goodsId
     * @return
     */
    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @PostMapping("/offShelf/{goodsId}")
    @ApiOperationLog(description = "下架商品")
    public Response<Void> offShelf(@PathVariable("goodsId") Long goodsId) {
        log.info("==> 下架商品请求, goodsId: {}", goodsId);
        return goodsService.offShelf(goodsId);
    }

    /**
     * 根据SKU ID查询SKU信息
     * @param skuId
     * @return
     */
    @GetMapping("/sku/{skuId}")
    @ApiOperationLog(description = "查询SKU信息")
    public Response<GoodsSku> getSkuById(@PathVariable("skuId") Long skuId) {
        log.info("==> 查询SKU信息请求, skuId: {}", skuId);
        return goodsService.getSkuById(skuId);
    }

    /**
     * 预占库存（创建订单时调用，使用行锁防止超卖）
     * @param skuId SKU ID
     * @param buyGoodsNum 预占数量
     * @return true=预占成功，false=库存不足
     */
    @PostMapping("/sku/lockStock")
    @ApiOperationLog(description = "预占库存")
    public Response<Boolean> lockStock(@RequestParam Long skuId, @RequestParam Integer buyGoodsNum) {
        log.info("==> 预占库存请求, skuId: {}, stockNum: {}", skuId, buyGoodsNum);
        return goodsService.lockStock(skuId, buyGoodsNum);
    }


    /**
     * 释放预占的库存（订单取消或超时）
     * @param skuId SKU ID
     * @param stockNum 释放数量
     * @return true=释放成功
     */
    @PostMapping("/sku/releaseStock")
    @ApiOperationLog(description = "释放库存")
    public Response<Boolean> releaseStock(@RequestParam Long skuId, @RequestParam Integer stockNum) {
        log.info("==> 释放库存请求, skuId: {}, stockNum: {}", skuId, stockNum);
        return goodsService.releaseStock(skuId, stockNum);
    }



}