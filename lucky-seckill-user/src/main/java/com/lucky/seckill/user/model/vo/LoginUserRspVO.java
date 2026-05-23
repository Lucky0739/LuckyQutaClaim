package com.lucky.seckill.user.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: Lucky
 * @Date: 2026/4/12 21:12
 * @Version: v1.0.0
 * @Description: 用户登录
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginUserRspVO {

    /**
     * Token 令牌
     */
    private String token;

    /**
     * 用户信息
     */
    private UserInfo userInfo;

    /**
     * 用户信息内部类
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserInfo {

        /**
         * 用户 ID
         */
        private Long id;

        /**
         * 昵称
         */
        private String nickname;

        /**
         * 头像
         */
        private String avatar;

        /**
         * 用户角色列表（用于前端权限判断）
         */
        private List<String> roles;
    }
}

