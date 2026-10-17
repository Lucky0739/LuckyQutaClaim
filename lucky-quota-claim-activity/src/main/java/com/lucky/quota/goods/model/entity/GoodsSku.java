package com.lucky.quota.goods.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@TableName("t_goods_sku")
public class GoodsSku {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long goodsId;

    private String skuSpecName;

    private String skuImg;

    private BigDecimal marketPrice;

    private BigDecimal salePrice;

    private Integer stockNum;

    private Integer salesNum;

    private Integer status;

    @TableLogic
    private Integer isDelete;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}