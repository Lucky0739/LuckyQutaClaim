package com.lucky.quota.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.pnvs")
public class AliyunSmsConfig {

    private String accessKeyId;

    private String accessKeySecret;

    private String regionId = "cn-hangzhou";

    private String endpoint = "dypnsapi.aliyuncs.com";

    private String signName;

    private String templateCode;

    private String schemeName;

    private String countryCode = "86";

    private Long validTime = 300L;

    private Long interval = 60L;

    private Long codeLength = 6L;

    private Long codeType = 1L;

    private Long duplicatePolicy;

    private Boolean returnVerifyCode = false;
}
