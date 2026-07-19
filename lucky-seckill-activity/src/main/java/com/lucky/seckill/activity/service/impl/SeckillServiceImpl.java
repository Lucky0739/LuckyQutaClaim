package com.lucky.seckill.activity.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.lucky.seckill.activity.enums.SeckillGoodsAuditStatusEnum;
import com.lucky.seckill.activity.support.SeckillActivityPurchaseValidator;
import com.lucky.seckill.activity.mapper.SeckillActivityMapper;
import com.lucky.seckill.activity.mapper.SeckillGoodsMapper;
import com.lucky.seckill.activity.mapper.SeckillOrderMapper;
import com.lucky.seckill.activity.model.dto.SeckillRequest;
import com.lucky.seckill.activity.model.entity.SeckillActivity;
import com.lucky.seckill.activity.model.entity.SeckillGoods;
import com.lucky.seckill.activity.model.entity.SeckillOrder;
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

import java.time.LocalDateTime;
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
    private SeckillOrderMapper seckillOrderMapper;

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

        SeckillGoods seckillGoods = seckillGoodsMapper.selectByPrimaryKey(seckillGoodsId);
        if (seckillGoods == null) {
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);
        }
        if (!SeckillGoodsAuditStatusEnum.APPROVED.getCode().equals(seckillGoods.getAuditStatus())) {
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_APPROVED);
        }

        Long activityId = seckillGoods.getActivityId();
        Long goodsId = seckillGoods.getGoodsId();
        Long skuId = seckillGoods.getSkuId();

        SeckillActivity activity = seckillActivityMapper.selectByPrimaryKey(activityId);
        seckillActivityPurchaseValidator.assertCanSeckillPurchase(activity);

        SeckillOrder existOrder = seckillOrderMapper.selectByUserActivityGoodsSku(
                userId, activityId, goodsId, skuId);
        if (existOrder != null) {
            throw new BizException(ResponseCodeEnum.SECKILL_REPEAT);
        }

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

    private int tryDeductStock(Long activityId, Long userId, Long goodsId, Long skuId, int limitNum) {
        for (int i = 0; i < MAX_RETRY; i++) {
            int result = seckillRedisService.deductStock(
                    activityId, userId, goodsId, skuId, BUY_NUM, limitNum);
            if (result != -1) {
                return result;
            }
        }
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
}
