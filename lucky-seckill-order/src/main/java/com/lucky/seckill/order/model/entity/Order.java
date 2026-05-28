package com.lucky.seckill.order.model.entity;

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
public class Order {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 普通商品ID
     */
    private Long goodsId;

    /**
     * 商品规格ID
     */
    private Long skuId;

    /**
     * 普通订单号
     */
    private String orderNo;

    /**
     * 商品单价
     */
    private BigDecimal goodsPrice;

    /**
     * 购买数量
     */
    private Integer buyGoodsNum;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 商品名称(冗余)
     */
    private String goodsName;

    /**
     * 商品图片(冗余)
     */
    private String goodsImg;

    /**
     * 订单状态(0:待支付 1:待发货 2:已发货 3:已收货 4:已退款 5:已取消 6:已关闭)
     */
    private Integer status;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 发货时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 确认收货时间
     */
    private LocalDateTime receiveTime;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 是否删除(0:未删除 1:已删除)
     */
    private Integer isDelete;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}