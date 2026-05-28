package com.lucky.seckill.notify.model.dto;

import lombok.Data;

/**
 * 通知查询请求DTO
 */
@Data
public class NotifyQueryRequest {

    /**
     * 通知类型
     */
    private Integer notifyType;

    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
