package com.lucky.seckill.shop.config;

import cn.dev33.satoken.stp.StpInterface;
import com.lucky.seckill.common.constant.RedisKeyConstants;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.feign.UserFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sa-Token 角色认证实现（Shop 服务）
 * 优先从 Redis 读取角色（与 user 服务登录时写入的缓存一致），失败时再 Feign 调用 user 服务
 */
@Component
@Slf4j
public class SaTokenRoleImpl implements StpInterface {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserFeignClient userFeignClient;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());

        List<String> rolesFromRedis = getRolesFromRedis(userId);
        if (!rolesFromRedis.isEmpty()) {
            log.debug("==> 从 Redis 获取用户角色, userId: {}, roles: {}", userId, rolesFromRedis);
            return rolesFromRedis;
        }

        try {
            Response<List<String>> response = userFeignClient.getUserRoles(userId);
            if (response != null && response.isSuccess() && response.getData() != null && !response.getData().isEmpty()) {
                log.info("==> 从 user 服务获取用户角色, userId: {}, roles: {}", userId, response.getData());
                return response.getData();
            }
        } catch (Exception e) {
            log.error("==> 调用 user 服务获取角色失败, userId: {}, error: {}", userId, e.getMessage(), e);
        }

        log.warn("==> 用户没有可用角色, userId: {}", userId);
        return new ArrayList<>();
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<String> getRolesFromRedis(Long userId) {
        String rolesKey = RedisKeyConstants.USER_ROLES_KEY_PREFIX + userId;
        Object cached = redisTemplate.opsForValue().get(rolesKey);
        if (cached == null) {
            return new ArrayList<>();
        }
        if (cached instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
