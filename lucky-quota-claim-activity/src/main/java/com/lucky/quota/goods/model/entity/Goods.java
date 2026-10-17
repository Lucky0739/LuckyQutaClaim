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
@TableName("t_goods")
public class Goods {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String goodsName;

    private String goodsImg;

    private BigDecimal goodsPrice;

    private Long categoryId;

    private Long userId;

    private Long shopId;

    /**
     * 物资上架审核状态
     */
    private Integer auditStatus;

    /**
     * 物资状态 0=下架 1=上架
     */
    private Integer status;

    /**
     * 物资收藏数
     */
    private Integer collectNum;


    @TableLogic
    private Integer isDelete;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}