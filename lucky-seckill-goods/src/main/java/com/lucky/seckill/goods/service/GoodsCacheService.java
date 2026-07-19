package com.lucky.seckill.goods.service;

import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.goods.model.dto.GoodsQueryRequest;
import com.lucky.seckill.goods.model.entity.Goods;
import com.lucky.seckill.goods.model.vo.GoodsDetailVO;
import com.lucky.seckill.goods.model.vo.GoodsListVO;

import java.util.function.Supplier;

/**
 * 商品缓存服务：逻辑过期 + 互斥锁，防止缓存击穿
 */
public interface GoodsCacheService {

    PageResult<GoodsListVO> getGoodsPage(GoodsQueryRequest request, Supplier<PageResult<GoodsListVO>> dbLoader);

    Goods getGoodsById(Long goodsId, Supplier<Goods> dbLoader);

    GoodsDetailVO getGoodsDetail(Long goodsId, Supplier<GoodsDetailVO> dbLoader);

    /**
     * 清除全部分页缓存
     */
    void clearPageCache();

    /**
     * 清除指定商品的基础信息与详情缓存
     */
    void evictGoodsCache(Long goodsId);

    /**
     * 清除全部商品相关缓存（分页 + 单条）
     */
    void clearAllGoodsCache();
}
