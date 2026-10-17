package com.lucky.quota.user.service;

import com.lucky.quota.common.utils.Response;
import com.lucky.quota.user.model.dto.UserAddressAddRequest;
import com.lucky.quota.user.model.dto.UserAddressUpdateRequest;
import com.lucky.quota.user.model.entity.UserAddress;
import com.lucky.quota.user.model.vo.UserAddressVO;

import java.util.List;

public interface UserAddressService {
    /**
     * 新增领取地址
     */
    Response<?> addAddress(UserAddressAddRequest request);

    /**
     * 修改领取地址
     */
    Response<?> updateAddress(UserAddressUpdateRequest request);

    /**
     * 删除领取地址
     */
    Response<?> deleteAddress(Long userAddressId);

    /**
     * 查询用户地址列表
     */
    Response<List<UserAddressVO>> listAddress();

    /**
     * 根据地址ID查询当前用户的领取地址
     */
    Response<UserAddress> getAddressById(Long userAddressId);
}
