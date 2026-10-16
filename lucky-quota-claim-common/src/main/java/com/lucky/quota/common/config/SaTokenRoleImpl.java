package com.lucky.quota.common.config;

import cn.dev33.satoken.stp.StpInterface;
import com.lucky.quota.common.constant.RedisKeyConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class SaTokenRoleImpl implements StpInterface {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final Long USER_ROLES_EXPIRE_HOURS = 24L;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());

        String rolesKey = RedisKeyConstants.USER_ROLES_KEY_PREFIX + userId;
        List<String> roleList = (List<String>) redisTemplate.opsForValue().get(rolesKey);

        if (roleList != null && !roleList.isEmpty()) {
            log.debug("==> 从 Redis 获取用户角色, userId: {}, roles: {}", userId, roleList);
            return roleList;
        }

        log.warn("==> Redis 中无角色信息, userId: {}", userId);
        return new ArrayList<>();
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }
}