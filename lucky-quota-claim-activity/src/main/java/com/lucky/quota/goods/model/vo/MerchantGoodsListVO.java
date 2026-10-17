package com.lucky.quota.goods.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户「我的物资」列表项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantGoodsListVO {

    private Long id;

    private String goodsName;

    private String goodsImg;

    private BigDecimal goodsPrice;

    private Integer salesNum;

    private Long categoryId;

    private String categoryName;

    private Long parentCategoryId;

    private String parentCategoryName;

    private Long shopId;

    /**
     * 上架状态：0=下架 1=上架
     */
    private Integer status;

    /**
     * 审核状态：0=未申请 1=待审核 2=通过 3=拒绝
     */
    private Integer auditStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
