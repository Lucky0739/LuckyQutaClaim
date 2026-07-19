package com.lucky.seckill.user.service;

/**
 * PNVS 短信认证服务。
 */
public interface SmsAuthService {

    /**
     * 发送短信验证码。
     *
     * @param mobile     手机号
     * @param verifyCode 验证码
     */
    void sendVerifyCode(String mobile, String verifyCode);
}
