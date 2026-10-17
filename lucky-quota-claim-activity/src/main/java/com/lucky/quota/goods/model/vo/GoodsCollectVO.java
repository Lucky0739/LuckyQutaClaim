package com.lucky.quota.goods.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsCollectVO {

    private Long id;

    private Long goodsId;

    private String goodsName;

    private String goodsImg;

    private BigDecimal goodsPrice;

    private Integer collectNum;

    private LocalDateTime createTime;
}
