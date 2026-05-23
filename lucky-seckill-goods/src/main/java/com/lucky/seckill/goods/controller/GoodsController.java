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
import com.lucky.seckill.goods.model.entity.GoodsSku;
import com.lucky.seckill.goods.model.vo.GoodsDetailVO;
import com.lucky.seckill.goods.model.vo.GoodsListVO;
import com.lucky.seckill.goods.model.vo.SkuImgVO;
import com.lucky.seckill.goods.service.GoodsService;
import jakarta.annotation.Resource;
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
     * 上架商品
     * @param goodsId
     * @return
     */
    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @PostMapping("/onShelf/{goodsId}")
    @ApiOperationLog(description = "上架商品")
    public Response<Void> onShelf(@PathVariable("goodsId") Long goodsId) {
        log.info("==> 上架商品请求, goodsId: {}", goodsId);
        return goodsService.onShelf(goodsId);
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

}