package com.lucky.quota.activity.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

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
@TableName("t_seckill_order")
public class QuotaOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long activityId;

    private Long goodsId;

    private Long skuId;

    private String orderNo;

    private BigDecimal seckillPrice;

    private String goodsName;

    private String goodsImg;

    /**
     * 申领状态 0=待确认领取 1=待发放 2=已发放 3=已到货 4=已退款 5=已取消 6=已关闭
     */
    private Integer status;

    private LocalDateTime expireTime;

    @TableField("isDeleted")
    @TableLogic
    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
