package com.lucky.seckill.shop.service;

import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.shop.model.dto.CreateShopRequest;
import com.lucky.seckill.shop.model.dto.ShopAuditRequest;
import org.springframework.web.multipart.MultipartFile;

public interface ShopService {

    Response<?> createShop(CreateShopRequest request);

    Response<?> auditShop(ShopAuditRequest shopAuditRequest);

    Response<?> getPendingApplyList();

    /**
     * 上传店铺头像（开店申请前仅上传 COS；已有店铺则同步更新数据库）
     */
    Response<UploadPictureResult> uploadShopAvatar(MultipartFile file);
}
