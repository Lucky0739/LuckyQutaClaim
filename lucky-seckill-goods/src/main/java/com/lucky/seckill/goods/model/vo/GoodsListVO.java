
package com.lucky.seckill.goods.model.vo;

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

    private Integer status;

    private String statusDesc;

    private Integer auditStatus;

    private String auditStatusDesc;

    /**
     * 已售数量（该商品对应的所有规格的已售数量之和）
     */
    private Integer salesNum;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}