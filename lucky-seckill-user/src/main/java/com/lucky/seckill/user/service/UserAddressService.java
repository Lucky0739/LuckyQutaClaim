package com.lucky.seckill.user.service;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.model.dto.UserAddressAddRequest;
import com.lucky.seckill.user.model.dto.UserAddressUpdateRequest;
import com.lucky.seckill.user.model.entity.UserAddress;
import com.lucky.seckill.user.model.vo.UserAddressVO;

import java.util.List;

public interface UserAddressService {
    /**
     * 新增收货地址
     */
    Response<?> addAddress(UserAddressAddRequest request);

    /**
     * 修改收货地址
     */
    Response<?> updateAddress(UserAddressUpdateRequest request);

    /**
     * 删除收货地址
     */
    Response<?> deleteAddress(Long userAddressId);

    /**
     * 查询用户地址列表
     */
    Response<List<UserAddressVO>> listAddress();

    /**
     * 根据地址ID查询当前用户的收货地址
     */
    Response<UserAddress> getAddressById(Long userAddressId);
}
