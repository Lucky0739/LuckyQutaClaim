package com.lucky.seckill.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.model.dto.UserAddressAddRequest;
import com.lucky.seckill.user.service.UserAddressService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/address")
@Slf4j
public class UserAddressController {
    @Resource
    private UserAddressService userAddressService;

    /**
     * 新增收货地址
     */
    @SaCheckLogin
    @PostMapping("/add")
    @ApiOperationLog(description = "新增收货地址")
    public Response<?> addAddress(@Validated @RequestBody UserAddressAddRequest request) {
        return userAddressService.addAddress(request);
    }

    /**
     * 修改收货地址
     */

}
