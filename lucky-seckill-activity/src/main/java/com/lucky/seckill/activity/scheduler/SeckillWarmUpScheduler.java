package com.lucky.seckill.activity.scheduler;

import com.lucky.seckill.activity.enums.SeckillActivityStatusEnum;
import com.lucky.seckill.activity.mapper.SeckillActivityMapper;
import com.lucky.seckill.activity.model.entity.SeckillActivity;
import com.lucky.seckill.activity.service.SeckillActivityService;
import com.lucky.seckill.activity.service.SeckillRedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀活动定时任务：到达开始时间自动预热，到达结束时间自动关闭。
 * <p>
 * 预热重试上限：最多重试 {@link #MAX_WARMUP_RETRY} 次（约 5 分钟），
 * 超过上限后记录告警日志并停止重试，需管理员通过后台手动介入。
 */
@Component
@Slf4j
public class SeckillWarmUpScheduler {

    /** 预热最大重试次数（每 30s 一次，10 次 ≈ 5 分钟） */
    private static final int MAX_WARMUP_RETRY = 10;

    @Resource
    private SeckillActivityMapper seckillActivityMapper;

    @Resource
    private SeckillActivityService seckillActivityService;

    @Resource
    private SeckillRedisService seckillRedisService;

    @Scheduled(fixedDelay = 30000)
    public void scanActivityStatus() {
        LocalDateTime now = LocalDateTime.now();
        warmUpStartedActivities(now);
        endExpiredActivities(now);
    }

    private void warmUpStartedActivities(LocalDateTime now) {
        List<SeckillActivity> activities = seckillActivityMapper.selectNeedWarmUp(now);
        for (SeckillActivity activity : activities) {
            try {
                int retryCount = seckillRedisService.incrementWarmupRetryCount(activity.getId());
                if (retryCount > MAX_WARMUP_RETRY) {
                    log.error("""
                                    ========================================
                                    ==  【预热告警】秒杀活动预热重试已达上限
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
                log.info("==> 定时任务：秒杀活动已开始并预热完成, activityId={}, activityName={}, retryCount={}",
                        activity.getId(), activity.getActivityName(), retryCount);
            } catch (Exception e) {
                log.error("==> 定时任务：秒杀活动预热失败, activityId={}, activityName={}, retryCount 将在下次扫描递增",
                        activity.getId(), activity.getActivityName(), e);
            }
        }
    }

    private void endExpiredActivities(LocalDateTime now) {
        List<SeckillActivity> activities = seckillActivityMapper.selectNeedEnd(now);
        for (SeckillActivity activity : activities) {
            seckillActivityMapper.updateStatus(
                    activity.getId(),
                    SeckillActivityStatusEnum.ENDED.getCode(),
                    now
            );
            log.info("==> 定时任务：秒杀活动已结束, activityId={}, activityName={}",
                    activity.getId(), activity.getActivityName());
        }
    }
}
