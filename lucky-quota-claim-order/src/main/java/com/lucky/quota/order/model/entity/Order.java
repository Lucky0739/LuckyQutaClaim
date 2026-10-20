package com.lucky.quota.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("t_order")
public class Order {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 物资ID
     */
    private Long goodsId;

    /**
     * 发放单位ID（冗余，来源于物资）
     */
    private Long shopId;

    /**
     * 物资规格ID
     */
    private Long skuId;

    /**
     * 用户地址ID(关联用)
     */
    private Long userAddressId;

    /**
     * 申领单号
     */
    private String orderNo;

    /**
     * 物资单价
     */
    private BigDecimal goodsPrice;

    /**
     * 申领数量
     */
    @TableField("buyGoods_num")
    private Integer buyGoodsNum;

    /**
     * 对应金额
     */
    private BigDecimal totalAmount;

    /**
     * 物资名称(冗余)
     */
    private String goodsName;

    /**
     * 物资图片(冗余)
     */
    private String goodsImg;

    /**
     * 申领状态(0:待确认领取 1:待发放 2:已发放 3:已到货 4:已取消 5:已退款 6:已关闭)
     */
    private Integer status;

    /**
     * 领取人手机号
     */
    private String mobile;

    /**
     * 领取人姓名
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
     * 确认领取时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime payTime;

    /**
     * 发放时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime deliveryTime;

    /**
     * 确认到货时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime receiveTime;

    /**
     * 确认领取截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;

    /**
     * 是否删除(0:未删除 1:已删除)
     */
    @TableLogic
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