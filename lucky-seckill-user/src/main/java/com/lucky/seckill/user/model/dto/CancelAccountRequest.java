package com.lucky.seckill.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注销账号请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CancelAccountRequest {

    /**
     * 登录密码（二次确认）
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 短信验证码（type=4 注销账号）
     */
    @NotBlank(message = "验证码不能为空")
    private String verifyCode;
}
