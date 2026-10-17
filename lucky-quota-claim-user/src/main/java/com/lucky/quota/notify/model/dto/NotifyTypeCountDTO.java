package com.lucky.quota.notify.model.dto;

import lombok.Data;

/**
 * 按消息类型聚合统计（Mapper 查询结果）
 */
@Data
public class NotifyTypeCountDTO {

    private Integer notifyType;

    private Long totalCount;

    private Long unreadCount;
}
