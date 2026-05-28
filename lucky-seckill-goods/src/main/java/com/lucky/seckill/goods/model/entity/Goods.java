package com.lucky.seckill.goods.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Goods {
    private Long id;

    private String goodsName;

    private String goodsImg;

    private BigDecimal goodsPrice;

    private Long categoryId;

    private Long userId;

    private Long shopId;

    /**
     * 商品上架审核状态
     */
    private Integer auditStatus;

    /**
     * 商品状态 0=下架 1=上架
     */
    private Integer status;

    /**
     * 商品收藏数
     */
    private Integer collectNum;


    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}