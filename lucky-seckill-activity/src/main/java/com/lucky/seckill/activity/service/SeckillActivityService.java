package com.lucky.seckill.activity.service;

import com.lucky.seckill.activity.model.vo.SeckillActivityVO;
import com.lucky.seckill.activity.model.vo.SeckillGoodsVO;
import com.lucky.seckill.common.utils.Response;

import java.util.List;

public interface SeckillActivityService {

    Response<List<SeckillActivityVO>> listActivities();

    Response<SeckillActivityVO> getActivityDetail(Long activityId);

    Response<List<SeckillGoodsVO>> listActivityGoods(Long activityId);

    /**
     * 手动开始秒杀活动：预热 Redis 库存并更新活动状态为进行中
     */
    Response<SeckillActivityVO> startActivity(Long activityId);

    /**
     * 手动结束秒杀活动
     */
    Response<Boolean> endActivity(Long activityId);

    /**
     * 指定商品规格是否处于进行中的秒杀活动（用于普通订单拦截）
     */
    Response<Boolean> isSkuInOngoingSeckill(Long goodsId, Long skuId);
}
