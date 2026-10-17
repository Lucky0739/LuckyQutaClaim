package com.lucky.quota.goods.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsCollectStatusVO {

    private Long goodsId;

    private Boolean collected;

    private Integer collectNum;
}
