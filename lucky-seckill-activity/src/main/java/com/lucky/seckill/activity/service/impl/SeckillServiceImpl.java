package com.lucky.seckill.activity.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.activity.enums.SeckillActivityStatusEnum;
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
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.model.entity.SeckillOrderMessage;
import com.lucky.seckill.common.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    @Override
    public Response<SeckillResultVO> doSeckill(SeckillRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long activityId = request.getActivityId();
        Long goodsId = request.getGoodsId();
        Long skuId = request.getSkuId();

        SeckillActivity activity = seckillActivityMapper.selectByPrimaryKey(activityId);
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
        if (SeckillActivityStatusEnum.NOT_STARTED.getCode().equals(activity.getStatus())) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_STARTED);
        }
        if (SeckillActivityStatusEnum.ENDED.getCode().equals(activity.getStatus())) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_ENDED);
        }
        if (!seckillRedisService.isWarmedUp(activityId)) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_STARTED, "活动库存预热中，请稍后再试");
        }

        SeckillGoods seckillGoods = seckillGoodsMapper.selectByActivityAndGoodsAndSku(activityId, goodsId, skuId);
        if (seckillGoods == null) {
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);
        }

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
            seckillRedisService.rollbackStock(activityId, userId, goodsId, skuId, BUY_NUM);
            throw new BizException(ResponseCodeEnum.SECKILL_ORDER_CREATE_FAIL);
        }

        log.info("==> 秒杀请求成功，userId={}, activityId={}, goodsId={}, skuId={}, orderNo={}",
                userId, activityId, goodsId, skuId, orderNo);

        return Response.success(SeckillResultVO.builder()
                .orderNo(orderNo)
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
}
