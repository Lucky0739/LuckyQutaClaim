package com.lucky.quota.goods.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 首页物资列表查询（按 Redis ZSet 排序）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsHomeQueryRequest {

    /**
     * 排序方式：weight=综合权重（默认），collect=收藏量，sales=已售数量，hot 等同 weight
     */
    private String sortType;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
