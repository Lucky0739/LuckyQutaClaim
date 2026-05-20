package com.lucky.seckill.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.model.dto.UserAddressAddRequest;
import com.lucky.seckill.user.model.dto.UserAddressUpdateRequest;
import com.lucky.seckill.user.model.vo.UserAddressVO;
import com.lucky.seckill.user.service.UserAddressService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @SaCheckLogin
    @PostMapping("/update")
    @ApiOperationLog(description = "修改收货地址")
    public Response<?> updateAddress(@Validated @RequestBody UserAddressUpdateRequest request) {
        return userAddressService.updateAddress(request);
    }

    /**
     * 删除收货地址
     */
    @SaCheckLogin
    @PostMapping("/delete/{userAddressId}")
    @ApiOperationLog(description = "删除收货地址")
    public Response<?> deleteAddress(@PathVariable Long userAddressId) {
        return userAddressService.deleteAddress(userAddressId);
    }

    /**
     * 查询用户地址列表
     */
    @SaCheckLogin
    @GetMapping("/list")
    @ApiOperationLog(description = "查询用户地址列表")
    public Response<List<UserAddressVO>> listAddress() {
        return userAddressService.listAddress();
    }
}
