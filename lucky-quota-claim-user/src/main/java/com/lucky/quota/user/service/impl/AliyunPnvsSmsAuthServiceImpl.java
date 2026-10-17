package com.lucky.quota.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dypnsapi.model.v20170525.SendSmsVerifyCodeRequest;
import com.aliyuncs.dypnsapi.model.v20170525.SendSmsVerifyCodeResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.lucky.quota.user.config.AliyunSmsConfig;
import com.lucky.quota.user.service.SmsAuthService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 阿里云 PNVS 短信认证服务实现。
 */
@Service
@Slf4j
public class AliyunPnvsSmsAuthServiceImpl implements SmsAuthService {

    private static final String PNVS_PRODUCT = "Dypnsapi";
    private static final String SUCCESS_CODE = "OK";

    @Resource
    private AliyunSmsConfig aliyunSmsConfig;

    private volatile IAcsClient client;

    @Override
    public void sendVerifyCode(String mobile, String verifyCode) {
        checkConfig();

        SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest();
        request.setPhoneNumber(mobile);
        request.setSignName(aliyunSmsConfig.getSignName());
        request.setTemplateCode(aliyunSmsConfig.getTemplateCode());
        request.setTemplateParam(buildTemplateParam(verifyCode));
        request.setCountryCode(aliyunSmsConfig.getCountryCode());
        request.setValidTime(aliyunSmsConfig.getValidTime());
        request.setInterval(aliyunSmsConfig.getInterval());
        request.setCodeLength(aliyunSmsConfig.getCodeLength());
        request.setCodeType(aliyunSmsConfig.getCodeType());
        request.setReturnVerifyCode(aliyunSmsConfig.getReturnVerifyCode());

        if (StrUtil.isNotBlank(aliyunSmsConfig.getSchemeName())) {
            request.setSchemeName(aliyunSmsConfig.getSchemeName());
        }
        if (aliyunSmsConfig.getDuplicatePolicy() != null) {
            request.setDuplicatePolicy(aliyunSmsConfig.getDuplicatePolicy());
        }

        try {
            SendSmsVerifyCodeResponse response = getClient().getAcsResponse(request);
            if (!Boolean.TRUE.equals(response.getSuccess()) || !SUCCESS_CODE.equalsIgnoreCase(response.getCode())) {
                log.warn("==> PNVS 短信验证码发送失败, mobile: {}, code: {}, message: {}",
                        mobile, response.getCode(), response.getMessage());
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "短信验证码发送失败");
            }
            SendSmsVerifyCodeResponse.Model model = response.getModel();
            String bizId = model == null ? null : model.getBizId();
            log.info("==> PNVS 短信验证码发送成功, mobile: {}, bizId: {}", mobile, bizId);
        } catch (ClientException e) {
            log.error("==> PNVS 短信验证码发送异常, mobile: {}, errorCode: {}, errorMessage: {}",
                    mobile, e.getErrCode(), e.getErrMsg(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "短信验证码发送失败");
        }
    }

    private IAcsClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    DefaultProfile.addEndpoint(aliyunSmsConfig.getRegionId(), PNVS_PRODUCT, aliyunSmsConfig.getEndpoint());
                    DefaultProfile profile = DefaultProfile.getProfile(
                            aliyunSmsConfig.getRegionId(),
                            aliyunSmsConfig.getAccessKeyId(),
                            aliyunSmsConfig.getAccessKeySecret()
                    );
                    client = new DefaultAcsClient(profile);
                    log.info("==> PNVS 短信认证客户端初始化成功, endpoint: {}", aliyunSmsConfig.getEndpoint());
                }
            }
        }
        return client;
    }

    private void checkConfig() {
        if (StrUtil.hasBlank(aliyunSmsConfig.getAccessKeyId(),
                aliyunSmsConfig.getAccessKeySecret(),
                aliyunSmsConfig.getSignName(),
                aliyunSmsConfig.getTemplateCode())) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "PNVS 短信认证配置不完整");
        }
    }

    private String buildTemplateParam(String verifyCode) {
        long validMinutes = Math.max(1L, aliyunSmsConfig.getValidTime() / 60L);
        return String.format("{\"code\":\"%s\",\"min\":\"%d\"}", verifyCode, validMinutes);
    }
}
