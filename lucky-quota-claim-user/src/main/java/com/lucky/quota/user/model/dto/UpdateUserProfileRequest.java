package com.lucky.quota.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lucky
 * @Date: 2026/5/17
 * @Version: v1.0.0
 * @Description: 完善用户资料请求
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class
UpdateUserProfileRequest {

    /**
     * 用户昵称
     */
    @NotBlank(message = "用户昵称不能为空")
    private String nickname;

}
