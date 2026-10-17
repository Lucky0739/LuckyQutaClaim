package com.lucky.quota.goods.elasticsearch.support;

import com.lucky.quota.goods.config.GoodsElasticsearchProperties;
import com.lucky.quota.goods.elasticsearch.service.GoodsElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

@Slf4j
@RequiredArgsConstructor
public class GoodsIndexStartupRunner implements ApplicationRunner {

    private final GoodsElasticsearchProperties properties;
    private final GoodsElasticsearchService goodsElasticsearchService;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isRebuildOnStartup()) {
            return;
        }
        try {
            goodsElasticsearchService.rebuildIndex();
            log.info("==> 物资 ES 索引启动重建完成, index: {}", properties.getIndexName());
        } catch (Exception e) {
            log.warn("==> 物资 ES 索引启动重建失败（可稍后调用 POST /goods/search/rebuild）: {}", e.getMessage());
        }
    }
}
