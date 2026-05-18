package com.lucky.seckill.user.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.lucky.seckill.user.mapper.RoleMapper;
import com.lucky.seckill.user.model.entity.Role;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sa-Token 角色认证实现
 * 基于角色的访问控制（RBAC）
 *
 * 采用混合模式：优先从 Session 读取，不存在时查数据库并缓存
 */
@Component
@Slf4j
public class SaTokenRoleImpl implements StpInterface {

    @Resource
    private RoleMapper roleMapper;

    /**
     * 返回一个账号所拥有的角色标识集合
     *
     * 1. 优先从 Session 中读取（高性能）
     * 2. Session 中没有则从数据库查询并缓存
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());

        // 1. 尝试从 Session 中读取
        List<String> roleList = (List<String>) StpUtil.getSession().get("roles");

        if (roleList != null && !roleList.isEmpty()) {
            log.debug("==> 从 Session 获取用户角色, userId: {}, roles: {}", userId, roleList);
            return roleList;
        }

        // 2. Session 中没有，从数据库查询
        log.info("==> Session 中无角色信息，从数据库查询, userId: {}", userId);
        List<Role> roles = roleMapper.selectRolesByUserId(userId);

        if (roles == null || roles.isEmpty()) {
            log.warn("==> 用户没有分配角色, userId: {}", userId);
            roleList = new ArrayList<>();
        } else {
            // 提取角色编码
            roleList = roles.stream()
                    .map(Role::getRoleCode)
                    .collect(Collectors.toList());

            // 3. 缓存到 Session（下次直接从 Redis 读取）
            StpUtil.getSession().set("roles", roleList);
            log.info("==> 用户角色已缓存到 Session, userId: {}, roles: {}", userId, roleList);
        }

        return roleList;
    }

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 不需要权限控制，返回空列表
        return new ArrayList<>();
    }
}
