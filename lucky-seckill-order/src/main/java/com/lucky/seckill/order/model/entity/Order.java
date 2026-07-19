package com.lucky.seckill.order.model.entity;

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
     * 店铺ID（冗余，来源于商品）
     */
    private Long shopId;

    /**
     * 商品规格ID
     */
    private Long skuId;

    /**
     * 用户地址ID(关联用)
     */
    private Long userAddressId;

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
     * 订单状态(0:待支付 1:待发货 2:已发货 3:已收货 4:已取消 5:已退款 6:已关闭)
     */
    private Integer status;

    /**
     * 收货人手机号
     */
    private String mobile;

    /**
     * 收货人姓名
     */
    private String receiver;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区县
     */
    private String area;

    /**
     * 详细地址
     */
    private String detailAddress;

    /**
     * 支付时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime payTime;

    /**
     * 发货时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime deliveryTime;

    /**
     * 确认收货时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime receiveTime;

    /**
     * 订单过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;

    /**
     * 是否删除(0:未删除 1:已删除)
     */
    private Integer isDelete;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}