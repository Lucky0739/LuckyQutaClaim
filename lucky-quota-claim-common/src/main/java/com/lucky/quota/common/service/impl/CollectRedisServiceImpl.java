package com.lucky.quota.common.service.impl;

import com.lucky.quota.common.constant.RedisKeyConstants;
import com.lucky.quota.common.enums.CollectTypeEnum;
import com.lucky.quota.common.service.CollectRedisService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class CollectRedisServiceImpl implements CollectRedisService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> collectAddScript;
    private DefaultRedisScript<Long> collectCancelScript;

    @PostConstruct
    public void init() {
        collectAddScript = new DefaultRedisScript<>();
        collectAddScript.setLocation(new ClassPathResource("lua/collect_add.lua"));
        collectAddScript.setResultType(Long.class);

        collectCancelScript = new DefaultRedisScript<>();
        collectCancelScript.setLocation(new ClassPathResource("lua/collect_cancel.lua"));
        collectCancelScript.setResultType(Long.class);
    }

    @Override
    public int addCollect(CollectTypeEnum type, Long userId, Long targetId, int initialCount) {
        initCountIfAbsent(type, targetId, initialCount);

        Long result = stringRedisTemplate.execute(
                collectAddScript,
                Arrays.asList(
                        recordKey(type, userId, targetId),
                        countKey(type, targetId),
                        syncSetKey(type)
                ),
                String.valueOf(targetId)
        );
        return result == null ? -1 : result.intValue();
    }

    @Override
    public int cancelCollect(CollectTypeEnum type, Long userId, Long targetId) {
        Long result = stringRedisTemplate.execute(
                collectCancelScript,
                Arrays.asList(
                        recordKey(type, userId, targetId),
                        countKey(type, targetId),
                        syncSetKey(type)
                ),
                String.valueOf(targetId)
        );
        return result == null ? -1 : result.intValue();
    }

    @Override
    public boolean isCollected(CollectTypeEnum type, Long userId, Long targetId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(recordKey(type, userId, targetId)));
    }

    @Override
    public Integer getCollectCount(CollectTypeEnum type, Long targetId) {
        String count = stringRedisTemplate.opsForValue().get(countKey(type, targetId));
        return count == null ? null : Integer.parseInt(count);
    }

    @Override
    public Set<String> popSyncIds(CollectTypeEnum type, int batchSize) {
        String syncSetKey = syncSetKey(type);
        Set<String> members = stringRedisTemplate.opsForSet().members(syncSetKey);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<>();
        int count = 0;
        for (String member : members) {
            if (count >= batchSize) {
                break;
            }
            Long removed = stringRedisTemplate.opsForSet().remove(syncSetKey, member);
            if (removed != null && removed > 0) {
                result.add(member);
                count++;
            }
        }
        return result;
    }

    private void initCountIfAbsent(CollectTypeEnum type, Long targetId, int initialCount) {
        String countKey = countKey(type, targetId);
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(countKey))) {
            stringRedisTemplate.opsForValue().set(countKey, String.valueOf(Math.max(initialCount, 0)));
        }
    }

    private String recordKey(CollectTypeEnum type, Long userId, Long targetId) {
        if (type == CollectTypeEnum.GOODS) {
            return RedisKeyConstants.goodsCollectRecordKey(userId, targetId);
        }
        return RedisKeyConstants.shopCollectRecordKey(userId, targetId);
    }

    private String countKey(CollectTypeEnum type, Long targetId) {
        if (type == CollectTypeEnum.GOODS) {
            return RedisKeyConstants.goodsCollectCountKey(targetId);
        }
        return RedisKeyConstants.shopCollectCountKey(targetId);
    }

    private String syncSetKey(CollectTypeEnum type) {
        if (type == CollectTypeEnum.GOODS) {
            return RedisKeyConstants.GOODS_COLLECT_SYNC_SET_KEY;
        }
        return RedisKeyConstants.SHOP_COLLECT_SYNC_SET_KEY;
    }
}
