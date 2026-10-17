package com.lucky.quota.goods.runner;

import com.lucky.quota.goods.service.GoodsRankRedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时初始化首页排行 ZSet
 */
@Component
@Slf4j
public class GoodsRankInitRunner implements ApplicationRunner {

    @Resource
    private GoodsRankRedisService goodsRankRedisService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            goodsRankRedisService.rebuildRankFromDb();
            log.info("==> 应用启动：首页物资排行 ZSet 初始化完成");
        } catch (Exception e) {
            log.error("==> 应用启动：首页物资排行 ZSet 初始化失败", e);
        }
    }
}
