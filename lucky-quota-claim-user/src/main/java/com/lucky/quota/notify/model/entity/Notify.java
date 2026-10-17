package com.lucky.quota.notify.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

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
@TableName("t_notify")
public class Notify {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
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
     * 消息类型 1=发放单位申请 2=物资上架申请 3=上架审核通过 4=申领确认 5=发放通知 6=领取确认
     */
    private Integer notifyType;

    /**
     * 关联业务ID（申请ID/物资ID/申领单ID）
     */
    private Long businessId;

    /**
     * 子类型：区分点赞/收藏的目标类型 0=无 1=帖子 2=物资 3=评论
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
    @TableLogic
    private Integer isDelete;
}