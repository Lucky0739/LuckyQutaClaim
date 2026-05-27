package com.lucky.seckill.shop.service;

import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.shop.model.dto.CreateShopRequest;
import com.lucky.seckill.shop.model.dto.ShopApplyQueryRequest;
import com.lucky.seckill.shop.model.dto.ShopAuditRequest;
import com.lucky.seckill.shop.model.vo.ShopApplyVO;
import org.springframework.web.multipart.MultipartFile;

public interface ShopService {

    Response<?> createShop(CreateShopRequest request);

    Response<?> auditShop(ShopAuditRequest shopAuditRequest);


    /**
     * 上传店铺头像（开店申请前仅上传 COS；已有店铺则同步更新数据库）
     */
    Response<UploadPictureResult> uploadShopAvatar(MultipartFile file);

    Response<PageResult<ShopApplyVO>> getShopApplyList(ShopApplyQueryRequest queryRequest);

    /**
     * 商家查看自己的店铺列表
     */
    Response<PageResult<ShopApplyVO>> getMyShopList();

    /**
     * 管理员分页查询店铺列表（支持多条件筛选）
     */
    Response<PageResult<ShopApplyVO>> getShopList(ShopApplyQueryRequest queryRequest);

    /**
     * 禁用/启用店铺
     * @param shopId 店铺ID
     * @param shopStatus 店铺状态 0-禁用 1-启用
     */
    Response<?> updateShopStatus(Long shopId, Integer shopStatus);
}
