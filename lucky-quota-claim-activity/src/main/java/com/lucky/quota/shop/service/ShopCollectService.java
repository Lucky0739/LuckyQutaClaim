package com.lucky.quota.shop.service;

import com.lucky.quota.common.utils.Response;
import com.lucky.quota.shop.model.dto.ShopCollectRequest;
import com.lucky.quota.shop.model.vo.ShopCollectStatusVO;
import com.lucky.quota.shop.model.vo.ShopCollectVO;

import java.util.List;

public interface ShopCollectService {

    Response<Void> collectShop(ShopCollectRequest request);

    Response<Void> cancelCollect(ShopCollectRequest request);

    Response<ShopCollectStatusVO> getCollectStatus(Long shopId);

    Response<List<ShopCollectVO>> getMyCollectList();

    void syncCollectCountToDb();
}
