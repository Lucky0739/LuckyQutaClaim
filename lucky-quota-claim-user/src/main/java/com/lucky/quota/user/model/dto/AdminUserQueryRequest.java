package com.lucky.quota.user.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员查询用户列表请求DTO
 */
@Data
public class AdminUserQueryRequest {

    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 用户昵称（模糊查询）
     */
    private String nickname;

    /**
     * 手机号（模糊查询）
     */
    private String mobile;

    /**
     * 用户状态（0-正常，1-禁用）
     */
    private Integer status;

    /**
     * 开始时间（创建时间范围查询）
     */
    private LocalDateTime startTime;

    /**
     * 结束时间（创建时间范围查询）
     */
    private LocalDateTime endTime;
}
