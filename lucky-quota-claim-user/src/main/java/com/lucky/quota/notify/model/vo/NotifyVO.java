package com.lucky.quota.notify.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lucky.quota.notify.enums.NotifyTypeEnum;
import com.lucky.quota.notify.model.entity.Notify;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息展示 VO，供前端列表/详情使用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyVO {

    private Long id;

    private Long sendUserId;

    private Long receiveUserId;

    private String title;

    private String content;

    private Integer notifyType;

    private String notifyTypeName;

    private Long businessId;

    private Integer subType;

    /**
     * 是否已读：0=未读 1=已读
     */
    private Integer isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime readTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    public static NotifyVO from(Notify notify) {
        if (notify == null) {
            return null;
        }
        return NotifyVO.builder()
                .id(notify.getId())
                .sendUserId(notify.getSendUserId())
                .receiveUserId(notify.getReceiveUserId())
                .title(notify.getTitle())
                .content(notify.getContent())
                .notifyType(notify.getNotifyType())
                .notifyTypeName(NotifyTypeEnum.getNameByCode(notify.getNotifyType()))
                .businessId(notify.getBusinessId())
                .subType(notify.getSubType())
                .isRead(notify.getIsRead())
                .readTime(notify.getReadTime())
                .createTime(notify.getCreateTime())
                .build();
    }
}
