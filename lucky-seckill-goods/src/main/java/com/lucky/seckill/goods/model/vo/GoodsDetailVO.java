package com.lucky.seckill.goods.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoodsDetailVO {
    private Long goodsId;

    private String goodsName;

    private String goodsImg;

    private BigDecimal goodsPrice;

    private List<String> goodsImgList;

    private String detailContent;

    private List<SkuInfoVO> skuList;


}
