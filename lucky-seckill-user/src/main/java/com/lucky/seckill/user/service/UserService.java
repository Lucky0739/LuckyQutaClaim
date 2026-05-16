package com.lucky.seckill.user.service;


import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.model.dto.LoginUserRequest;
import com.lucky.seckill.user.model.dto.RegisterUserRequest;
import com.lucky.seckill.user.model.dto.SendVerifyCodeRequest;
import com.lucky.seckill.user.model.vo.LoginUserRspVO;

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
    Response<?> sendVerifyCode(SendVerifyCodeRequest sendVerifyCodeReqVO);

    /**
     * 退出登录
     * @return
     */
    Response<?> logout();
}

