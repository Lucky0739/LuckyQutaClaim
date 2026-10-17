package com.lucky.quota.goods.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商户「我的物资」详情（含管理状态字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantGoodsDetailVO {

    private Long goodsId;

    private String goodsName;

    private String goodsImg;

    private BigDecimal goodsPrice;

    private List<String> goodsImgList;

    private String detailContent;

    private List<SkuInfoVO> skuList;

    private Long categoryId;

    private Long shopId;

    /**
     * 上架状态：0=下架 1=上架
     */
    private Integer status;

    /**
     * 审核状态：0=未申请 1=待审核 2=通过 3=拒绝
     */
    private Integer auditStatus;
}
