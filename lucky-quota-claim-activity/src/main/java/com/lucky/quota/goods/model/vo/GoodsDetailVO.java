package com.lucky.quota.goods.model.vo;

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
     * 物资ID
     */
    private Long goodsId;

    /**
     * 物资名称
     */
    private String goodsName;

    /**
     * 物资主图
     */
    private String goodsImg;
    /**
     * 物资价格（分页查询）
     */
    private BigDecimal goodsPrice;

    /**
     * 物资主图数组
     */
    private List<String> goodsImgList;
    /**
     * 物资详细内容
     */
    private String detailContent;

    /**
     * 物资规格列表
     */
    private List<SkuInfoVO> skuList;

}
