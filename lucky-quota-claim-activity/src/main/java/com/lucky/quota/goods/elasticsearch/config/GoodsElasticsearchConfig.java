package com.lucky.quota.goods.elasticsearch.config;

import com.lucky.quota.goods.config.GoodsElasticsearchProperties;
import com.lucky.quota.goods.elasticsearch.service.GoodsElasticsearchService;
import com.lucky.quota.goods.elasticsearch.service.impl.GoodsElasticsearchServiceImpl;
import com.lucky.quota.goods.elasticsearch.service.impl.NoOpGoodsElasticsearchService;
import com.lucky.quota.goods.elasticsearch.support.GoodsIndexStartupRunner;
import com.lucky.quota.goods.mapper.GoodsCategoryMapper;
import com.lucky.quota.goods.mapper.GoodsMapper;
import com.lucky.quota.goods.mapper.GoodsSkuMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@Configuration
public class GoodsElasticsearchConfig {

    @Configuration
    @ConditionalOnProperty(prefix = "goods.elasticsearch", name = "enabled", havingValue = "true")
    static class EnabledConfig {

        @Bean
        @Lazy
        GoodsElasticsearchService goodsElasticsearchService(
                GoodsElasticsearchProperties properties,
                GoodsMapper goodsMapper,
                GoodsSkuMapper goodsSkuMapper,
                GoodsCategoryMapper goodsCategoryMapper,
                @Lazy ElasticsearchOperations elasticsearchOperations) {
            return new GoodsElasticsearchServiceImpl(
                    properties,
                    goodsMapper,
                    goodsSkuMapper,
                    goodsCategoryMapper,
                    elasticsearchOperations);
        }

        @Bean
        GoodsIndexStartupRunner goodsIndexStartupRunner(
                GoodsElasticsearchProperties properties,
                @Lazy GoodsElasticsearchService goodsElasticsearchService) {
            return new GoodsIndexStartupRunner(properties, goodsElasticsearchService);
        }
    }

    @Configuration
    @ConditionalOnProperty(prefix = "goods.elasticsearch", name = "enabled", havingValue = "false", matchIfMissing = true)
    static class DisabledConfig {

        @Bean
        GoodsElasticsearchService goodsElasticsearchService() {
            return new NoOpGoodsElasticsearchService();
        }
    }
}
