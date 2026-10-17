package com.lucky.quota.goods.service;

import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.model.dto.GoodsCollectRequest;
import com.lucky.quota.goods.model.vo.GoodsCollectStatusVO;
import com.lucky.quota.goods.model.vo.GoodsCollectVO;

import java.util.List;

public interface GoodsCollectService {

    Response<Void> collectGoods(GoodsCollectRequest request);

    Response<Void> cancelCollect(GoodsCollectRequest request);

    Response<GoodsCollectStatusVO> getCollectStatus(Long goodsId);

    Response<List<GoodsCollectVO>> getMyCollectList();

    void syncCollectCountToDb();
}
