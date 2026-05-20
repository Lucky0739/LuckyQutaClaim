package com.lucky.seckill.notify.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 全站站内消息表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notify {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 发送人ID 0=系统推送
     */
    private Long sendUserId;

    /**
     * 接收人ID(管理员/商家/用户)
     */
    private Long receiveUserId;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型 1=开店申请 2=点赞 3=收藏 4=审核通知
     */
    private Integer notifyType;

    /**
     * 关联业务ID（申请ID/帖子ID/商品ID）
     */
    private Long businessId;

    /**
     * 子类型：区分点赞/收藏的目标类型 0=无 1=帖子 2=商品 3=评论
     */
    private Integer subType;

    /**
     * 是否已读 0未读 1已读
     */
    private Integer isRead;

    /**
     * 读取时间
     */
    private LocalDateTime readTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除 0未删除 1已删除
     */
    private Integer isDelete;
}