package com.lucky.quota.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 限量单镜像写入 t_order 用的行数据（与订单服务字段对齐） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MirroredOrderRow {

    private Long id;
    private Long userId;
    private Long goodsId;
    private Long shopId;
    private Long skuId;
    private String orderNo;
    private BigDecimal goodsPrice;
    private Integer buyGoodsNum;
    private BigDecimal totalAmount;
    private String goodsName;
    private String goodsImg;
    private Integer status;
    private String mobile;
    private String receiver;
    private String province;
    private String city;
    private String area;
    private String detailAddress;
    private LocalDateTime expireTime;
    private Integer isDelete;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
