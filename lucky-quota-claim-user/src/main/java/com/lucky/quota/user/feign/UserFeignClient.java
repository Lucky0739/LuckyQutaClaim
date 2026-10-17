package com.lucky.quota.user.feign;

import com.lucky.quota.common.utils.Response;
import com.lucky.quota.user.model.vo.UserProfileVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "lucky-quota-claim-user", path = "/user")
public interface UserFeignClient {

    @GetMapping("/get/{id}")
    Response<UserProfileVO> getUserById(@PathVariable("id") Long id);

    @PostMapping("/role/assign")
    void assignMerchantRole(@RequestParam("userId") Long userId, @RequestParam("roleId") Long roleId);

    @GetMapping("/roles/{userId}")
    Response<List<String>> getUserRoles(@PathVariable("userId") Long userId);
}