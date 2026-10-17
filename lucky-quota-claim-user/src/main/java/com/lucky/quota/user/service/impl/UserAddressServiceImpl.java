package com.lucky.quota.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.user.mapper.UserAddressMapper;
import com.lucky.quota.user.model.dto.UserAddressAddRequest;
import com.lucky.quota.user.model.dto.UserAddressUpdateRequest;
import com.lucky.quota.user.model.entity.UserAddress;
import com.lucky.quota.user.model.vo.UserAddressVO;
import com.lucky.quota.user.service.UserAddressService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> updateAddress(UserAddressUpdateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 1. 校验地址是否存在且属于当前用户
        UserAddress existAddress = userAddressMapper.selectByIdAndUserId(request.getId(), userId);
        if (existAddress == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "地址不存在或无权操作");
        }

        // 2. 如果设置为默认地址，先取消该用户其他地址的默认状态
        if (Objects.equals(request.getIsDefault(), 1)) {
            userAddressMapper.cancelDefaultByUserId(userId);
        }

        // 3. 构建更新实体
        UserAddress address = new UserAddress();
        address.setId(request.getId());
        address.setReceiver(request.getReceiver());
        address.setMobile(request.getMobile());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setArea(request.getArea());
        address.setDetailAddress(request.getDetailAddress());
        address.setIsDefault(request.getIsDefault());
        address.setUpdateTime(LocalDateTime.now());

        // 4. 执行更新
        userAddressMapper.updateByPrimaryKeySelective(address);

        log.info("==> 用户修改地址成功, userId: {}, addressId: {}", userId, request.getId());
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deleteAddress(Long userAddressId) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 1. 校验地址是否存在且属于当前用户
        UserAddress existAddress = userAddressMapper.selectByIdAndUserId(userAddressId, userId);
        if (existAddress == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "地址不存在或无权操作");
        }

        // 2. 逻辑删除地址
        userAddressMapper.deleteByIdAndUserId(userAddressId, userId);

        log.info("==> 用户删除地址成功, userId: {}, addressId: {}", userId, userAddressId);
        return Response.success();
    }

    @Override
    public Response<List<UserAddressVO>> listAddress() {
        Long userId = StpUtil.getLoginIdAsLong();

        // 1. 查询用户地址列表
        List<UserAddress> addressList = userAddressMapper.selectListByUserId(userId);

        // 2. 转换为VO对象
        List<UserAddressVO> voList = addressList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Response.success(voList);
    }

    @Override
    public Response<UserAddress> getAddressById(Long userAddressId) {
        if (userAddressId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "地址ID不能为空");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        UserAddress address = userAddressMapper.selectByIdAndUserId(userAddressId, userId);
        if (address == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "地址不存在或无权操作");
        }
        return Response.success(address);
    }

    /**
     * 实体转VO
     */
    private UserAddressVO convertToVO(UserAddress address) {
        UserAddressVO userAddressVO = new UserAddressVO();
        BeanUtils.copyProperties(address, userAddressVO);
        return userAddressVO;
    }
}
