package com.lucky.quota.notify.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全部标记已读结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyMarkAllReadVO {

    /**
     * 本次标记为已读的消息条数
     */
    private Integer markedCount;
}
