package com.lucky.quota.goods.elasticsearch.service.impl;

import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.goods.elasticsearch.service.GoodsElasticsearchService;
import com.lucky.quota.goods.model.dto.GoodsSearchRequest;
import com.lucky.quota.goods.model.vo.GoodsListVO;

import java.util.List;

public class NoOpGoodsElasticsearchService implements GoodsElasticsearchService {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void syncGoods(Long goodsId) {
        // no-op
    }

    @Override
    public void removeGoods(Long goodsId) {
        // no-op
    }

    @Override
    public void rebuildIndex() {
        // no-op
    }

    @Override
    public PageResult<GoodsListVO> search(String keyword, List<Long> categoryIds, GoodsSearchRequest request) {
        throw new UnsupportedOperationException("Elasticsearch 未启用");
    }
}
