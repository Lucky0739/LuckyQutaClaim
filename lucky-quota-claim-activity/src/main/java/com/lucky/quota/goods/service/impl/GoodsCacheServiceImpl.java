package com.lucky.quota.goods.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lucky.quota.common.constant.RedisKeyConstants;
import com.lucky.quota.common.model.cache.RedisData;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.goods.config.GoodsCacheProperties;
import com.lucky.quota.goods.model.dto.GoodsQueryRequest;
import com.lucky.quota.goods.model.entity.Goods;
import com.lucky.quota.goods.model.vo.GoodsDetailVO;
import com.lucky.quota.goods.model.vo.GoodsListVO;
import com.lucky.quota.goods.service.GoodsCacheService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@Slf4j
public class GoodsCacheServiceImpl implements GoodsCacheService {

    private static final TypeReference<RedisData<PageResult<GoodsListVO>>> PAGE_TYPE_REF =
            new TypeReference<RedisData<PageResult<GoodsListVO>>>() {};
    private static final TypeReference<RedisData<Goods>> GOODS_TYPE_REF =
            new TypeReference<RedisData<Goods>>() {};
    private static final TypeReference<RedisData<GoodsDetailVO>> DETAIL_TYPE_REF =
            new TypeReference<RedisData<GoodsDetailVO>>() {};

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GoodsCacheProperties goodsCacheProperties;

    @Resource(name = "goodsCacheExecutor")
    private Executor goodsCacheExecutor;

    private DefaultRedisScript<Long> releaseLockScript;

    @PostConstruct
    public void init() {
        releaseLockScript = new DefaultRedisScript<>();
        releaseLockScript.setLocation(new ClassPathResource("lua/release_lock.lua"));
        releaseLockScript.setResultType(Long.class);
    }

    @Override
    public PageResult<GoodsListVO> getGoodsPage(GoodsQueryRequest request,
                                                Supplier<PageResult<GoodsListVO>> dbLoader) {
        String queryHash = DigestUtils.md5Hex(JSON.toJSONString(request));
        String cacheKey = RedisKeyConstants.goodsPageKey(queryHash);
        String lockKey = RedisKeyConstants.goodsPageLockKey(queryHash);
        return getWithLogicalExpire(cacheKey, lockKey, PAGE_TYPE_REF, dbLoader);
    }

    @Override
    public Goods getGoodsById(Long goodsId, Supplier<Goods> dbLoader) {
        String cacheKey = RedisKeyConstants.goodsInfoKey(goodsId);
        String lockKey = RedisKeyConstants.goodsInfoLockKey(goodsId);
        return getWithLogicalExpire(cacheKey, lockKey, GOODS_TYPE_REF, dbLoader);
    }

    @Override
    public GoodsDetailVO getGoodsDetail(Long goodsId, Supplier<GoodsDetailVO> dbLoader) {
        String cacheKey = RedisKeyConstants.goodsDetailKey(goodsId);
        String lockKey = RedisKeyConstants.goodsDetailLockKey(goodsId);
        return getWithLogicalExpire(cacheKey, lockKey, DETAIL_TYPE_REF, dbLoader);
    }

    @Override
    public void clearPageCache() {
        deleteByPattern(RedisKeyConstants.GOODS_PAGE_KEY_PREFIX + "*");
    }

    @Override
    public void evictGoodsCache(Long goodsId) {
        if (goodsId == null) {
            return;
        }
        stringRedisTemplate.delete(RedisKeyConstants.goodsInfoKey(goodsId));
        stringRedisTemplate.delete(RedisKeyConstants.goodsDetailKey(goodsId));
        log.info("==> 清除物资单条缓存, goodsId: {}", goodsId);
    }

    @Override
    public void clearAllGoodsCache() {
        deleteByPattern(RedisKeyConstants.GOODS_PAGE_KEY_PREFIX + "*");
        deleteByPattern(RedisKeyConstants.GOODS_INFO_KEY_PREFIX + "*");
        deleteByPattern(RedisKeyConstants.GOODS_DETAIL_KEY_PREFIX + "*");
        log.info("==> 清除全部物资缓存");
    }

    private <T> T getWithLogicalExpire(String cacheKey,
                                       String lockKey,
                                       TypeReference<RedisData<T>> typeRef,
                                       Supplier<T> dbLoader) {
        String cacheJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cacheJson != null) {
            RedisData<T> redisData = JSON.parseObject(cacheJson, typeRef);
            if (redisData != null && redisData.getData() != null) {
                if (isNotExpired(redisData.getExpireTime())) {
                    log.debug("==> 缓存命中且未逻辑过期, key: {}", cacheKey);
                    return redisData.getData();
                }
                log.debug("==> 缓存逻辑过期，返回旧数据并异步重建, key: {}", cacheKey);
                rebuildCacheAsync(cacheKey, lockKey, dbLoader);
                return redisData.getData();
            }
        }
        return loadWithMutexLock(cacheKey, lockKey, typeRef, dbLoader, 0);
    }

    private <T> T loadWithMutexLock(String cacheKey,
                                    String lockKey,
                                    TypeReference<RedisData<T>> typeRef,
                                    Supplier<T> dbLoader,
                                    int retryCount) {
        String lockValue = IdUtil.fastSimpleUUID();
        boolean locked = tryLock(lockKey, lockValue);
        if (locked) {
            try {
                String cacheJson = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cacheJson != null) {
                    RedisData<T> redisData = JSON.parseObject(cacheJson, typeRef);
                    if (redisData != null && redisData.getData() != null) {
                        return redisData.getData();
                    }
                }
                T data = dbLoader.get();
                saveCache(cacheKey, data);
                log.debug("==> 互斥锁重建缓存成功, key: {}", cacheKey);
                return data;
            } finally {
                releaseLock(lockKey, lockValue);
            }
        }

        if (retryCount < goodsCacheProperties.getLockRetryTimes()) {
            sleepQuietly(goodsCacheProperties.getLockRetryIntervalMs());
            return loadWithMutexLock(cacheKey, lockKey, typeRef, dbLoader, retryCount + 1);
        }

        log.warn("==> 获取缓存互斥锁失败，降级查库, key: {}", cacheKey);
        return dbLoader.get();
    }

    private <T> void rebuildCacheAsync(String cacheKey, String lockKey, Supplier<T> dbLoader) {
        goodsCacheExecutor.execute(() -> {
            String lockValue = IdUtil.fastSimpleUUID();
            if (!tryLock(lockKey, lockValue)) {
                return;
            }
            try {
                T data = dbLoader.get();
                saveCache(cacheKey, data);
                log.debug("==> 异步重建缓存成功, key: {}", cacheKey);
            } catch (Exception e) {
                log.error("==> 异步重建缓存失败, key: {}", cacheKey, e);
            } finally {
                releaseLock(lockKey, lockValue);
            }
        });
    }

    private <T> void saveCache(String cacheKey, T data) {
        long expireAt = System.currentTimeMillis()
                + TimeUnit.MINUTES.toMillis(goodsCacheProperties.getLogicalExpireMinutes());
        RedisData<T> redisData = RedisData.<T>builder()
                .data(data)
                .expireTime(expireAt)
                .build();
        stringRedisTemplate.opsForValue().set(
                cacheKey,
                JSON.toJSONString(redisData),
                goodsCacheProperties.getPhysicalExpireMinutes(),
                TimeUnit.MINUTES
        );
    }

    private boolean isNotExpired(Long expireTime) {
        return expireTime != null && expireTime > System.currentTimeMillis();
    }

    private boolean tryLock(String lockKey, String lockValue) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey,
                lockValue,
                goodsCacheProperties.getLockExpireSeconds(),
                TimeUnit.SECONDS
        );
        return Boolean.TRUE.equals(success);
    }

    private void releaseLock(String lockKey, String lockValue) {
        stringRedisTemplate.execute(releaseLockScript, List.of(lockKey), lockValue);
    }

    private void deleteByPattern(String pattern) {
        List<String> keys = scanKeys(pattern);
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            log.info("==> 按模式清除缓存, pattern: {}, count: {}", pattern, keys.size());
        }
    }

    private List<String> scanKeys(String pattern) {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
