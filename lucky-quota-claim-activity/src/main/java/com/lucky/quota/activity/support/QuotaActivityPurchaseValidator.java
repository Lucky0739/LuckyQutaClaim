package com.lucky.quota.activity.support;

import com.lucky.quota.activity.enums.QuotaActivityStatusEnum;
import com.lucky.quota.activity.model.entity.QuotaActivity;
import com.lucky.quota.activity.service.QuotaRedisService;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 限量场次是否处于可申领时间窗口（场次已开始、未结束、进行中且已预热）。
 */
@Component
public class QuotaActivityPurchaseValidator {

    @Resource
    private QuotaRedisService seckillRedisService;

    public void assertCanQuotaPurchase(QuotaActivity activity) {
        if (activity == null) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_NOT_EXIST);
        }
        LocalDateTime now = LocalDateTime.now();
        assertWithinTimeWindow(activity, now);
        if (!QuotaActivityStatusEnum.IN_PROGRESS.getCode().equals(activity.getStatus())) {
            if (QuotaActivityStatusEnum.ENDED.getCode().equals(activity.getStatus())) {
                throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_ENDED);
            }
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_NOT_STARTED);
        }
        if (!seckillRedisService.isWarmedUp(activity.getId())) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_NOT_STARTED, "活动库存预热中，请稍后再试");
        }
    }

    /**
     * 是否处于限量申领进行中时间窗（用于拦截普通申领）。
     */
    public boolean isInOngoingQuotaWindow(QuotaActivity activity) {
        if (activity == null) {
            return false;
        }
        if (!QuotaActivityStatusEnum.IN_PROGRESS.getCode().equals(activity.getStatus())) {
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

    private void assertWithinTimeWindow(QuotaActivity activity, LocalDateTime now) {
        if (activity.getBeginTime() != null && now.isBefore(activity.getBeginTime())) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_NOT_STARTED);
        }
        if (activity.getEndTime() != null && !now.isBefore(activity.getEndTime())) {
            throw new BizException(ResponseCodeEnum.QUOTA_ACTIVITY_ENDED);
        }
    }
}
