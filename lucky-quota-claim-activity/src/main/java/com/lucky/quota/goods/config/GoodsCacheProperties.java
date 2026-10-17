package com.lucky.quota.goods.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 物资缓存配置（逻辑过期 + 互斥锁）
 */
@Data
@Component
@ConfigurationProperties(prefix = "goods.cache")
public class GoodsCacheProperties {

    /**
     * 逻辑过期时间（分钟）
     */
    private long logicalExpireMinutes = 10;

    /**
     * 物理过期时间（分钟），应大于逻辑过期时间
     */
    private long physicalExpireMinutes = 30;

    /**
     * 互斥锁过期时间（秒）
     */
    private long lockExpireSeconds = 10;

    /**
     * 获取锁失败时的最大重试次数
     */
    private int lockRetryTimes = 3;

    /**
     * 获取锁失败时的重试间隔（毫秒）
     */
    private long lockRetryIntervalMs = 50;
}
