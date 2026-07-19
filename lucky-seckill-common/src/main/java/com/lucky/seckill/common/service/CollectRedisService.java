package com.lucky.seckill.common.service;

import com.lucky.seckill.common.enums.CollectTypeEnum;

import java.util.Set;

/**
 * 收藏 Redis 服务（Lua 保证原子性）
 */
public interface CollectRedisService {

    /**
     * 收藏
     *
     * @return 1=成功, -1=已收藏
     */
    int addCollect(CollectTypeEnum type, Long userId, Long targetId, int initialCount);

    /**
     * 取消收藏
     *
     * @return 1=成功, -1=未收藏
     */
    int cancelCollect(CollectTypeEnum type, Long userId, Long targetId);

    /**
     * 是否已收藏
     */
    boolean isCollected(CollectTypeEnum type, Long userId, Long targetId);

    /**
     * 获取收藏数（Redis 中不存在时返回 null）
     */
    Integer getCollectCount(CollectTypeEnum type, Long targetId);

    /**
     * 从待同步 Set 中取出指定数量的目标 ID
     */
    Set<String> popSyncIds(CollectTypeEnum type, int batchSize);
}
