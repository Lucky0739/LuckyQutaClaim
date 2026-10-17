package com.lucky.quota.goods.scheduler;

import com.lucky.quota.goods.service.GoodsRankRedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 首页物资排行 ZSet 定时全量重建（与 DB、Redis 收藏数对齐）
 */
@Component
@Slf4j
public class GoodsRankSyncScheduler {

    @Resource
    private GoodsRankRedisService goodsRankRedisService;

    @Scheduled(fixedDelay = 300000)
    public void rebuildGoodsRank() {
        try {
            goodsRankRedisService.rebuildRankFromDb();
        } catch (Exception e) {
            log.error("==> 定时任务：首页物资排行 ZSet 重建失败", e);
        }
    }
}
