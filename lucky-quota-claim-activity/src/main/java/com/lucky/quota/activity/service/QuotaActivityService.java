package com.lucky.quota.activity.service;

import com.lucky.quota.activity.model.vo.QuotaActivityVO;
import com.lucky.quota.activity.model.vo.QuotaGoodsVO;
import com.lucky.quota.common.utils.Response;

import java.util.List;

public interface QuotaActivityService {

    Response<List<QuotaActivityVO>> listActivities();

    Response<QuotaActivityVO> getActivityDetail(Long activityId);

    Response<List<QuotaGoodsVO>> listActivityGoods(Long activityId);

    /**
     * 手动开始限量场次：预热 Redis 库存并更新活动状态为进行中
     */
    Response<QuotaActivityVO> startActivity(Long activityId);

    /**
     * 手动结束限量场次
     */
    Response<Boolean> endActivity(Long activityId);

    /**
     * 指定物资规格是否处于进行中的限量场次（用于申领单拦截）
     */
    Response<Boolean> isSkuInOngoingQuota(Long goodsId, Long skuId);
}
