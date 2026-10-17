package com.lucky.quota.goods.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 首页物资排行权重配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "goods.rank")
public class GoodsRankProperties {

    /**
     * 收藏量权重（默认 3）
     */
    private double collectWeight = 3.0;

    /**
     * 已售数量权重（默认 1）
     */
    private double salesWeight = 1.0;

    public double calcWeightedScore(int collectNum, int salesNum) {
        return collectWeight * collectNum + salesWeight * salesNum;
    }
}
