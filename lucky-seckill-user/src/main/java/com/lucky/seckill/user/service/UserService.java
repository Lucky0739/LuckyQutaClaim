package com.lucky.seckill.user.service;


import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.model.dto.*;
import com.lucky.seckill.user.model.vo.LoginUserRspVO;
import com.lucky.seckill.user.model.vo.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author: 犬小哈
 * @Date: 2026/4/10 18:09
 * @Version: v1.0.0
 * @Description: 用户业务
 **/
public interface UserService {

    /**
     * 用户注册
     * @param registerUserReqVO
     * @return
     */
    Response<?> register(RegisterUserRequest registerUserReqVO);

    /**
     * 用户登录
     * @param loginUserReqVO
     * @return
     */
    Response<LoginUserRspVO> login(LoginUserRequest loginUserReqVO);

    /**
     * 发送验证码
     * @param sendVerifyCodeReqVO
     * @return
     */
    Response<Boolean> sendVerifyCode(SendVerifyCodeRequest sendVerifyCodeReqVO);

    /**
     * 退出登录
     * @return
     */
    Response<?> logout();

    /**
     * 完善用户资料
     * @param updateUserProfileRequest
     * @return
     */
    Response<?> updateUserProfile(UpdateUserProfileRequest updateUserProfileRequest);

    /**
     * 上传用户头像
     * @param file
     * @return
     */
    Response<UploadPictureResult> uploadAvatar(MultipartFile file);


    /**
     * 管理员修改用户资料
     * @param request
     * @return
     */
    Response<?> adminUpdateUserProfile(AdminUpdateUserProfileRequest request);

    /**
     * 查看用户资料（优先从 Redis 缓存获取）
     * @return
     */
    Response<UserProfileVO> getUserProfile();

    /**
     * 禁用/启用用户
     * @param userId
     * @param status
     * @return
     */
    Response<?> updateUserStatus(Long userId, Integer status);

    /**
     * 管理员上传用户头像
     */
    Response<UploadPictureResult> adminUploadAvatar(Long userId, MultipartFile file);
}

