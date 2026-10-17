package com.lucky.quota.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.lucky.quota.user.model.entity.Role;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {
    int deleteByPrimaryKey(Long id);

    int insert(Role record);

    int insertSelective(Role record);

    Role selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Role record);

    int updateByPrimaryKey(Role record);

    /**
     * 根据用户ID查询角色列表
     */
    List<Role> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 根据角色编码查询角色
     */
    Role selectByRoleCode(@Param("roleCode") String roleCode);

}