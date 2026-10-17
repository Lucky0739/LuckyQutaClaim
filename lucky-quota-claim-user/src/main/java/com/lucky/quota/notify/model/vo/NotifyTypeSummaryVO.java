package com.lucky.quota.notify.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息类型 Tab 汇总（未读数/总数）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyTypeSummaryVO {

    private Integer notifyType;

    private String notifyTypeName;

    private Long unreadCount;

    private Long totalCount;
}
