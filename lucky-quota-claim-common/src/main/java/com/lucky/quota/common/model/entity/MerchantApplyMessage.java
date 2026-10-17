package com.lucky.quota.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 商家申请消息实体类
 * 用于在用户申请发放单位时，通过RocketMQ发送消息通知管理员进行审批
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantApplyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 发放单位ID
     */
    private Long shopId;

    /**
     * 申请用户ID
     */
    private Long userId;

    /**
     * 发放单位名称
     */
    private String shopName;

    /**
     * 单位头像/LOGO
     */
    private String shopAvatar;

    /**
     * 单位简介
     */
    private String shopDesc;

    /**
     * 申请时间
     */
    private Date applyTime;

    /**
     * 消息ID（用于追踪）
     */
    private String messageId;

    /**
     * 消息状态：0-待处理 1-已处理 2-处理失败
     */
    private Integer status;

    /**
     * 处理时间
     */
    private Date processTime;

    /**
     * 处理结果备注
     */
    private String remark;
}
