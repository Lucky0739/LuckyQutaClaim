package com.lucky.seckill.activity.service;

import com.lucky.seckill.activity.model.entity.SeckillGoods;

import java.util.List;

public interface SeckillRedisService {

    /**
     * 预热活动库存到 Redis
     */
    void warmUpActivityStock(Long activityId, List<SeckillGoods> goodsList);

    /**
     * 是否已预热
     */
    boolean isWarmedUp(Long activityId);

    /**
     * Lua 原子扣减库存
     *
     * @return 1=成功, -1=获取锁失败, -2=超过限购, -3=库存不足
     */
    int deductStock(Long activityId, Long userId, Long goodsId, Long skuId, int buyNum, int limitNum);

    /**
     * 回滚 Redis 库存
     */
    void rollbackStock(Long activityId, Long userId, Long goodsId, Long skuId, int rollbackNum);

    /**
     * 获取 Redis 剩余库存
     */
    Integer getStock(Long activityId, Long goodsId, Long skuId);
}
