package com.lucky.quota.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.lucky.quota.common.aspect.ApiOperationLog;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.model.file.UploadPictureResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.user.model.dto.*;
import com.lucky.quota.user.model.vo.AdminUserListVO;
import com.lucky.quota.user.model.vo.LoginUserRspVO;
import com.lucky.quota.user.model.vo.UserProfileVO;
import cn.dev33.satoken.stp.StpInterface;
import com.lucky.quota.user.service.UserService;
import com.lucky.quota.user.service.impl.UserServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @Resource
    private StpInterface stpInterface;

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
     * 按用户 ID 查询资料（本人或管理员，供发放单位等微服务 Feign 调用）
     */
    @SaCheckLogin
    @GetMapping("/get/{id}")
    public Response<UserProfileVO> getUserById(@PathVariable("id") Long id) {
        return userService.getUserById(id);
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

    /**
     * 管理员分配商户角色
     * @param userId
     * @param roleId
     * @return
     */
    @PostMapping("/role/assign")
    public Response<?> assignMerchantRole(@RequestParam("userId") Long userId, @RequestParam("roleId") Long roleId) {
        log.info("==> 收到分配角色请求, userId: {}, roleId: {}", userId, roleId);
        return userService.assignMerchantRole(userId, roleId);
    }

    /**
     * 获取用户角色列表（供其他服务调用）
     */
    @GetMapping("/roles/{userId}")
    public Response<List<String>> getUserRoles(@PathVariable("userId") Long userId) {
        List<String> roles = stpInterface.getRoleList(userId, StpUtil.getLoginType());
        log.debug("==> 获取用户角色列表, userId: {}, roles: {}", userId, roles);
        return Response.success(roles);
    }

    /**
     * 管理员分页查询用户列表
     */
    @SaCheckRole("admin")
    @GetMapping("/admin/list")
    @ApiOperationLog(description = "管理员查询用户列表")
    public Response<PageResult<AdminUserListVO>> getUserListByAdmin(AdminUserQueryRequest request) {
        log.info("==> 管理员查询用户列表, adminId: {}", StpUtil.getLoginId());
        return userService.getUserListByAdmin(request);
    }

    /**
     * 修改密码
     */
    @SaCheckLogin
    @PostMapping("/password/change")
    @ApiOperationLog(description = "修改密码")
    public Response<?> changePassword(@Validated @RequestBody ChangePasswordRequest request) {
        log.info("==> 用户修改密码, userId: {}", StpUtil.getLoginId());
        return userService.changePassword(request);
    }

    /**
     * 用户注销账号（本人操作，需密码 + 短信验证码）
     */
    @SaCheckLogin
    @PostMapping("/account/cancel")
    @ApiOperationLog(description = "用户注销账号")
    public Response<?> cancelAccount(@Validated @RequestBody CancelAccountRequest request) {
        log.info("==> 用户注销账号, userId: {}", StpUtil.getLoginId());
        return userService.cancelAccount(request);
    }

    /**
     * 管理员删除用户（逻辑删除）
     */
    @SaCheckRole("admin")
    @PostMapping("/admin/delete")
    @ApiOperationLog(description = "管理员删除用户")
    public Response<?> adminDeleteUser(@RequestParam("userId") Long userId) {
        log.info("==> 管理员删除用户, adminId: {}, targetUserId: {}", StpUtil.getLoginId(), userId);
        return userService.adminDeleteUser(userId);
    }
}