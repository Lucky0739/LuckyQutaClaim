package com.lucky.seckill.shop.scheduler;

import com.lucky.seckill.shop.service.ShopCollectService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 店铺收藏数定时同步：将 Redis 中的收藏计数同步到 MySQL
 */
@Component
@Slf4j
public class ShopCollectSyncScheduler {

    @Resource
    private ShopCollectService shopCollectService;

    @Scheduled(fixedDelay = 60000)
    public void syncCollectCount() {
        try {
            shopCollectService.syncCollectCountToDb();
        } catch (Exception e) {
            log.error("==> 定时任务：店铺收藏数同步失败", e);
        }
    }
}
