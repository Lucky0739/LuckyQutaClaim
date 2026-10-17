package com.lucky.quota.goods.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 物资搜索请求（面向 C 端：仅搜索已上架且审核通过的物资）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsSearchRequest {

    /**
     * 搜索关键词（物资名称）
     */
    private String keyword;

    private Long categoryId;

    private Long parentCategoryId;

    @JsonIgnore
    private List<Long> categoryIds;

    private Long shopId;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
