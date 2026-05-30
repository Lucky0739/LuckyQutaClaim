package com.lucky.seckill.goods.service;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.model.dto.GoodsCollectRequest;
import com.lucky.seckill.goods.model.vo.GoodsCollectStatusVO;
import com.lucky.seckill.goods.model.vo.GoodsCollectVO;

import java.util.List;

public interface GoodsCollectService {

    Response<Void> collectGoods(GoodsCollectRequest request);

    Response<Void> cancelCollect(GoodsCollectRequest request);

    Response<GoodsCollectStatusVO> getCollectStatus(Long goodsId);

    Response<List<GoodsCollectVO>> getMyCollectList();

    void syncCollectCountToDb();
}
