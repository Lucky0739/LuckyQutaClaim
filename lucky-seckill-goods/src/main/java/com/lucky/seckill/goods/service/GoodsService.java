package com.lucky.seckill.goods.service;

import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.model.dto.AddGoodsRequest;
import com.lucky.seckill.goods.model.dto.GoodsAuditRequest;
import com.lucky.seckill.goods.model.dto.GoodsQueryRequest;
import com.lucky.seckill.goods.model.dto.GoodsSkuRequest;
import com.lucky.seckill.goods.model.entity.Goods;
import com.lucky.seckill.goods.model.entity.GoodsSku;
import com.lucky.seckill.goods.model.vo.GoodsDetailVO;
import com.lucky.seckill.goods.model.vo.GoodsListVO;
import com.lucky.seckill.goods.model.vo.SkuImgVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GoodsService {
    /**
     * 新增商品
     * @param request
     * @return
     */
    Response<Long> addGoods(AddGoodsRequest request);

    /**
     * 上传商品图片
     * @param file
     * @return
     */
    Response<UploadPictureResult> uploadGoodsImg(MultipartFile file);


    /**
     * 查看商品详情
     * @param goodsId
     * @return
     */
    Response<GoodsDetailVO> getGoodsDetail(Long goodsId);

    /**
     * 查看规格对应的图片
     * @param goodsId
     * @return
     */
    Response<List<SkuImgVO>> getSkuImages(Long goodsId);

    /**
     * 分页查询商品列表
     * @param goodsQueryRequest
     * @return
     */
    Response<PageResult<GoodsListVO>> getGoodsPage(GoodsQueryRequest goodsQueryRequest);
    /**
     * 申请上架商品
     * @param goodsId 商品ID
     * @return
     */
    Response<Void> applyOnShelf(Long goodsId);

    /**
     * 管理员审核商品上架申请
     * @param request 审核请求
     * @return
     */
    Response<Void> auditGoods(GoodsAuditRequest request);

    /**
     * 下架商品
     * @param goodsId 商品ID
     * @return
     */
    Response<Void> offShelf(Long goodsId);

    /**
     * 根据商品ID查询商品信息
     * @param goodsId 商品ID
     * @return 商品信息
     */
    Response<Goods> getGoodsById(Long goodsId);

    /**
     * 根据SKU ID查询SKU信息
     * @param skuId SKU ID
     * @return
     */
    Response<GoodsSku> getSkuById(Long skuId);

    /**
     * 预占库存（创建订单时调用，使用行锁防止超卖）
     * @param skuId SKU ID
     * @param buyGoodsNum 预占数量
     * @return true=预占成功，false=库存不足
     */
    Response<Boolean> lockStock(Long skuId, Integer buyGoodsNum);


    /**
     * 释放预占的库存（订单取消或超时）
     * @param skuId SKU ID
     * @param stockNum 释放数量
     * @return true=释放成功
     */
    Response<Boolean> releaseStock(Long skuId, Integer stockNum);

    /**
     * 扣减库存（支付成功后调用，只增加已售数量）
     * @param skuId SKU ID
     * @param stockNum 扣减数量
     * @return true=扣减成功
     */
    Response<Boolean> deductStock(Long skuId, Integer stockNum);

}