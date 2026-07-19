package com.lucky.seckill.user.feign;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.model.entity.UserAddress;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(contextId = "userAddressFeignClient", name = "lucky-seckill-user", path = "/user/address")
public interface UserAddressFeignClient {

    @GetMapping("/get/{userAddressId}")
    Response<UserAddress> getAddressById(@PathVariable("userAddressId") Long userAddressId);
}
