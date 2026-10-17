package com.lucky.quota.activity.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 限量申领单最终失败记录（DLQ 重试全部耗尽后落库，供人工介入处理）。
 * <p>
 * 管理员可通过后台：
 * <ul>
 *   <li>查看失败申领列表</li>
 *   <li>手动重试（重新投递到主队列）</li>
 *   <li>手动确认放弃（标记为已处理）</li>
 * </ul>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_seckill_failed_order")
public class QuotaFailedOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long activityId;

    private Long goodsId;

    private Long skuId;

    private Long seckillGoodsId;

    private String orderNo;

    private BigDecimal seckillPrice;

    private String goodsName;

    private String goodsImg;

    private Integer buyNum;

    /** 完整的消息 JSON，用于重试时反序列化 */
    private String messageJson;

    /** 最后一次失败的错误信息 */
    private String lastError;

    /** 已重试次数（主队列 + DLQ 总次数） */
    private Integer retryCount;

    /**
     * 处理状态：0=待人工处理，1=已手动重试成功，2=已手动放弃
     */
    private Integer handleStatus;

    /** 处理备注（管理员填写） */
    private String handleRemark;

    /** 处理人 */
    private String handledBy;

    private LocalDateTime handledTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
