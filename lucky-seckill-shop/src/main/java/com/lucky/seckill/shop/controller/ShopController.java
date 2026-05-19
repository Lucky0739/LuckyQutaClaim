package com.lucky.seckill.shop.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.shop.model.dto.CreateShopRequest;
import com.lucky.seckill.shop.model.dto.ShopAuditRequest;
import com.lucky.seckill.shop.service.ShopService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 店铺控制器
 */
@RestController
@RequestMapping("/shop")
@Slf4j
public class ShopController {
    @Resource
    private ShopService shopService;

    /**
     * 上传店铺头像（开店申请前上传，将返回的 imageUrl 填入开店申请的 shopAvatar 字段）
     */
    @SaCheckLogin
    @PostMapping("/avatar/upload")
    @ApiOperationLog(description = "上传店铺头像")
    public Response<UploadPictureResult> uploadShopAvatar(@RequestParam("file") MultipartFile file) {
        return shopService.uploadShopAvatar(file);
    }

    @SaCheckLogin
    @PostMapping("/create")
    public Response<?> createShop(@Validated @RequestBody CreateShopRequest request) {
        log.info("==> 接收到开店请求, shopName: {}", request.getShopName());
        return shopService.createShop(request);
    }

    @PostMapping("/audit")
    public Response<?> auditShop(@Validated @RequestBody ShopAuditRequest request) {
        log.info("==> 接收到店铺审批请求, shopId: {}, auditStatus: {}",
                request.getShopId(), request.getAuditStatus());
        return shopService.auditShop(request);
    }
}
