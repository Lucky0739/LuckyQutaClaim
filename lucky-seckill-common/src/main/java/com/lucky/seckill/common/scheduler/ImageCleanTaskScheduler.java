package com.lucky.seckill.common.scheduler;

import com.lucky.seckill.common.service.ImageCleanTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 待清理图片定时删除任务
 */
@Component
@Slf4j
public class ImageCleanTaskScheduler {

    @Resource
    private ImageCleanTaskService imageCleanTaskService;

    @Scheduled(fixedDelay = 60000)
    public void cleanExpiredImages() {
        try {
            imageCleanTaskService.processExpiredTasks();
        } catch (Exception e) {
            log.error("==> 定时任务：清理过期图片失败", e);
        }
    }
}
