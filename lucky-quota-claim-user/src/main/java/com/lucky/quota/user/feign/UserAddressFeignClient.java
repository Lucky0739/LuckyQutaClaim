package com.lucky.quota.user.feign;

import com.lucky.quota.common.utils.Response;
import com.lucky.quota.user.model.entity.UserAddress;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(contextId = "userAddressFeignClient", name = "lucky-quota-claim-user", path = "/user/address")
public interface UserAddressFeignClient {

    @GetMapping("/get/{userAddressId}")
    Response<UserAddress> getAddressById(@PathVariable("userAddressId") Long userAddressId);
}
