package com.lucky.quota.goods.elasticsearch.service;

import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.goods.model.dto.GoodsSearchRequest;
import com.lucky.quota.goods.model.vo.GoodsListVO;

import java.util.List;

/**
 * 物资 Elasticsearch 索引与搜索
 */
public interface GoodsElasticsearchService {

    boolean isEnabled();

    /**
     * 按物资 ID 同步索引（不可搜索则从索引删除）
     */
    void syncGoods(Long goodsId);

    /**
     * 从索引删除物资
     */
    void removeGoods(Long goodsId);

    /**
     * 全量重建可搜索物资索引
     */
    void rebuildIndex();

    /**
     * ES 分页搜索，失败时由调用方降级
     */
    PageResult<GoodsListVO> search(String keyword, List<Long> categoryIds, GoodsSearchRequest request);
}
