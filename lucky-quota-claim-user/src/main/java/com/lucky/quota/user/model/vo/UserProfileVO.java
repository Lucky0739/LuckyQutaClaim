        package com.lucky.quota.user.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lucky
 * @Date: 2026/5/18
 * @Version: v1.0.0
 * @Description: 用户资料响应
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileVO {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号（脱敏）
     */
    private String mobile;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 用户状态
     */
    private Integer status;
}
