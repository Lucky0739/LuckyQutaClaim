package com.lucky.seckill.user.service;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.model.dto.UserAddressAddRequest;

public interface UserAddressService {
    /**
     * 新增收货地址
     */
    Response<?> addAddress(UserAddressAddRequest request);
}
