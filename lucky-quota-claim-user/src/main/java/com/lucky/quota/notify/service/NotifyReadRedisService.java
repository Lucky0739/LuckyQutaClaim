package com.lucky.quota.notify.service;

import com.lucky.quota.common.constant.RedisKeyConstants;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户已读消息 Redis ZSet 存储
 */
@Service
public class NotifyReadRedisService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 记录用户已读消息（score 为已读时间戳）
     */
    public void addReadNotify(Long userId, Integer notifyType, Long notifyId) {
        if (userId == null || notifyType == null || notifyId == null) {
            return;
        }
        String key = RedisKeyConstants.notifyReadKey(userId, notifyType);
        stringRedisTemplate.opsForZSet().add(key, String.valueOf(notifyId), System.currentTimeMillis());
    }

    /**
     * 批量记录已读（按消息类型分组写入 ZSet）
     */
    public void batchAddReadNotify(Long userId, Map<Integer, List<Long>> notifyIdsByType) {
        if (userId == null || notifyIdsByType == null || notifyIdsByType.isEmpty()) {
            return;
        }
        double score = System.currentTimeMillis();
        for (Map.Entry<Integer, List<Long>> entry : notifyIdsByType.entrySet()) {
            Integer notifyType = entry.getKey();
            List<Long> notifyIds = entry.getValue();
            if (notifyType == null || notifyIds == null || notifyIds.isEmpty()) {
                continue;
            }
            String key = RedisKeyConstants.notifyReadKey(userId, notifyType);
            Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
            for (Long notifyId : notifyIds) {
                if (notifyId != null) {
                    tuples.add(new DefaultTypedTuple<>(String.valueOf(notifyId), score));
                }
            }
            if (!tuples.isEmpty()) {
                stringRedisTemplate.opsForZSet().add(key, tuples);
            }
        }
    }

    /**
     * 获取用户在指定消息类型下已读的消息 ID 集合
     */
    public Set<Long> getReadNotifyIds(Long userId, Integer notifyType) {
        if (userId == null || notifyType == null) {
            return Collections.emptySet();
        }
        String key = RedisKeyConstants.notifyReadKey(userId, notifyType);
        Set<String> members = stringRedisTemplate.opsForZSet().range(key, 0, -1);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        return members.stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    /**
     * 判断消息是否已在 Redis 中标记为已读
     */
    public boolean isRead(Long userId, Integer notifyType, Long notifyId) {
        if (userId == null || notifyType == null || notifyId == null) {
            return false;
        }
        String key = RedisKeyConstants.notifyReadKey(userId, notifyType);
        Double score = stringRedisTemplate.opsForZSet().score(key, String.valueOf(notifyId));
        return score != null;
    }
}
