package com.lucky.seckill.activity.service.impl;

import com.lucky.seckill.activity.enums.SeckillActivityStatusEnum;
import com.lucky.seckill.activity.mapper.SeckillActivityMapper;
import com.lucky.seckill.activity.mapper.SeckillGoodsMapper;
import com.lucky.seckill.activity.model.entity.SeckillActivity;
import com.lucky.seckill.activity.model.entity.SeckillGoods;
import com.lucky.seckill.activity.model.vo.SeckillActivityVO;
import com.lucky.seckill.activity.model.vo.SeckillGoodsVO;
import com.lucky.seckill.activity.service.SeckillActivityService;
import com.lucky.seckill.activity.service.SeckillRedisService;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SeckillActivityServiceImpl implements SeckillActivityService {

    @Resource
    private SeckillActivityMapper seckillActivityMapper;

    @Resource
    private SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private SeckillRedisService seckillRedisService;

    @Override
    public Response<List<SeckillActivityVO>> listActivities() {
        List<SeckillActivityVO> list = seckillActivityMapper.selectAll().stream()
                .map(SeckillActivityVO::fromEntity)
                .collect(Collectors.toList());
        return Response.success(list);
    }

    @Override
    public Response<SeckillActivityVO> getActivityDetail(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectByPrimaryKey(activityId);
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
        return Response.success(SeckillActivityVO.fromEntity(activity));
    }

    @Override
    public Response<List<SeckillGoodsVO>> listActivityGoods(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectByPrimaryKey(activityId);
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }

        List<SeckillGoodsVO> list = seckillGoodsMapper.selectApprovedByActivityId(activityId).stream()
                .map(goods -> {
                    SeckillGoodsVO vo = SeckillGoodsVO.fromEntity(goods);
                    if (SeckillActivityStatusEnum.IN_PROGRESS.getCode().equals(activity.getStatus())
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
    public Response<SeckillActivityVO> startActivity(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectByPrimaryKey(activityId);
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
        if (SeckillActivityStatusEnum.ENDED.getCode().equals(activity.getStatus())) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_ENDED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getEndTime() != null && !now.isBefore(activity.getEndTime())) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_ENDED);
        }
        if (SeckillActivityStatusEnum.IN_PROGRESS.getCode().equals(activity.getStatus())
                && seckillRedisService.isWarmedUp(activityId)) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_ALREADY_STARTED);
        }

        warmUpAndStart(activity, now);
        SeckillActivity updated = seckillActivityMapper.selectByPrimaryKey(activityId);
        log.info("==> 手动开始秒杀活动, activityId={}, activityName={}", activityId, activity.getActivityName());
        return Response.success(SeckillActivityVO.fromEntity(updated));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> endActivity(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectByPrimaryKey(activityId);
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
        if (SeckillActivityStatusEnum.ENDED.getCode().equals(activity.getStatus())) {
            return Response.success(true);
        }

        seckillActivityMapper.updateStatus(
                activityId,
                SeckillActivityStatusEnum.ENDED.getCode(),
                LocalDateTime.now()
        );
        log.info("==> 手动结束秒杀活动, activityId={}, activityName={}", activityId, activity.getActivityName());
        return Response.success(true);
    }

    /**
     * 预热 Redis 库存并将活动置为进行中（供手动开始与定时任务复用）
     */
    public void warmUpAndStart(SeckillActivity activity, LocalDateTime now) {
        List<SeckillGoods> goodsList = seckillGoodsMapper.selectApprovedByActivityId(activity.getId());
        if (goodsList == null || goodsList.isEmpty()) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NO_GOODS);
        }

        seckillRedisService.warmUpActivityStock(activity.getId(), goodsList);
        seckillActivityMapper.updateStatus(
                activity.getId(),
                SeckillActivityStatusEnum.IN_PROGRESS.getCode(),
                now
        );
    }
}
