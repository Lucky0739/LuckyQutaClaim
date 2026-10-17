package com.lucky.quota.activity.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

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
@TableName("t_seckill_goods")
public class QuotaGoods {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long goodsId;

    private Long skuId;

    private String skuSpecName;

    private String seckillTitle;

    private String seckillImg;

    private BigDecimal seckillPrice;

    private Integer seckillTotal;

    private Integer seckillStock;

    private Integer seckillLimit;

    private Integer sort;

    private Integer auditStatus;

    private String auditRemark;

    @Version
    private Integer version;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
