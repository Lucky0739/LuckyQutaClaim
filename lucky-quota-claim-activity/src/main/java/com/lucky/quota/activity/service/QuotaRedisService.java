package com.lucky.quota.activity.service;

import com.lucky.quota.activity.model.entity.QuotaActivity;
import com.lucky.quota.activity.model.entity.QuotaGoods;

import java.util.List;

public interface QuotaRedisService {

    /**
     * 预热活动库存到 Redis（同时缓存物资和活动信息）
     */
    void warmUpActivityStock(Long activityId, List<QuotaGoods> goodsList, QuotaActivity activity);

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
     * 活动已预热后，为审核新通过的限量物资补充 Redis 库存（不重复覆盖已有 key）
     */
    void supplementGoodsStock(Long activityId, QuotaGoods goods);

    /**
     * 递增预热重试次数并返回最新值（首次调用自动初始化，带 TTL）
     */
    int incrementWarmupRetryCount(Long activityId);

    /**
     * 清除预热重试计数（预热成功后调用）
     */
    void clearWarmupRetryCount(Long activityId);

    /**
     * 缓存限量物资信息到 Redis
     */
    void cacheQuotaGoods(QuotaGoods goods);

    /**
     * 从 Redis 获取缓存的限量物资
     */
    QuotaGoods getCachedQuotaGoods(Long seckillGoodsId);

    /**
     * 缓存限量场次信息到 Redis
     */
    void cacheQuotaActivity(QuotaActivity activity);

    /**
     * 从 Redis 获取缓存的限量场次
     */
    QuotaActivity getCachedQuotaActivity(Long activityId);
}
