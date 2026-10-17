package com.lucky.quota.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.user.model.entity.UserRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserRoleMapper extends BaseMapper<UserRole> {

    int deleteByPrimaryKey(Long id);

    int insert(UserRole record);

    int insertSelective(UserRole record);

    UserRole selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserRole record);

    int updateByPrimaryKey(UserRole record);

    /**
     * 根据用户ID查询权限编码列表
     */
    List<String> selectCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询权限编码列表
     */
    List<String> selectPermCodesByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询角色关联（未删除的，仅返回一条）
     */
    UserRole selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID与角色ID查询未删除的关联
     */
    UserRole selectByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 根据用户ID与角色ID查询关联（含已逻辑删除）
     */
    UserRole selectByUserIdAndRoleIdIgnoreDelete(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 根据用户ID查询已逻辑删除的角色关联
     */
    UserRole selectDeletedByUserId(@Param("userId") Long userId);

    /**
     * 按用户 ID 逻辑删除全部角色关联
     */
    int logicalDeleteByUserId(@Param("userId") Long userId);
}
