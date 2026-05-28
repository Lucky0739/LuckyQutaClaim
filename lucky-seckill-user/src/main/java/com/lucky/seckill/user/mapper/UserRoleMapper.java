package com.lucky.seckill.user.mapper;

import com.lucky.seckill.user.model.entity.UserRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserRoleMapper {

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
     * 根据用户ID查询角色关联（未删除的）
     */
    UserRole selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询已逻辑删除的角色关联
     */
    UserRole selectDeletedByUserId(@Param("userId") Long userId);
}
