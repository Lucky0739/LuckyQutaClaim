package com.lucky.seckill.activity.service.impl;

import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.activity.model.entity.SeckillGoods;
import com.lucky.seckill.activity.service.SeckillRedisService;
import com.lucky.seckill.common.constant.RedisKeyConstants;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeckillRedisServiceImpl implements SeckillRedisService {

    private static final int LOCK_EXPIRE_SECONDS = 5;
    private static final int ROLLBACK_FLAG_EXPIRE_SECONDS = 300; // 幂等标记 5 分钟，覆盖 MQ 重试窗口
    private static final String WARMUP_FLAG = "1";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> deductStockScript;
    private DefaultRedisScript<Long> rollbackStockScript;

    @PostConstruct
    public void init() {
        deductStockScript = new DefaultRedisScript<>();
        deductStockScript.setLocation(new ClassPathResource("lua/seckill_deduct_stock.lua"));
        deductStockScript.setResultType(Long.class);

        rollbackStockScript = new DefaultRedisScript<>();
        rollbackStockScript.setLocation(new ClassPathResource("lua/seckill_rollback_stock.lua"));
        rollbackStockScript.setResultType(Long.class);
    }

    @Override
    public void warmUpActivityStock(Long activityId, List<SeckillGoods> goodsList) {
        if (goodsList == null || goodsList.isEmpty()) {
            log.warn("==> 活动 {} 无秒杀商品，跳过预热", activityId);
            return;
        }

        String warmupLockKey = RedisKeyConstants.seckillWarmupLockKey(activityId);
        String lockValue = IdUtil.fastSimpleUUID();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(warmupLockKey, lockValue, 30, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            log.info("==> 活动 {} 预热锁已被占用，跳过", activityId);
            return;
        }

        try {
            if (isWarmedUp(activityId)) {
                log.info("==> 活动 {} 已预热，跳过", activityId);
                return;
            }

            for (SeckillGoods goods : goodsList) {
                String stockKey = RedisKeyConstants.seckillStockKey(
                        activityId, goods.getGoodsId(), goods.getSkuId());
                stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(goods.getSeckillStock()));
                log.info("==> 预热库存 activityId={}, goodsId={}, skuId={}, stock={}",
                        activityId, goods.getGoodsId(), goods.getSkuId(), goods.getSeckillStock());
            }

            stringRedisTemplate.opsForValue().set(RedisKeyConstants.seckillWarmupKey(activityId), WARMUP_FLAG);
            log.info("==> 活动 {} 库存预热完成，商品数={}", activityId, goodsList.size());
        } finally {
            String currentLock = stringRedisTemplate.opsForValue().get(warmupLockKey);
            if (lockValue.equals(currentLock)) {
                stringRedisTemplate.delete(warmupLockKey);
            }
        }
    }

    @Override
    public boolean isWarmedUp(Long activityId) {
        return WARMUP_FLAG.equals(stringRedisTemplate.opsForValue()
                .get(RedisKeyConstants.seckillWarmupKey(activityId)));
    }

    @Override
    public int deductStock(Long activityId, Long userId, Long goodsId, Long skuId,
                           int buyNum, int limitNum) {
        String stockKey = RedisKeyConstants.seckillStockKey(activityId, goodsId, skuId);
        String boughtKey = RedisKeyConstants.seckillBoughtKey(activityId, userId, goodsId, skuId);
        String lockKey = RedisKeyConstants.seckillLockKey(activityId, goodsId, skuId);
        String lockValue = IdUtil.fastSimpleUUID();

        Long result = stringRedisTemplate.execute(
                deductStockScript,
                Arrays.asList(stockKey, boughtKey, lockKey),
                String.valueOf(buyNum),
                String.valueOf(limitNum),
                String.valueOf(LOCK_EXPIRE_SECONDS),
                lockValue
        );

        return result == null ? -1 : result.intValue();
    }

    @Override
    public void rollbackStock(Long activityId, Long userId, Long goodsId, Long skuId, int rollbackNum, String orderNo) {
        String stockKey = RedisKeyConstants.seckillStockKey(activityId, goodsId, skuId);
        String boughtKey = RedisKeyConstants.seckillBoughtKey(activityId, userId, goodsId, skuId);
        String lockKey = RedisKeyConstants.seckillLockKey(activityId, goodsId, skuId);
        String rollbackFlagKey = RedisKeyConstants.seckillRollbackFlagKey(orderNo);
        String lockValue = IdUtil.fastSimpleUUID();

        Long result = stringRedisTemplate.execute(
                rollbackStockScript,
                Arrays.asList(stockKey, boughtKey, lockKey, rollbackFlagKey),
                String.valueOf(rollbackNum),
                lockValue,
                String.valueOf(LOCK_EXPIRE_SECONDS),
                String.valueOf(ROLLBACK_FLAG_EXPIRE_SECONDS)
        );

        if (result != null && result == 0) {
            log.warn("==> 回滚已执行过，幂等跳过, orderNo={}", orderNo);
        } else if (result != null && result == -1) {
            log.warn("==> 回滚获取锁失败，等待 MQ 重试, orderNo={}", orderNo);
        }
    }

    @Override
    public Integer getStock(Long activityId, Long goodsId, Long skuId) {
        String stock = stringRedisTemplate.opsForValue()
                .get(RedisKeyConstants.seckillStockKey(activityId, goodsId, skuId));
        return stock == null ? null : Integer.parseInt(stock);
    }

    @Override
    public void supplementGoodsStock(Long activityId, SeckillGoods goods) {
        if (activityId == null || goods == null || !isWarmedUp(activityId)) {
            return;
        }
        String stockKey = RedisKeyConstants.seckillStockKey(activityId, goods.getGoodsId(), goods.getSkuId());
        Boolean absent = stringRedisTemplate.opsForValue().setIfAbsent(
                stockKey, String.valueOf(goods.getSeckillStock()));
        if (Boolean.TRUE.equals(absent)) {
            log.info("==> 补充预热库存 activityId={}, goodsId={}, skuId={}, stock={}",
                    activityId, goods.getGoodsId(), goods.getSkuId(), goods.getSeckillStock());
        }
    }
}
