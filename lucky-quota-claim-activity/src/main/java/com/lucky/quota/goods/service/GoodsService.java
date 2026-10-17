package com.lucky.quota.goods.service;

import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.model.file.UploadPictureResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.model.dto.AddGoodsRequest;
import com.lucky.quota.goods.model.dto.GoodsAuditRequest;
import com.lucky.quota.goods.model.dto.GoodsHomeQueryRequest;
import com.lucky.quota.goods.model.dto.GoodsQueryRequest;
import com.lucky.quota.goods.model.dto.GoodsSearchRequest;
import com.lucky.quota.goods.model.dto.UpdateGoodsRequest;
import com.lucky.quota.goods.model.entity.Goods;
import com.lucky.quota.goods.model.entity.GoodsSku;
import com.lucky.quota.goods.model.vo.GoodsDetailVO;
import com.lucky.quota.goods.model.vo.GoodsListVO;
import com.lucky.quota.goods.model.vo.MerchantGoodsDetailVO;
import com.lucky.quota.goods.model.vo.MerchantGoodsListVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GoodsService {
    /**
     * 新增物资
     * @param request
     * @return
     */
    Response<Long> addGoods(AddGoodsRequest request);

    /**
     * 上传物资图片
     * @param file
     * @return
     */
    Response<UploadPictureResult> uploadGoodsImg(MultipartFile file);


    /**
     * 查看物资详情
     * @param goodsId
     * @return
     */
    Response<GoodsDetailVO> getGoodsDetail(Long goodsId);

    /**
     * 分页查询物资列表
     * @param goodsQueryRequest
     * @return
     */
    Response<PageResult<GoodsListVO>> getGoodsPage(GoodsQueryRequest goodsQueryRequest);

    /**
     * 管理员待审核物资列表（直连数据库，不走分页缓存，保证申请上架后立即可见）
     */
    Response<PageResult<GoodsListVO>> getPendingAuditGoodsPage(GoodsQueryRequest request);

    /**
     * 管理员已审核物资列表（直连数据库）
     */
    Response<PageResult<GoodsListVO>> getAuditedGoodsPage(GoodsQueryRequest request);

    /**
     * 首页物资列表（按收藏量/已售数量排序，数据来自 Redis ZSet）
     */
    Response<PageResult<GoodsListVO>> getHomeGoodsList(GoodsHomeQueryRequest request);

    /**
     * 物资搜索（优先 Elasticsearch，不可用时降级 MySQL）
     */
    Response<PageResult<GoodsListVO>> searchGoods(GoodsSearchRequest request);

    /**
     * 申请上架物资
     * @param goodsId 物资ID
     * @return
     */
    Response<Void> applyOnShelf(Long goodsId);

    /**
     * 管理员审核物资上架申请
     * @param request 审核请求
     * @return
     */
    Response<Void> auditGoods(GoodsAuditRequest request);

    /**
     * 下架物资
     * @param goodsId 物资ID
     * @return
     */
    Response<Void> offShelf(Long goodsId);

    /**
     * 根据物资ID查询物资信息
     * @param goodsId 物资ID
     * @return 物资信息
     */
    Response<Goods> getGoodsById(Long goodsId);

    /**
     * 根据SKU ID查询SKU信息
     * @param skuId SKU ID
     * @return
     */
    Response<GoodsSku> getSkuById(Long skuId);

    /**
     * 预占库存（创建申领单时调用，使用行锁防止超卖）
     * @param skuId SKU ID
     * @param buyGoodsNum 预占数量
     * @return true=预占成功，false=库存不足
     */
    Response<Boolean> lockStock(Long skuId, Integer buyGoodsNum);


    /**
     * 释放预占的库存（申领单取消或超时）
     * @param skuId SKU ID
     * @param stockNum 释放数量
     * @return true=释放成功
     */
    Response<Boolean> releaseStock(Long skuId, Integer stockNum);

    /**
     * 扣减库存（申领确认后调用，只增加已售数量）
     * @param skuId SKU ID
     * @param stockNum 扣减数量
     * @return true=扣减成功
     */
    Response<Boolean> deductStock(Long skuId, Integer stockNum);

    /**
     * 商户分页查询自己发布的物资
     */
    Response<PageResult<MerchantGoodsListVO>> getMyGoodsPage(GoodsQueryRequest request);

    /**
     * 商户查看自己发布的物资详情
     */
    Response<MerchantGoodsDetailVO> getMyGoodsDetail(Long goodsId);

    /**
     * 商户更新自己发布的物资
     */
    Response<Void> updateMyGoods(UpdateGoodsRequest request);

    /**
     * 商户删除自己发布的物资（逻辑删除）
     */
    Response<Void> deleteMyGoods(Long goodsId);

}