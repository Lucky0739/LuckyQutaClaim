package com.lucky.quota.activity.scheduler;

import com.lucky.quota.activity.enums.QuotaActivityStatusEnum;
import com.lucky.quota.activity.mapper.QuotaActivityMapper;
import com.lucky.quota.activity.model.entity.QuotaActivity;
import com.lucky.quota.activity.service.QuotaActivityService;
import com.lucky.quota.activity.service.QuotaRedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 限量场次定时任务：到达开始时间自动预热，到达结束时间自动关闭。
 * <p>
 * 预热重试上限：最多重试 {@link #MAX_WARMUP_RETRY} 次（约 5 分钟），
 * 超过上限后记录告警日志并停止重试，需管理员通过后台手动介入。
 */
@Component
@Slf4j
public class QuotaWarmUpScheduler {

    /** 预热最大重试次数（每 30s 一次，10 次 ≈ 5 分钟） */
    private static final int MAX_WARMUP_RETRY = 10;

    @Resource
    private QuotaActivityMapper seckillActivityMapper;

    @Resource
    private QuotaActivityService seckillActivityService;

    @Resource
    private QuotaRedisService seckillRedisService;

    @Scheduled(fixedDelay = 30000)
    public void scanActivityStatus() {
        LocalDateTime now = LocalDateTime.now();
        warmUpStartedActivities(now);
        recoverLostWarmup(now);
        endExpiredActivities(now);
    }

    /**
     * 场次已是进行中，但 Redis 预热标记或库存 key 丢失时重新预热。
     * 只扫 status=0 的话，Redis 重启后进行中的场次会一直报「库存预热中」，数据库库存不会再扣。
     */
    private void recoverLostWarmup(LocalDateTime now) {
        List<QuotaActivity> activities = seckillActivityMapper.selectInProgress(now);
        for (QuotaActivity activity : activities) {
            if (seckillRedisService.isWarmedUp(activity.getId())) {
                continue;
            }
            try {
                seckillActivityService.startActivity(activity.getId());
                log.info("==> 定时任务：进行中场次 Redis 预热丢失，已重新预热, activityId={}, activityName={}",
                        activity.getId(), activity.getActivityName());
            } catch (Exception e) {
                log.error("==> 定时任务：进行中场次补预热失败, activityId={}, activityName={}",
                        activity.getId(), activity.getActivityName(), e);
            }
        }
    }

    private void warmUpStartedActivities(LocalDateTime now) {
        List<QuotaActivity> activities = seckillActivityMapper.selectNeedWarmUp(now);
        for (QuotaActivity activity : activities) {
            try {
                int retryCount = seckillRedisService.incrementWarmupRetryCount(activity.getId());
                if (retryCount > MAX_WARMUP_RETRY) {
                    log.error("""
                                    ========================================
                                    ==  【预热告警】限量场次预热重试已达上限
                                    ==  activityId:    {}
                                    ==  activityName:  {}
                                    ==  beginTime:     {}
                                    ==  endTime:       {}
                                    ==  retryCount:    {}/{}
                                    ==  已停止自动重试，请管理员手动介入！
                                    ========================================""",
                            activity.getId(),
                            activity.getActivityName(),
                            activity.getBeginTime(),
                            activity.getEndTime(),
                            retryCount - 1, MAX_WARMUP_RETRY);
                    continue; // 跳过，不再重试
                }

                seckillActivityService.startActivity(activity.getId());
                seckillRedisService.clearWarmupRetryCount(activity.getId());
                log.info("==> 定时任务：限量场次已开始并预热完成, activityId={}, activityName={}, retryCount={}",
                        activity.getId(), activity.getActivityName(), retryCount);
            } catch (Exception e) {
                log.error("==> 定时任务：限量场次预热失败, activityId={}, activityName={}, retryCount 将在下次扫描递增",
                        activity.getId(), activity.getActivityName(), e);
            }
        }
    }

    private void endExpiredActivities(LocalDateTime now) {
        List<QuotaActivity> activities = seckillActivityMapper.selectNeedEnd(now);
        for (QuotaActivity activity : activities) {
            seckillActivityMapper.updateStatus(
                    activity.getId(),
                    QuotaActivityStatusEnum.ENDED.getCode(),
                    now
            );
            log.info("==> 定时任务：限量场次已结束, activityId={}, activityName={}",
                    activity.getId(), activity.getActivityName());
        }
    }
}
