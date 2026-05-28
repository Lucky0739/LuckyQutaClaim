package com.lucky.seckill.pay.config;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AlipayConfig {

    @Value("${alipay.gateway-url}")
    private String gatewayUrl;

    @Value("${alipay.app-id}")
    private String appId;

    @Value("${alipay.merchant-private-key}")
    private String merchantPrivateKey;

    @Value("${alipay.alipay-public-key}")
    private String alipayPublicKey;

    @Value("${alipay.sign-type}")
    private String signType;

    @PostConstruct
    public void init() {
        // 设置支付宝参数
        Config options = new Config();
        options.protocol = "https";
        options.gatewayHost = gatewayUrl.replace("https://", "").replace("http://", "");
        options.signType = signType;
        options.appId = appId;
        options.merchantPrivateKey = merchantPrivateKey;
        options.alipayPublicKey = alipayPublicKey;

        Factory.setOptions(options);
        log.info("==> 支付宝SDK初始化完成，gatewayUrl: {}, appId: {}", gatewayUrl, appId);
    }
}
