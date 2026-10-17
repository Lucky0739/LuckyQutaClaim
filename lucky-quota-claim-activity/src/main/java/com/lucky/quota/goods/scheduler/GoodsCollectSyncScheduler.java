package com.lucky.quota.goods.scheduler;

import com.lucky.quota.goods.service.GoodsCollectService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 物资收藏数定时同步：将 Redis 中的收藏计数同步到 MySQL
 */
@Component
@Slf4j
public class GoodsCollectSyncScheduler {

    @Resource
    private GoodsCollectService goodsCollectService;

    @Scheduled(fixedDelay = 60000)
    public void syncCollectCount() {
        try {
            goodsCollectService.syncCollectCountToDb();
        } catch (Exception e) {
            log.error("==> 定时任务：物资收藏数同步失败", e);
        }
    }
}
