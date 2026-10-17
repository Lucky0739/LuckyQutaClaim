package com.lucky.quota.notify.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户消息分页结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyListVO {

    private List<NotifyVO> records;

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    private Integer totalPages;

    /**
     * 当前筛选条件下的未读消息数
     */
    private Long unreadCount;
}
