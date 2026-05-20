package com.lucky.seckill.user.mapper;

import com.lucky.seckill.user.model.entity.UserAddress;
import org.apache.ibatis.annotations.Param;

public interface UserAddressMapper {
    /**
     * 选择性插入地址记录
     */
    int insertSelective(UserAddress record);

    /**
     * 取消用户其他地址的默认状态
     */
    void cancelDefaultByUserId(@Param("userId") Long userId);
}
