package com.lucky.seckill.activity.service;

import com.lucky.seckill.activity.model.entity.SeckillActivity;
import com.lucky.seckill.activity.model.entity.SeckillGoods;

import java.util.List;

public interface SeckillRedisService {

    /**
     * 预热活动库存到 Redis（同时缓存商品和活动信息）
     */
    void warmUpActivityStock(Long activityId, List<SeckillGoods> goodsList, SeckillActivity activity);

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
     * 回滚 Redis 库存（带分布式锁 + 幂等标记，防止 MQ 重复回滚导致幽灵库存）
     */
    void rollbackStock(Long activityId, Long userId, Long goodsId, Long skuId, int rollbackNum, String orderNo);

    /**
     * 获取 Redis 剩余库存
     */
    Integer getStock(Long activityId, Long goodsId, Long skuId);

    /**
     * 活动已预热后，为审核新通过的秒杀商品补充 Redis 库存（不重复覆盖已有 key）
     */
    void supplementGoodsStock(Long activityId, SeckillGoods goods);

    /**
     * 递增预热重试次数并返回最新值（首次调用自动初始化，带 TTL）
     */
    int incrementWarmupRetryCount(Long activityId);

    /**
     * 清除预热重试计数（预热成功后调用）
     */
    void clearWarmupRetryCount(Long activityId);

    /**
     * 缓存秒杀商品信息到 Redis
     */
    void cacheSeckillGoods(SeckillGoods goods);

    /**
     * 从 Redis 获取缓存的秒杀商品
     */
    SeckillGoods getCachedSeckillGoods(Long seckillGoodsId);

    /**
     * 缓存秒杀活动信息到 Redis
     */
    void cacheSeckillActivity(SeckillActivity activity);

    /**
     * 从 Redis 获取缓存的秒杀活动
     */
    SeckillActivity getCachedSeckillActivity(Long activityId);
}
