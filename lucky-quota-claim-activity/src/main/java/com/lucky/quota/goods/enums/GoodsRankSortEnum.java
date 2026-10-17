package com.lucky.quota.goods.enums;

import com.lucky.quota.common.constant.RedisKeyConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 首页物资排序方式（对应 Redis ZSet）
 */
@Getter
@AllArgsConstructor
public enum GoodsRankSortEnum {

    /** 综合权重（收藏×权重 + 销量×权重），首页默认 */
    WEIGHT("weight", RedisKeyConstants.GOODS_RANK_WEIGHT_ZSET, "综合权重"),
    COLLECT("collect", RedisKeyConstants.GOODS_RANK_COLLECT_ZSET, "收藏量"),
    SALES("sales", RedisKeyConstants.GOODS_RANK_SALES_ZSET, "已售数量");

    private final String code;
    private final String redisKey;
    private final String desc;

    public static GoodsRankSortEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return WEIGHT;
        }
        String normalized = code.trim().toLowerCase();
        if ("hot".equals(normalized)) {
            return WEIGHT;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }
}
