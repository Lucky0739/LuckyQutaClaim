package com.lucky.quota.goods.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 物资 Elasticsearch 搜索配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "goods.elasticsearch")
public class GoodsElasticsearchProperties {

    /**
     * 是否启用 ES 搜索（关闭时搜索接口回退 MySQL LIKE）
     */
    private boolean enabled = false;

    /**
     * 索引名称
     */
    private String indexName = "lucky_goods";

    /**
     * 启动时是否全量重建索引（仅上架且审核通过的物资）
     */
    private boolean rebuildOnStartup = false;
}
