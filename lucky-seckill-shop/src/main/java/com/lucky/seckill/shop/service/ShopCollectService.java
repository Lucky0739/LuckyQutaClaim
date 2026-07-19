package com.lucky.seckill.shop.service;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.shop.model.dto.ShopCollectRequest;
import com.lucky.seckill.shop.model.vo.ShopCollectStatusVO;
import com.lucky.seckill.shop.model.vo.ShopCollectVO;

import java.util.List;

public interface ShopCollectService {

    Response<Void> collectShop(ShopCollectRequest request);

    Response<Void> cancelCollect(ShopCollectRequest request);

    Response<ShopCollectStatusVO> getCollectStatus(Long shopId);

    Response<List<ShopCollectVO>> getMyCollectList();

    void syncCollectCountToDb();
}
