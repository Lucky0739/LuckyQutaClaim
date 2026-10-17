package com.lucky.quota.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lucky
 * @Date: 2026/5/18
 * @Version: v1.0.0
 * @Description: 管理员修改用户资料请求
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminUpdateUserProfileRequest {

    /**
     * 目标用户 ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 用户昵称
     */
    @NotBlank(message = "用户昵称不能为空")
    private String nickname;
}
