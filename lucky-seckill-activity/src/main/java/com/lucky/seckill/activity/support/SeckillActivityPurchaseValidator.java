package com.lucky.seckill.activity.support;

import com.lucky.seckill.activity.enums.SeckillActivityStatusEnum;
import com.lucky.seckill.activity.model.entity.SeckillActivity;
import com.lucky.seckill.activity.service.SeckillRedisService;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 秒杀活动是否处于可购买时间窗口（活动已开始、未结束、进行中且已预热）。
 */
@Component
public class SeckillActivityPurchaseValidator {

    @Resource
    private SeckillRedisService seckillRedisService;

    public void assertCanSeckillPurchase(SeckillActivity activity) {
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
        LocalDateTime now = LocalDateTime.now();
        assertWithinTimeWindow(activity, now);
        if (!SeckillActivityStatusEnum.IN_PROGRESS.getCode().equals(activity.getStatus())) {
            if (SeckillActivityStatusEnum.ENDED.getCode().equals(activity.getStatus())) {
                throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_ENDED);
            }
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_STARTED);
        }
        if (!seckillRedisService.isWarmedUp(activity.getId())) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_STARTED, "活动库存预热中，请稍后再试");
        }
    }

    /**
     * 是否处于秒杀进行中时间窗（用于拦截普通下单）。
     */
    public boolean isInOngoingSeckillWindow(SeckillActivity activity) {
        if (activity == null) {
            return false;
        }
        if (!SeckillActivityStatusEnum.IN_PROGRESS.getCode().equals(activity.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getBeginTime() != null && now.isBefore(activity.getBeginTime())) {
            return false;
        }
        if (activity.getEndTime() != null && !now.isBefore(activity.getEndTime())) {
            return false;
        }
        return true;
    }

    private void assertWithinTimeWindow(SeckillActivity activity, LocalDateTime now) {
        if (activity.getBeginTime() != null && now.isBefore(activity.getBeginTime())) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_STARTED);
        }
        if (activity.getEndTime() != null && !now.isBefore(activity.getEndTime())) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_ENDED);
        }
    }
}
