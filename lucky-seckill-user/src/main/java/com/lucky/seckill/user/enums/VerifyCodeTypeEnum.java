package com.lucky.seckill.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: Lucky
 * @Date: 2026/4/14 17:23
 * @Version: v1.0.0
 * @Description: 验证码类型枚举
 **/
@Getter
@AllArgsConstructor
public enum VerifyCodeTypeEnum {

    REGISTER(1, "register", "注册"),
    LOGIN(2, "login", "登录"),
    CHANGE_PASSWORD(3, "change_password", "修改密码"),
    CANCEL_ACCOUNT(4, "cancel_account", "注销账号")
    ;

    // 类型值
    private final Integer code;
    // 场景标识（用于拼接 Redis Key）
    private final String purpose;
    // 类型描述
    private final String description;

    /**
     * 根据 code 获取枚举
     *
     * @param code
     * @return
     */
    public static VerifyCodeTypeEnum valueOf(Integer code) {
        for (VerifyCodeTypeEnum typeEnum : values()) {
            if (typeEnum.getCode().equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }
}

