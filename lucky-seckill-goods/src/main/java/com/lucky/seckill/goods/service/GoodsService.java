package com.lucky.seckill.goods.service;

import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.model.dto.AddGoodsRequest;
import com.lucky.seckill.goods.model.dto.GoodsQueryRequest;
import com.lucky.seckill.goods.model.dto.GoodsSkuRequest;
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
     * 上架商品
     * @param goodsId 商品ID
     * @return
     */
    Response<Void> onShelf(Long goodsId);

    /**
     * 下架商品
     * @param goodsId 商品ID
     * @return
     */
    Response<Void> offShelf(Long goodsId);

    /**
     * 根据SKU ID查询SKU信息
     * @param skuId SKU ID
     * @return
     */
    Response<GoodsSku> getSkuById(Long skuId);

}