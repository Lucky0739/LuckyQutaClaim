package com.lucky.seckill.activity.scheduler;

import com.lucky.seckill.activity.enums.SeckillActivityStatusEnum;
import com.lucky.seckill.activity.mapper.SeckillActivityMapper;
import com.lucky.seckill.activity.model.entity.SeckillActivity;
import com.lucky.seckill.activity.service.SeckillActivityService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀活动定时任务：到达开始时间自动预热，到达结束时间自动关闭
 */
@Component
@Slf4j
public class SeckillWarmUpScheduler {

    @Resource
    private SeckillActivityMapper seckillActivityMapper;

    @Resource
    private SeckillActivityService seckillActivityService;

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
                seckillActivityService.startActivity(activity.getId());
                log.info("==> 定时任务：秒杀活动已开始并预热完成, activityId={}, activityName={}",
                        activity.getId(), activity.getActivityName());
            } catch (Exception e) {
                log.error("==> 定时任务：秒杀活动预热失败, activityId={}", activity.getId(), e);
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
