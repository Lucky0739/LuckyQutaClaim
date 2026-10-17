package com.lucky.quota.shop.service;

import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.model.file.UploadPictureResult;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.model.vo.GoodsListVO;
import com.lucky.quota.shop.model.dto.CreateShopRequest;
import com.lucky.quota.shop.model.dto.ShopApplyQueryRequest;
import com.lucky.quota.shop.model.dto.ShopAuditRequest;
import com.lucky.quota.shop.model.vo.ShopApplyVO;
import com.lucky.quota.shop.model.vo.ShopListVO;
import org.springframework.web.multipart.MultipartFile;

public interface ShopService {

    Response<?> createShop(CreateShopRequest request);

    Response<?> auditShop(ShopAuditRequest shopAuditRequest);


    /**
     * 上传单位头像（申请发放单位前仅上传 COS；已有发放单位则同步更新数据库）
     */
    Response<UploadPictureResult> uploadShopAvatar(MultipartFile file);

    Response<PageResult<ShopApplyVO>> getShopApplyList(ShopApplyQueryRequest queryRequest);

    /**
     * 商家查看自己的发放单位列表
     */
    Response<PageResult<ShopApplyVO>> getMyShopList();

    /**
     * 商家删除自己的发放单位（软删除）
     */
    Response<?> deleteMyShop(Long shopId);

    /**
     * 管理员分页查询发放单位列表（支持多条件筛选）
     */
    Response<PageResult<ShopApplyVO>> getShopList(ShopApplyQueryRequest queryRequest);

    /**
     * 普通用户分页查询发放单位列表（仅返回公开字段）
     */
    Response<PageResult<ShopListVO>> getPublicShopList(ShopApplyQueryRequest queryRequest);

    /**
     * 禁用/启用发放单位
     * @param shopId 发放单位ID
     * @param shopStatus 发放单位状态 0-禁用 1-启用
     */
    Response<?> updateShopStatus(Long shopId, Integer shopStatus);

    /**
     * 按发放单位ID查询发放单位物资列表
     */
    Response<PageResult<GoodsListVO>> getShopGoods(Long shopId, Integer pageNum, Integer pageSize);

    /**
     * 根据发放单位ID查询发放单位信息
     */
    Response<ShopListVO> getShopById(Long shopId);
}
