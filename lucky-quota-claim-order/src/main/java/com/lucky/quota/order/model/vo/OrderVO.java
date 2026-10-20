package com.lucky.quota.order.model.vo;

import com.lucky.quota.order.enums.OrderStatusEnum;
import com.lucky.quota.order.model.entity.Order;
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
     * 申领单ID
     */
    private Long id;

    /**
     * 申领单号
     */
    private String orderNo;

    /**
     * 物资ID
     */
    private Long goodsId;

    /**
     * 发放单位ID
     */
    private Long shopId;

    /**
     * 申领单物资信息（含所属发放单位）
     */
    private OrderGoodsVO goods;

    /**
     * 物资名称
     */
    private String goodsName;

    /**
     * 物资图片
     */
    private String goodsImg;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 物资单价
     */
    private BigDecimal goodsPrice;

    /**
     * 申领数量
     */
    private Integer buyGoodsNum;

    /**
     * 对应金额
     */
    private BigDecimal totalAmount;

    /**
     * 申领状态(0:待确认领取 1:待发放 2:已发放 3:已到货 4:已取消 5:已退款 6:已关闭)
     */
    private Integer status;

    /**
     * 申领状态描述
     */
    private String statusDesc;

    /**
     * 领取人姓名
     */
    private String receiver;

    /**
     * 领取人手机号
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
     * 确认领取时间
     */
    private LocalDateTime payTime;

    /**
     * 发放时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 确认到货时间
     */
    private LocalDateTime receiveTime;

    /**
     * 是否可确认到货（已发放状态为 true）
     */
    private Boolean canReceive;

    /**
     * 申领过期时间
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
     * 获取申领状态描述
     */
    private static String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        return OrderStatusEnum.getDescByCode(status);
    }
}
