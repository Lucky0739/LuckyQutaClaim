package com.lucky.seckill.user.feign;

import com.lucky.seckill.common.utils.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "lucky-seckill-user", path = "/user")
public interface UserFeignClient {

    @GetMapping("/get/{id}")
    Object getUserById(@PathVariable("id") Long id);

    @GetMapping("/info")
    Object getUserInfo();

    @PostMapping("/role/assign")
    void assignMerchantRole(@RequestParam("userId") Long userId, @RequestParam("roleId") Long roleId);

    @GetMapping("/roles/{userId}")
    Response<List<String>> getUserRoles(@PathVariable("userId") Long userId);
}