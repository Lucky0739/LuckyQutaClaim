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

    /**
     * 用户已读消息 ZSet：notify:read:{userId}:{notifyType}，member=消息ID，score=已读时间戳
     */
    public static final String NOTIFY_READ_KEY_PREFIX = "notify:read:";

    public static String notifyReadKey(Long userId, Integer notifyType) {
        return NOTIFY_READ_KEY_PREFIX + userId + ":" + notifyType;
    }
}
