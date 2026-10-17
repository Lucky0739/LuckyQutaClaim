
package com.lucky.quota.goods.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class GoodsListVO {
    private Long id;

    private String goodsName;

    private String goodsImg;

    private BigDecimal goodsPrice;

    /**
     * 已售数量（该物资对应的所有规格的已售数量之和）
     */
    private Integer salesNum;

    private Long categoryId;

    private String categoryName;

    private Long parentCategoryId;

    private String parentCategoryName;

    /**
     * 上架状态：0=下架 1=上架
     */
    private Integer status;

    /**
     * 审核状态：0=未申请 1=待审核 2=通过 3=拒绝
     */
    private Integer auditStatus;

    private Long shopId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}