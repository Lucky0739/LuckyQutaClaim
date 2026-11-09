package com.lucky.quota.activity.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.lucky.quota.activity.enums.QuotaActivityStatusEnum;
import com.lucky.quota.activity.enums.QuotaGoodsAuditStatusEnum;
import com.lucky.quota.activity.enums.QuotaOrderStatusEnum;
import com.lucky.quota.activity.support.QuotaActivityPurchaseValidator;
import com.lucky.quota.activity.mapper.QuotaActivityMapper;
import com.lucky.quota.activity.mapper.QuotaGoodsMapper;
import com.lucky.quota.activity.mapper.QuotaOrderMapper;
import com.lucky.quota.activity.model.entity.QuotaOrder;
import com.lucky.quota.activity.model.dto.QuotaRequest;
import com.lucky.quota.activity.model.entity.QuotaActivity;
import com.lucky.quota.activity.model.entity.QuotaGoods;
import com.lucky.quota.activity.model.vo.QuotaResultVO;
import com.lucky.quota.activity.mq.QuotaOrderProducer;
import com.lucky.quota.activity.service.QuotaRedisService;
import com.lucky.quota.activity.service.QuotaService;
import com.lucky.quota.activity.support.QuotaOrderMirrorService;
import com.lucky.quota.common.constant.RedisKeyConstants;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.lucky.quota.common.model.entity.QuotaOrderMessage;
import com.lucky.quota.common.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class QuotaServiceImpl implements QuotaService {

    private static final int BUY_NUM = 1;
    private static final int MAX_RETRY = 3;

    @Resource
    private QuotaActivityMapper seckillActivityMapper;

    @Resource
    private QuotaGoodsMapper seckillGoodsMapper;

    @Resource
    private QuotaOrderMapper quotaOrderMapper;

    @Resource
    private QuotaRedisService seckillRedisService;

    @Resource
    private QuotaOrderProducer seckillOrderProducer;

    @Resource
    private QuotaActivityPurchaseValidator seckillActivityPurchaseValidator;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private QuotaOrderMirrorService quotaOrderMirrorService;

    @Override
    public Response<QuotaResultVO> doQuota(QuotaRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long seckillGoodsId = request.getSeckillGoodsId();

        // ========== 1. Redis 读物资缓存，miss 时降级 MySQL ==========
        QuotaGoods seckillGoods = seckillRedisService.getCachedQuotaGoods(seckillGoodsId);
        if (seckillGoods == null) {
            seckillGoods = seckillGoodsMapper.selectByPrimaryKey(seckillGoodsId);
            if (seckillGoods == null) {
                throw new BizException(ResponseCodeEnum.QUOTA_GOODS_NOT_EXIST);
            }
            // 回填缓存
            seckillRedisService.cacheQuotaGoods(seckillGoods);
        }

        // ========== 2. 内存校验审核状态 ==========
        if (!QuotaGoodsAuditStatusEnum.APPROVED.getCode().equals(seckillGoods.getAuditStatus())) {
            throw new BizException(ResponseCodeEnum.QUOTA_GOODS_NOT_APPROVED);
        }

        Long activityId = seckillGoods.getActivityId();
        Long goodsId = seckillGoods.getGoodsId();
        Long skuId = seckillGoods.getSkuId();

        // ========== 3. 活动信息：Redis 优先，但与库内 status 不一致时以 MySQL 为准（避免预热时旧缓存） ==========
        QuotaActivity activity = loadActivityForQuota(activityId);
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_NOT_EXIST);
        }

        // ========== 4. 内存校验活动时间/状态/预热 ==========
        seckillActivityPurchaseValidator.assertCanQuotaPurchase(activity);

        QuotaOrder existingOrder = quotaOrderMapper.selectByUserActivityGoodsSku(
                userId, activityId, goodsId, skuId);
        if (existingOrder != null
                && QuotaOrderStatusEnum.PENDING_PAYMENT.getCode().equals(existingOrder.getStatus())) {
            throw new BizException(ResponseCodeEnum.QUOTA_LIMIT_EXCEEDED,
                    "本场次该规格已有待支付申领单，单号: " + existingOrder.getOrderNo());
        }

        // ========== 5. Redis Lua 原子扣库存（内置限购检查，替代 MySQL 查重） ==========
        int limitNum = seckillGoods.getSeckillLimit() == null ? 1 : seckillGoods.getSeckillLimit();
        int deductResult = tryDeductStock(activityId, userId, goodsId, skuId, limitNum);
        if (deductResult == -1) {
            throw new BizException(ResponseCodeEnum.QUOTA_BUSY);
        }
        if (deductResult == -2) {
            throw new BizException(ResponseCodeEnum.QUOTA_LIMIT_EXCEEDED);
        }
        if (deductResult == -3) {
            throw new BizException(ResponseCodeEnum.STOCK_NOT_ENOUGH);
        }

        // ========== 6. 发送 MQ 异步创建申领单（MQ 消费端做最终 MySQL 幂等兜底） ==========
        String orderNo = generateOrderNo(userId);
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(10);

        QuotaOrderMessage message = QuotaOrderMessage.builder()
                .userId(userId)
                .activityId(activityId)
                .goodsId(goodsId)
                .skuId(skuId)
                .seckillGoodsId(seckillGoods.getId())
                .orderNo(orderNo)
                .seckillPrice(seckillGoods.getSeckillPrice())
                .goodsName(seckillGoods.getSeckillTitle())
                .goodsImg(seckillGoods.getSeckillImg())
                .buyNum(BUY_NUM)
                .expireTime(expireTime)
                .build();

        try {
            seckillOrderProducer.sendQuotaOrderMessage(message);
        } catch (Exception e) {
            log.error("==> 申领消息发送失败，回滚 Redis 库存, orderNo={}", orderNo, e);
            seckillRedisService.rollbackStock(activityId, userId, goodsId, skuId, BUY_NUM, orderNo);
            throw new BizException(ResponseCodeEnum.QUOTA_ORDER_CREATE_FAIL);
        }

        // MQ 发送成功，记录异步处理状态（仅 Redis，不落库）
        saveOrderStatus(userId, orderNo, RedisKeyConstants.ORDER_STATUS_PROCESSING);

        log.info("==> 申领请求成功，userId={}, seckillGoodsId={}, activityId={}, goodsId={}, skuId={}, orderNo={}",
                userId, seckillGoodsId, activityId, goodsId, skuId, orderNo);

        return Response.success(QuotaResultVO.builder()
                .orderNo(orderNo)
                .status(RedisKeyConstants.ORDER_STATUS_PROCESSING)
                .message("申领成功，申领单创建中")
                .build());
    }

    /**
     * 带重试的 Lua 扣库存。
     * <p>
     * 两种重试场景：
     * <ul>
     *   <li><b>业务层竞争</b>（result == -1）：锁被其他请求占用，短暂重试</li>
     *   <li><b>基础设施异常</b>（Redis 超时/断开等）：Lua 脚本未能执行，同样重试</li>
     * </ul>
     * 3 次全部失败返回 -1，上游当作"系统繁忙"处理。
     */
    private int tryDeductStock(Long activityId, Long userId, Long goodsId, Long skuId, int limitNum) {
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                int result = seckillRedisService.deductStock(
                        activityId, userId, goodsId, skuId, BUY_NUM, limitNum);
                if (result != -1) {
                    return result; // 拿到明确结果（成功、限购、库存不足）
                }
                // -1：锁竞争，等待几毫秒后重试
            } catch (Exception e) {
                log.warn("==> Lua扣库存脚本异常，第{}次重试, activityId={}, goodsId={}, skuId={}",
                        i + 1, activityId, goodsId, skuId, e);
            }
        }
        log.error("==> Lua扣库存重试{}次全部失败, activityId={}, goodsId={}, skuId={}",
                MAX_RETRY, activityId, goodsId, skuId);
        return -1;
    }

    private String generateOrderNo(Long userId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String userIdStr = String.format("%04d", userId % 10000);
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return "SK" + timestamp + userIdStr + random;
    }

    /**
     * 在 Redis 中记录限量申领单异步处理状态（仅 Redis，不落库）
     *
     * @param userId  用户ID
     * @param orderNo 申领单号
     * @param status  -1=处理中，0=待确认领取，-2=创建失败
     */
    private void saveOrderStatus(Long userId, String orderNo, Integer status) {
        String redisKey = RedisKeyConstants.seckillOrderStatusKey(userId, orderNo);
        stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(status),
                RedisKeyConstants.QUOTA_ORDER_STATUS_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public Response<QuotaResultVO> getQuotaResult(String orderNo) {
        Long userId = StpUtil.getLoginIdAsLong();
        String redisKey = RedisKeyConstants.seckillOrderStatusKey(userId, orderNo);
        String statusStr = stringRedisTemplate.opsForValue().get(redisKey);

        // Redis key 不存在 → 超过30分钟或orderNo非法
        if (statusStr == null) {
            return Response.success(QuotaResultVO.builder()
                    .orderNo(orderNo)
                    .status(null)
                    .message("申领结果已过期，请到我的申领确认")
                    .build());
        }

        int status = Integer.parseInt(statusStr);
        if (status == RedisKeyConstants.ORDER_STATUS_PROCESSING
                || status == QuotaOrderStatusEnum.PENDING_PAYMENT.getCode()) {
            try {
                quotaOrderMirrorService.mirrorFromSeckillOrder(orderNo);
            } catch (Exception e) {
                log.warn("==> 轮询时补镜像 t_order 失败, orderNo={}", orderNo, e);
            }
        }
        String message = switch (status) {
            case RedisKeyConstants.ORDER_STATUS_PROCESSING -> "申领单创建中，请稍候...";
            case 0 -> "申领成功，请尽快确认领取";
            case RedisKeyConstants.ORDER_STATUS_FAILED -> "申领单创建失败，限量名额已返还";
            default -> "未知状态";
        };

        return Response.success(QuotaResultVO.builder()
                .orderNo(orderNo)
                .status(status)
                .message(message)
                .build());
    }

    // ==================== SSE 实时推送申领结果 ====================

    private static final long SSE_TIMEOUT_MS = 60_000L;
    private static final long POLL_INTERVAL_MS = 500L;
    private static final ScheduledExecutorService SSE_EXECUTOR =
            Executors.newScheduledThreadPool(4, r -> {
                Thread t = new Thread(r, "sse-seckill-result");
                t.setDaemon(true);
                return t;
            });

    @Override
    public SseEmitter streamQuotaResult(String orderNo, Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        String redisKey = RedisKeyConstants.seckillOrderStatusKey(userId, orderNo);

        // 定时检查 Redis 状态，变更时推送
        SSE_EXECUTOR.execute(() -> {
            try {
                String lastStatus = null;
                long deadline = System.currentTimeMillis() + SSE_TIMEOUT_MS;

                while (System.currentTimeMillis() < deadline) {
                    String statusStr = stringRedisTemplate.opsForValue().get(redisKey);

                    // 状态变更或首次检查
                    if (statusStr != null && !statusStr.equals(lastStatus)) {
                        lastStatus = statusStr;
                        int status = Integer.parseInt(statusStr);
                        String message = resolveStatusMessage(status);

                        QuotaResultVO vo = QuotaResultVO.builder()
                                .orderNo(orderNo)
                                .status(status)
                                .message(message)
                                .build();
                        emitter.send(SseEmitter.event()
                                .name("result")
                                .data(vo));

                        // 终态（成功或失败）→ 关闭连接
                        if (status != RedisKeyConstants.ORDER_STATUS_PROCESSING) {
                            emitter.complete();
                            return;
                        }
                    }

                    // key 不存在 → 可能 TTL 过期
                    if (statusStr == null && lastStatus != null) {
                        emitter.send(SseEmitter.event()
                                .name("result")
                                .data(QuotaResultVO.builder()
                                        .orderNo(orderNo)
                                        .status(null)
                                        .message("申领结果已过期，请到我的申领确认")
                                        .build()));
                        emitter.complete();
                        return;
                    }

                    Thread.sleep(POLL_INTERVAL_MS);
                }

                // 超时：推送当前状态
                String finalStatus = stringRedisTemplate.opsForValue().get(redisKey);
                if (finalStatus != null) {
                    int status = Integer.parseInt(finalStatus);
                    emitter.send(SseEmitter.event()
                            .name("result")
                            .data(QuotaResultVO.builder()
                                    .orderNo(orderNo)
                                    .status(status)
                                    .message(resolveStatusMessage(status))
                                    .build()));
                }
                emitter.complete();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("==> SSE推送申领结果异常, orderNo={}", orderNo, e);
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onError(ex -> {
            log.warn("==> SSE连接异常, orderNo={}", orderNo, ex);
        });

        return emitter;
    }

    private String resolveStatusMessage(int status) {
        return switch (status) {
            case RedisKeyConstants.ORDER_STATUS_PROCESSING -> "申领单创建中，请稍候...";
            case 0 -> "申领成功，请尽快确认领取";
            case RedisKeyConstants.ORDER_STATUS_FAILED -> "申领单创建失败，限量名额已返还";
            default -> "未知状态";
        };
    }

    private QuotaActivity loadActivityForQuota(Long activityId) {
        QuotaActivity cached = seckillRedisService.getCachedQuotaActivity(activityId);
        QuotaActivity fromDb = seckillActivityMapper.selectByPrimaryKey(activityId);
        if (fromDb == null) {
            return null;
        }
        if (cached == null) {
            seckillRedisService.cacheQuotaActivity(fromDb);
            return fromDb;
        }
        Integer cachedStatus = cached.getStatus();
        Integer dbStatus = fromDb.getStatus();
        boolean statusMismatch = !Objects.equals(cachedStatus, dbStatus);
        boolean staleNotStarted = QuotaActivityStatusEnum.NOT_STARTED.getCode().equals(cachedStatus)
                && QuotaActivityStatusEnum.IN_PROGRESS.getCode().equals(dbStatus);
        if (statusMismatch || staleNotStarted) {
            seckillRedisService.cacheQuotaActivity(fromDb);
            return fromDb;
        }
        return cached;
    }
}
