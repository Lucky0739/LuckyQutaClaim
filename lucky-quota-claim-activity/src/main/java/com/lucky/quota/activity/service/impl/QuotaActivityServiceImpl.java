package com.lucky.quota.activity.service.impl;

import com.lucky.quota.activity.enums.QuotaActivityStatusEnum;
import com.lucky.quota.activity.mapper.QuotaActivityMapper;
import com.lucky.quota.activity.mapper.QuotaGoodsMapper;
import com.lucky.quota.activity.model.entity.QuotaActivity;
import com.lucky.quota.activity.model.entity.QuotaGoods;
import com.lucky.quota.activity.model.vo.QuotaActivityVO;
import com.lucky.quota.activity.model.vo.QuotaGoodsVO;
import com.lucky.quota.activity.support.QuotaActivityPurchaseValidator;
import com.lucky.quota.activity.service.QuotaActivityService;
import com.lucky.quota.activity.service.QuotaRedisService;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.lucky.quota.common.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuotaActivityServiceImpl implements QuotaActivityService {

    @Resource
    private QuotaActivityMapper seckillActivityMapper;

    @Resource
    private QuotaGoodsMapper seckillGoodsMapper;

    @Resource
    private QuotaRedisService seckillRedisService;

    @Resource
    private QuotaActivityPurchaseValidator seckillActivityPurchaseValidator;

    @Override
    public Response<List<QuotaActivityVO>> listActivities() {
        List<QuotaActivityVO> list = seckillActivityMapper.selectAll().stream()
                .map(QuotaActivityVO::fromEntity)
                .collect(Collectors.toList());
        return Response.success(list);
    }

    @Override
    public Response<QuotaActivityVO> getActivityDetail(Long activityId) {
        QuotaActivity activity = seckillActivityMapper.selectByPrimaryKey(activityId);
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_NOT_EXIST);
        }
        return Response.success(QuotaActivityVO.fromEntity(activity));
    }

    @Override
    public Response<List<QuotaGoodsVO>> listActivityGoods(Long activityId) {
        QuotaActivity activity = seckillActivityMapper.selectByPrimaryKey(activityId);
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_NOT_EXIST);
        }

        List<QuotaGoodsVO> list = seckillGoodsMapper.selectApprovedByActivityId(activityId).stream()
                .map(goods -> {
                    QuotaGoodsVO vo = QuotaGoodsVO.fromEntity(goods);
                    if (QuotaActivityStatusEnum.IN_PROGRESS.getCode().equals(activity.getStatus())
                            && seckillRedisService.isWarmedUp(activityId)) {
                        Integer redisStock = seckillRedisService.getStock(
                                activityId, goods.getGoodsId(), goods.getSkuId());
                        if (redisStock != null) {
                            vo.setSeckillStock(redisStock);
                        }
                    }
                    return vo;
                })
                .collect(Collectors.toList());
        return Response.success(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<QuotaActivityVO> startActivity(Long activityId) {
        QuotaActivity activity = seckillActivityMapper.selectByPrimaryKey(activityId);
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_NOT_EXIST);
        }
        if (QuotaActivityStatusEnum.ENDED.getCode().equals(activity.getStatus())) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_ENDED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getBeginTime() != null && now.isBefore(activity.getBeginTime())) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_NOT_STARTED, "未到活动开始时间，无法提前开启");
        }
        if (activity.getEndTime() != null && !now.isBefore(activity.getEndTime())) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_ENDED);
        }
        if (QuotaActivityStatusEnum.IN_PROGRESS.getCode().equals(activity.getStatus())
                && seckillRedisService.isWarmedUp(activityId)) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_ALREADY_STARTED);
        }

        warmUpAndStart(activity, now);
        QuotaActivity updated = seckillActivityMapper.selectByPrimaryKey(activityId);
        log.info("==> 手动开始限量场次, activityId={}, activityName={}", activityId, activity.getActivityName());
        return Response.success(QuotaActivityVO.fromEntity(updated));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> endActivity(Long activityId) {
        QuotaActivity activity = seckillActivityMapper.selectByPrimaryKey(activityId);
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_NOT_EXIST);
        }
        if (QuotaActivityStatusEnum.ENDED.getCode().equals(activity.getStatus())) {
            return Response.success(true);
        }

        seckillActivityMapper.updateStatus(
                activityId,
                QuotaActivityStatusEnum.ENDED.getCode(),
                LocalDateTime.now()
        );
        log.info("==> 手动结束限量场次, activityId={}, activityName={}", activityId, activity.getActivityName());
        return Response.success(true);
    }

    @Override
    public Response<Boolean> isSkuInOngoingQuota(Long goodsId, Long skuId) {
        if (goodsId == null || skuId == null) {
            return Response.success(false);
        }
        QuotaGoods seckillGoods = seckillGoodsMapper.selectOngoingApprovedByGoodsIdAndSkuId(
                goodsId, skuId, LocalDateTime.now());
        if (seckillGoods == null) {
            return Response.success(false);
        }
        QuotaActivity activity = seckillActivityMapper.selectByPrimaryKey(seckillGoods.getActivityId());
        boolean ongoing = seckillActivityPurchaseValidator.isInOngoingQuotaWindow(activity);
        return Response.success(ongoing);
    }

    /**
     * 预热 Redis 库存并将活动置为进行中（供手动开始与定时任务复用）
     */
    public void warmUpAndStart(QuotaActivity activity, LocalDateTime now) {
        List<QuotaGoods> goodsList = seckillGoodsMapper.selectApprovedByActivityId(activity.getId());
        if (goodsList == null || goodsList.isEmpty()) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_NO_GOODS);
        }

        seckillRedisService.warmUpActivityStock(activity.getId(), goodsList, activity);
        seckillActivityMapper.updateStatus(
                activity.getId(),
                QuotaActivityStatusEnum.IN_PROGRESS.getCode(),
                now
        );
        // 预热成功，清除重试计数
        seckillRedisService.clearWarmupRetryCount(activity.getId());
    }
}
