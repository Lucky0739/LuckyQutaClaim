package com.lucky.seckill.common.constant;

/**
 * Redis Key 常量（跨服务共享）
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /**
     * 用户角色缓存：user:roles:{userId}
     */
    public static final String USER_ROLES_KEY_PREFIX = "user:roles:";
}
