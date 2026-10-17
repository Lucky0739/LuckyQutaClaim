package com.lucky.quota.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.user.model.entity.UserAddress;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserAddressMapper extends BaseMapper<UserAddress> {
    /**
     * 选择性插入地址记录
     */
    int insertSelective(UserAddress record);

    /**
     * 取消用户其他地址的默认状态
     */
    void cancelDefaultByUserId(@Param("userId") Long userId);

    /**
     * 选择性更新地址记录
     */
    int updateByPrimaryKeySelective(UserAddress record);

    /**
     * 根据 ID 和用户 ID 查询地址（确保只能操作自己的地址）
     */
    UserAddress selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 逻辑删除地址
     */
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 查询用户地址列表
     */
    List<UserAddress> selectListByUserId(@Param("userId") Long userId);
}
