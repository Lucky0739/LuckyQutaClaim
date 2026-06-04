package com.lucky.seckill.notify.model.dto;

import lombok.Data;

/**
 * 通知查询请求DTO
 */
@Data
public class NotifyQueryRequest {

    /**
     * 消息类型（可选，不传则查询用户可见的全部类型）
     * 1=开店申请 2=点赞 3=收藏 4=审核通知 5=支付成功 6=发货通知
     */
    private Integer notifyType;

    /**
     * 已读状态（可选）：0=未读 1=已读，不传则查全部
     */
    private Integer isRead;

    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
