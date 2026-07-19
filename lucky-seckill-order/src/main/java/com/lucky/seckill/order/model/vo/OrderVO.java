package com.lucky.seckill.order.model.vo;

import com.lucky.seckill.order.enums.OrderStatusEnum;
import com.lucky.seckill.order.model.entity.Order;
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
public class OrderVO {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 订单商品信息（含所属店铺）
     */
    private OrderGoodsVO goods;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品图片
     */
    private String goodsImg;

    /**
     * SKU ID
     */
    private Long skuId;

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
     * 订单状态(0:待支付 1:待发货 2:已发货 3:已收货 4:已取消 5:已退款 6:已关闭)
     */
    private Integer status;

    /**
     * 订单状态描述
     */
    private String statusDesc;

    /**
     * 收货人姓名
     */
    private String receiver;

    /**
     * 收货人手机号
     */
    private String mobile;

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
     * 是否可确认收货（已发货状态为 true）
     */
    private Boolean canReceive;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 从实体转换为VO
     */
    public static OrderVO fromEntity(Order order) {
        if (order == null) {
            return null;
        }
        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .goodsId(order.getGoodsId())
                .shopId(order.getShopId())
                .goodsName(order.getGoodsName())
                .goodsImg(order.getGoodsImg())
                .skuId(order.getSkuId())
                .goodsPrice(order.getGoodsPrice())
                .buyGoodsNum(order.getBuyGoodsNum())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .statusDesc(getStatusDesc(order.getStatus()))
                .receiver(order.getReceiver())
                .mobile(order.getMobile())
                .province(order.getProvince())
                .city(order.getCity())
                .area(order.getArea())
                .detailAddress(order.getDetailAddress())
                .payTime(order.getPayTime())
                .deliveryTime(order.getDeliveryTime())
                .receiveTime(order.getReceiveTime())
                .canReceive(OrderStatusEnum.SHIPPED.getCode().equals(order.getStatus()))
                .expireTime(order.getExpireTime())
                .createTime(order.getCreateTime())
                .build();
    }

    /**
     * 获取订单状态描述
     */
    private static String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        return OrderStatusEnum.getDescByCode(status);
    }
}
