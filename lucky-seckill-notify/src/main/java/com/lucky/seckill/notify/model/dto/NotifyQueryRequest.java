package com.lucky.seckill.notify.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息查询请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyQueryRequest {

    /**
     * 消息类型 1=开店申请
     */
    private Integer notifyType;

    /**
     * 页码（默认第1页）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小（默认10条）
     */
    private Integer pageSize = 10;
}
