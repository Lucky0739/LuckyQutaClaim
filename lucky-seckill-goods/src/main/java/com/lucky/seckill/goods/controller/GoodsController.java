package com.lucky.seckill.goods.controller;

import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.model.dto.AddGoodsRequest;
import com.lucky.seckill.goods.model.dto.GoodsSkuRequest;
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

    @PostMapping("/add")
    @ApiOperationLog(description = "新增商品")
    public Response<Long> addGoods(@Validated @RequestBody AddGoodsRequest request) {
        log.info("==> 新增商品请求, goodsName: {}, skuCount: {}",
                request.getGoodsName(),
                request.getSkuList() != null ? request.getSkuList().size() : 0);
        return goodsService.addGoods(request);
    }


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
}