package com.lucky.quota.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.lucky.quota.common.aspect.ApiOperationLog;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.user.model.dto.UserAddressAddRequest;
import com.lucky.quota.user.model.dto.UserAddressUpdateRequest;
import com.lucky.quota.user.model.entity.UserAddress;
import com.lucky.quota.user.model.vo.UserAddressVO;
import com.lucky.quota.user.service.UserAddressService;
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
     * 新增领取地址
     */
    @SaCheckLogin
    @PostMapping("/add")
    @ApiOperationLog(description = "新增领取地址")
    public Response<?> addAddress(@Validated @RequestBody UserAddressAddRequest request) {
        return userAddressService.addAddress(request);
    }

    /**
     * 修改领取地址
     */
    @SaCheckLogin
    @PostMapping("/update")
    @ApiOperationLog(description = "修改领取地址")
    public Response<?> updateAddress(@Validated @RequestBody UserAddressUpdateRequest request) {
        return userAddressService.updateAddress(request);
    }

    /**
     * 删除领取地址
     */
    @SaCheckLogin
    @PostMapping("/delete/{userAddressId}")
    @ApiOperationLog(description = "删除领取地址")
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

    /**
     * 根据地址ID查询领取地址（申领等场景使用）
     */
    @SaCheckLogin
    @GetMapping("/get/{userAddressId}")
    @ApiOperationLog(description = "根据地址ID查询领取地址")
    public Response<UserAddress> getAddressById(@PathVariable Long userAddressId) {
        return userAddressService.getAddressById(userAddressId);
    }
}
