package com.lucky.seckill.user.service;

import cn.hutool.core.util.StrUtil;
import com.lucky.seckill.common.constant.RedisKeyConstants;
import com.lucky.seckill.user.model.vo.UserProfileVO;
import jakarta.annotation.Resource;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户资料 Redis Hash 缓存
 * <p>
 * Key: user:profile:{userId}，field 为资料字段名，value 为字符串
 */
@Service
public class UserProfileRedisService {

    private static final long USER_PROFILE_EXPIRE_HOURS = 24L;

    static final String FIELD_ID = "id";
    static final String FIELD_NICKNAME = "nickname";
    static final String FIELD_MOBILE = "mobile";
    static final String FIELD_AVATAR = "avatar";
    static final String FIELD_STATUS = "status";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 写入或刷新用户资料 Hash 缓存
     */
    public void save(UserProfileVO profile) {
        if (profile == null || profile.getId() == null) {
            return;
        }
        String key = RedisKeyConstants.userProfileKey(profile.getId());
        Map<String, String> hash = new HashMap<>(8);
        hash.put(FIELD_ID, String.valueOf(profile.getId()));
        putIfNotBlank(hash, FIELD_NICKNAME, profile.getNickname());
        putIfNotBlank(hash, FIELD_MOBILE, profile.getMobile());
        putIfNotBlank(hash, FIELD_AVATAR, profile.getAvatar());
        if (profile.getStatus() != null) {
            hash.put(FIELD_STATUS, String.valueOf(profile.getStatus()));
        }
        // 兼容旧版 String(JSON) 缓存，避免 HMSET 触发 WRONGTYPE
        deleteIfNotHash(key);
        stringRedisTemplate.opsForHash().putAll(key, hash);
        stringRedisTemplate.expire(key, USER_PROFILE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 从 Hash 读取用户资料，不存在或字段不完整时返回 null
     */
    public UserProfileVO get(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = RedisKeyConstants.userProfileKey(userId);
        if (deleteIfNotHash(key)) {
            return null;
        }
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        String idStr = asString(entries.get(FIELD_ID));
        if (StrUtil.isBlank(idStr)) {
            return null;
        }
        UserProfileVO.UserProfileVOBuilder builder = UserProfileVO.builder()
                .id(Long.valueOf(idStr))
                .nickname(asString(entries.get(FIELD_NICKNAME)))
                .mobile(asString(entries.get(FIELD_MOBILE)))
                .avatar(asString(entries.get(FIELD_AVATAR)));
        String statusStr = asString(entries.get(FIELD_STATUS));
        if (StrUtil.isNotBlank(statusStr)) {
            builder.status(Integer.valueOf(statusStr));
        }
        return builder.build();
    }

    /**
     * 删除用户资料缓存
     */
    public void delete(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(RedisKeyConstants.userProfileKey(userId));
    }

    private static void putIfNotBlank(Map<String, String> hash, String field, String value) {
        if (StrUtil.isNotBlank(value)) {
            hash.put(field, value);
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 若 key 已存在且不是 Hash（例如旧版 String 整对象缓存），则删除以便重建。
     *
     * @return true 表示已删除非 Hash 数据，调用方应视为缓存未命中
     */
    private boolean deleteIfNotHash(String key) {
        DataType type = stringRedisTemplate.type(key);
        if (type == null || DataType.NONE.equals(type) || DataType.HASH.equals(type)) {
            return false;
        }
        stringRedisTemplate.delete(key);
        return true;
    }
}
