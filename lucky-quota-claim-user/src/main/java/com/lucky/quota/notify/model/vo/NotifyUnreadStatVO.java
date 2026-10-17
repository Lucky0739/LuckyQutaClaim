package com.lucky.quota.notify.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 按用户 ID、消息类型统计的未读数量
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyUnreadStatVO {

    private Long receiveUserId;

    private Integer notifyType;

    private String notifyTypeName;

    private Long unreadCount;
}
