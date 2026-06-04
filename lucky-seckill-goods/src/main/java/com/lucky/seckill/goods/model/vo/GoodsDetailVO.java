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
    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品主图
     */
    private String goodsImg;
    /**
     * 商品价格（分页查询）
     */
    private BigDecimal goodsPrice;

    /**
     * 商品主图数组
     */
    private List<String> goodsImgList;
    /**
     * 商品详细内容
     */
    private String detailContent;

    /**
     * 商品规格列表
     */
    private List<SkuInfoVO> skuList;

}
