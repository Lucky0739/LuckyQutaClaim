package com.lucky.seckill.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.model.dto.*;
import com.lucky.seckill.user.model.vo.LoginUserRspVO;
import com.lucky.seckill.user.model.vo.UserProfileVO;
import com.lucky.seckill.user.service.UserService;
import com.lucky.seckill.user.service.impl.UserServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @SaIgnore
    @PostMapping("/register")
    public Response<?> register(@Validated @RequestBody RegisterUserRequest registerUserRequest) {
        return userService.register(registerUserRequest);
    }

    /**
     * 用户登录
     */
    @SaIgnore
    @PostMapping("/login")
    public Response<LoginUserRspVO> login(@Validated @RequestBody LoginUserRequest loginUserRequest) {
        return userService.login(loginUserRequest);
    }


    /**
     * 发送验证码
     */
    @SaIgnore
    @PostMapping("/code/send")
    public Response<Boolean> sendVerifyCode(@Validated @RequestBody SendVerifyCodeRequest sendVerifyCodeRequest) {
        return userService.sendVerifyCode(sendVerifyCodeRequest);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Response<?> logout() {
        return userService.logout();
    }

    /**
     * 完善用户资料（本人操作）
     */
    @SaCheckLogin
    @PostMapping("/profile/update")
    @ApiOperationLog(description = "完善用户资料")
    public Response<?> updateUserProfile(@Validated @RequestBody UpdateUserProfileRequest updateUserProfileRequest) {
        return userService.updateUserProfile(updateUserProfileRequest);
    }

    /**
     * 上传用户头像（本人操作）
     */
    @PostMapping("/avatar/upload")
    @ApiOperationLog(description = "上传用户头像")
    public Response<UploadPictureResult> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return userService.uploadAvatar(file);
    }

    /**
     * 管理员为用户上传头像
     */
    @SaCheckRole("admin")
    @PostMapping("/admin/avatar/upload/{userId}")
    @ApiOperationLog(description = "管理员为用户上传头像")
    public Response<UploadPictureResult> adminUploadAvatar(@PathVariable("userId") Long userId,
                                                           @RequestParam("file") MultipartFile file) {
        log.info("==> 管理员上传用户头像, adminId: {}, targetUserId: {}",
                StpUtil.getLoginId(), userId);
        return userService.adminUploadAvatar(userId, file);
    }

    /**
     * 管理员修改用户资料
     */
    @SaCheckRole("admin")
    @PostMapping("/admin/profile/update")
    @ApiOperationLog(description = "管理员修改用户资料")
    public Response<?> adminUpdateUserProfile(@Validated @RequestBody AdminUpdateUserProfileRequest request) {
        log.info("==> 管理员修改用户资料, adminId: {}, targetUserId: {}",
                StpUtil.getLoginId(), request.getUserId());
        return userService.adminUpdateUserProfile(request);
    }

    /**
     * 查看用户资料（优先从 Redis 缓存获取）
     */
    @SaCheckLogin
    @GetMapping("/profile")
    public Response<UserProfileVO> getUserProfile() {
        return userService.getUserProfile();
    }

    /**
     * 管理员禁用/启用用户
     */
    @SaCheckRole("admin")
    @PostMapping("/status/update")
    public Response<?> updateUserStatus(@RequestParam("userId") Long userId, @RequestParam("status") Integer status) {
        log.info("==> [DEBUG] 进入管理员接口");
        log.info("==> [DEBUG] 当前登录用户ID: {}", StpUtil.getLoginId());
        log.info("==> [DEBUG] 当前用户角色列表: {}", StpUtil.getRoleList());
        log.info("==> [DEBUG] 请求参数 - userId: {}, status: {}", userId, status);

        return userService.updateUserStatus(userId, status);
    }
}