package com.lucky.seckill.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.model.dto.LoginUserRequest;
import com.lucky.seckill.user.model.dto.RegisterUserRequest;
import com.lucky.seckill.user.model.dto.SendVerifyCodeRequest;
import com.lucky.seckill.user.model.vo.LoginUserRspVO;
import com.lucky.seckill.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: Lucky
 * @Date: 2026/4/10 18:49
 * @Version: v1.0.0
 * @Description: 用户接口
 **/
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @ApiOperationLog(description = "用户注册")
    public Response<?> register(@Validated @RequestBody RegisterUserRequest registerUserRequest) {
        return userService.register(registerUserRequest);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @ApiOperationLog(description = "用户登录")
    public Response<LoginUserRspVO> login(@Validated @RequestBody LoginUserRequest loginUserRequest) {
        return userService.login(loginUserRequest);
    }

    /**
     * 测试是否真的登录了
     */
    @GetMapping("/test/isLogin")
    public Response<?> isLogin() {
        // 调用 SaToken 提供的方法，判断当前请求是否已登录
        boolean isLogin = StpUtil.isLogin();

        if (isLogin) {
            // 已登录，获取当前登录的用户 ID
            long loginId = StpUtil.getLoginIdAsLong();
            log.info("==> 当前已登录, userId: {}", loginId);
            return Response.success("当前登录用户 ID: " + loginId);
        } else {
            // 未登录
            return Response.success("当前未登录");
        }
    }
    /**
     * 发送验证码
     */
    @PostMapping("/code/send")
    @ApiOperationLog(description = "发送验证码")
    public Response<?> sendVerifyCode(@Validated @RequestBody SendVerifyCodeRequest sendVerifyCodeRequest) {
        return userService.sendVerifyCode(sendVerifyCodeRequest);
    }
    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @ApiOperationLog(description = "退出登录")
    public Response<?> logout() {
        return userService.logout();
    }
}

