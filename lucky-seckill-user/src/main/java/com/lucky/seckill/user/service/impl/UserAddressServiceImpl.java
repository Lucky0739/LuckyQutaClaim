package com.lucky.seckill.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.mapper.UserAddressMapper;
import com.lucky.seckill.user.model.dto.UserAddressAddRequest;
import com.lucky.seckill.user.model.entity.UserAddress;
import com.lucky.seckill.user.service.UserAddressService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class UserAddressServiceImpl implements UserAddressService {

    @Resource
    private UserAddressMapper userAddressMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> addAddress(UserAddressAddRequest request) {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 1. 如果设置为默认地址，先取消该用户其他地址的默认状态
        if (Objects.equals(request.getIsDefault(), 1)) {
            userAddressMapper.cancelDefaultByUserId(userId);
        }

        // 2. 构建地址实体
        UserAddress address = new UserAddress();
        address.setId(IdUtil.getSnowflakeNextId());
        address.setUserId(userId);
        address.setReceiver(request.getReceiver());
        address.setMobile(request.getMobile());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setArea(request.getArea());
        address.setDetailAddress(request.getDetailAddress());
        address.setIsDefault(request.getIsDefault() == null ? 0 : request.getIsDefault());
        address.setIsDelete(0);
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());

        // 3. 插入数据库
        userAddressMapper.insertSelective(address);

        log.info("==> 用户新增地址成功, userId: {}, addressId: {}", userId, address.getId());
        return Response.success();
    }
}
