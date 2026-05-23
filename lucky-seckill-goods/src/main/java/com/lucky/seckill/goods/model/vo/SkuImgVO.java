package com.lucky.seckill.goods.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SkuImgVO {
    private Long skuId;

    private String skuSpecName;

    private String skuImg;
}
