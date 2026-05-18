package com.lucky.seckill.common.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@Slf4j
@ConfigurationProperties(prefix = "cos.client")
public class CosClientConfig {

    private String secretId;
    private String secretKey;
    private String region;
    private String bucketName;
    private String host;

    @PostConstruct
    public void init() {
        log.info("=== CosClientConfig 初始化 ===");
        log.info("secretId: {}", secretId);
        log.info("region: {}", region);
        log.info("bucketName: {}", bucketName);
        log.info("host: {}", host);
    }

    @Bean
    public COSClient cosClient() {
        if (secretId == null || secretId.isEmpty()) {
            log.error("COS配置错误: secretId 不能为空");
            throw new IllegalArgumentException("COS配置错误: secretId 不能为空");
        }
        if (secretKey == null || secretKey.isEmpty()) {
            log.error("COS配置错误: secretKey 不能为空");
            throw new IllegalArgumentException("COS配置错误: secretKey 不能为空");
        }
        if (region == null || region.isEmpty()) {
            log.error("COS配置错误: region 不能为空");
            throw new IllegalArgumentException("COS配置错误: region 不能为空");
        }

        log.info("===== COS 客户端初始化 =====");
        log.info("secretId: {}", secretId);
        log.info("region: {}", region);
        log.info("bucketName: {}", bucketName);

        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        return new COSClient(cred, clientConfig);
    }
}