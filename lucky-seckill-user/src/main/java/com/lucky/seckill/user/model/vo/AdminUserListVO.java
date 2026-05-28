package com.lucky.seckill.user.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户手机号（脱敏）
     */
    private String mobile;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 用户状态 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 用户状态描述
     */
    private String statusDesc;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 用户角色列表
     */
    private List<String> roleNames;
}