package com.lucky.seckill.activity.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.lucky.seckill.activity.enums.SeckillGoodsAuditStatusEnum;
import com.lucky.seckill.activity.support.SeckillActivityPurchaseValidator;
import com.lucky.seckill.activity.mapper.SeckillActivityMapper;
import com.lucky.seckill.activity.mapper.SeckillGoodsMapper;
import com.lucky.seckill.activity.model.dto.SeckillRequest;
import com.lucky.seckill.activity.model.entity.SeckillActivity;
import com.lucky.seckill.activity.model.entity.SeckillGoods;
import com.lucky.seckill.activity.model.vo.SeckillResultVO;
import com.lucky.seckill.activity.mq.SeckillOrderProducer;
import com.lucky.seckill.activity.service.SeckillRedisService;
import com.lucky.seckill.activity.service.SeckillService;
import com.lucky.seckill.common.constant.RedisKeyConstants;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.model.entity.SeckillOrderMessage;
import com.lucky.seckill.common.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeckillServiceImpl implements SeckillService {

    private static final int BUY_NUM = 1;
    private static final int MAX_RETRY = 3;

    @Resource
    private SeckillActivityMapper seckillActivityMapper;

    @Resource
    private SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private SeckillRedisService seckillRedisService;

    @Resource
    private SeckillOrderProducer seckillOrderProducer;

    @Resource
    private SeckillActivityPurchaseValidator seckillActivityPurchaseValidator;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Response<SeckillResultVO> doSeckill(SeckillRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long seckillGoodsId = request.getSeckillGoodsId();

        // ========== 1. Redis 读商品缓存，miss 时降级 MySQL ==========
        SeckillGoods seckillGoods = seckillRedisService.getCachedSeckillGoods(seckillGoodsId);
        if (seckillGoods == null) {
            seckillGoods = seckillGoodsMapper.selectByPrimaryKey(seckillGoodsId);
            if (seckillGoods == null) {
                throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);
            }
            // 回填缓存
            seckillRedisService.cacheSeckillGoods(seckillGoods);
        }

        // ========== 2. 内存校验审核状态 ==========
        if (!SeckillGoodsAuditStatusEnum.APPROVED.getCode().equals(seckillGoods.getAuditStatus())) {
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_APPROVED);
        }

        Long activityId = seckillGoods.getActivityId();
        Long goodsId = seckillGoods.getGoodsId();
        Long skuId = seckillGoods.getSkuId();

        // ========== 3. Redis 读活动缓存，miss 时降级 MySQL ==========
        SeckillActivity activity = seckillRedisService.getCachedSeckillActivity(activityId);
        if (activity == null) {
            activity = seckillActivityMapper.selectByPrimaryKey(activityId);
            if (activity == null) {
                throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
            }
            seckillRedisService.cacheSeckillActivity(activity);
        }

        // ========== 4. 内存校验活动时间/状态/预热 ==========
        seckillActivityPurchaseValidator.assertCanSeckillPurchase(activity);

        // ========== 5. Redis Lua 原子扣库存（内置限购检查，替代 MySQL 查重） ==========
        int limitNum = seckillGoods.getSeckillLimit() == null ? 1 : seckillGoods.getSeckillLimit();
        int deductResult = tryDeductStock(activityId, userId, goodsId, skuId, limitNum);
        if (deductResult == -1) {
            throw new BizException(ResponseCodeEnum.SECKILL_BUSY);
        }
        if (deductResult == -2) {
            throw new BizException(ResponseCodeEnum.SECKILL_LIMIT_EXCEEDED);
        }
        if (deductResult == -3) {
            throw new BizException(ResponseCodeEnum.STOCK_NOT_ENOUGH);
        }

        // ========== 6. 发送 MQ 异步创建订单（MQ 消费端做最终 MySQL 幂等兜底） ==========
        String orderNo = generateOrderNo(userId);
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(15);

        SeckillOrderMessage message = SeckillOrderMessage.builder()
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
            seckillOrderProducer.sendSeckillOrderMessage(message);
        } catch (Exception e) {
            log.error("==> 秒杀消息发送失败，回滚 Redis 库存, orderNo={}", orderNo, e);
            seckillRedisService.rollbackStock(activityId, userId, goodsId, skuId, BUY_NUM, orderNo);
            throw new BizException(ResponseCodeEnum.SECKILL_ORDER_CREATE_FAIL);
        }

        // MQ 发送成功，记录异步处理状态（仅 Redis，不落库）
        saveOrderStatus(userId, orderNo, RedisKeyConstants.ORDER_STATUS_PROCESSING);

        log.info("==> 秒杀请求成功，userId={}, seckillGoodsId={}, activityId={}, goodsId={}, skuId={}, orderNo={}",
                userId, seckillGoodsId, activityId, goodsId, skuId, orderNo);

        return Response.success(SeckillResultVO.builder()
                .orderNo(orderNo)
                .status(RedisKeyConstants.ORDER_STATUS_PROCESSING)
                .message("秒杀成功，订单创建中")
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
     * 在 Redis 中记录秒杀订单异步处理状态（仅 Redis，不落库）
     *
     * @param userId  用户ID
     * @param orderNo 订单号
     * @param status  -1=处理中，0=待支付，-2=创建失败
     */
    private void saveOrderStatus(Long userId, String orderNo, Integer status) {
        String redisKey = RedisKeyConstants.seckillOrderStatusKey(userId, orderNo);
        stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(status),
                RedisKeyConstants.SECKILL_ORDER_STATUS_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public Response<SeckillResultVO> getSeckillResult(String orderNo) {
        Long userId = StpUtil.getLoginIdAsLong();
        String redisKey = RedisKeyConstants.seckillOrderStatusKey(userId, orderNo);
        String statusStr = stringRedisTemplate.opsForValue().get(redisKey);

        // Redis key 不存在 → 超过30分钟或orderNo非法
        if (statusStr == null) {
            return Response.success(SeckillResultVO.builder()
                    .orderNo(orderNo)
                    .status(null)
                    .message("订单结果已过期，请查看订单列表确认")
                    .build());
        }

        int status = Integer.parseInt(statusStr);
        String message = switch (status) {
            case RedisKeyConstants.ORDER_STATUS_PROCESSING -> "订单创建中，请稍候...";
            case 0 -> "订单创建成功，请尽快支付";
            case RedisKeyConstants.ORDER_STATUS_FAILED -> "订单创建失败，秒杀商品已返还库存";
            default -> "未知状态";
        };

        return Response.success(SeckillResultVO.builder()
                .orderNo(orderNo)
                .status(status)
                .message(message)
                .build());
    }

    // ==================== SSE 实时推送秒杀结果 ====================

    private static final long SSE_TIMEOUT_MS = 60_000L;
    private static final long POLL_INTERVAL_MS = 500L;
    private static final ScheduledExecutorService SSE_EXECUTOR =
            Executors.newScheduledThreadPool(4, r -> {
                Thread t = new Thread(r, "sse-seckill-result");
                t.setDaemon(true);
                return t;
            });

    @Override
    public SseEmitter streamSeckillResult(String orderNo, Long userId) {
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

                        SeckillResultVO vo = SeckillResultVO.builder()
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
                                .data(SeckillResultVO.builder()
                                        .orderNo(orderNo)
                                        .status(null)
                                        .message("订单结果已过期，请查看订单列表确认")
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
                            .data(SeckillResultVO.builder()
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
                log.error("==> SSE推送秒杀结果异常, orderNo={}", orderNo, e);
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
            case RedisKeyConstants.ORDER_STATUS_PROCESSING -> "订单创建中，请稍候...";
            case 0 -> "订单创建成功，请尽快支付";
            case RedisKeyConstants.ORDER_STATUS_FAILED -> "订单创建失败，商品已返还库存";
            default -> "未知状态";
        };
    }
}
