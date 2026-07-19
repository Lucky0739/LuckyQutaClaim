package com.lucky.seckill.goods.service;

import com.lucky.seckill.goods.enums.GoodsRankSortEnum;

import java.util.List;

/**
 * 首页商品排行 Redis ZSet 服务
 */
public interface GoodsRankRedisService {

    void updateCollectScore(Long goodsId, int collectNum);

    void updateSalesScore(Long goodsId, int salesNum);

    /**
     * 同步收藏、销量及综合权重三个 ZSet
     */
    void updateGoodsRankScores(Long goodsId, int collectNum, int salesNum);

    /**
     * 按 DB/Redis 最新数据刷新上架商品的排行（含权重 ZSet）
     */
    void refreshGoodsRankIfOnShelf(Long goodsId);

    void removeFromRank(Long goodsId);

    List<Long> listGoodsIdsByRank(GoodsRankSortEnum sortType, long offset, long limit);

    long countRank(GoodsRankSortEnum sortType);

    void rebuildRankFromDb();
}
