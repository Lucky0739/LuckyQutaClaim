package com.lucky.quota.goods.model.dto;

import lombok.Data;

/**
 * 物资排行统计（Mapper 查询）
 */
@Data
public class GoodsRankStatDTO {

    private Long goodsId;

    private Integer collectNum;

    private Integer salesNum;
}
