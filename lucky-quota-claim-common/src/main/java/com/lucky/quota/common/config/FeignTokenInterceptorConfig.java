package com.lucky.quota.common.config;

import cn.dev33.satoken.stp.StpUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 调用时透传当前请求的 Token，保证微服务间鉴权一致
 */
@Slf4j
@Configuration
public class FeignTokenInterceptorConfig {

    @Value("${sa-token.token-name:Authorization}")
    private String tokenName;

    @Value("${sa-token.token-prefix:Bearer}")
    private String tokenPrefix;

    @Bean
    public RequestInterceptor feignTokenInterceptor() {
        return template -> {
            log.info("==> Feign 调用拦截器触发，URL: {}", template.url());
            propagateAuthorizationHeader(template);
        };
    }

    private void propagateAuthorizationHeader(RequestTemplate template) {
        try {
            String tokenValue = StpUtil.getTokenValue();
            log.info("==> 从 StpUtil 获取到的 Token: {}", tokenValue != null ? "***" : "null");
            
            if (StringUtils.hasText(tokenValue)) {
                // 格式: Bearer xxx 或直接 xxx（取决于配置）
                String tokenHeader = tokenPrefix + " " + tokenValue;
                template.header(tokenName, tokenHeader);
                template.header("satoken", tokenValue);
                log.info("==> 已设置 Token 到请求头");
                return;
            }
        } catch (Exception e) {
            log.warn("==> 从 StpUtil 获取 Token 失败: {}", e.getMessage());
        }

        // 方法2: 从 RequestContextHolder 获取（备用方案）
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("==> RequestContextHolder.getRequestAttributes() 返回 null");
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        if (request == null) {
            log.warn("==> attributes.getRequest() 返回 null");
            return;
        }

        // 尝试从多个可能的请求头获取 Token
        String[] headerNames = {tokenName, tokenName.toLowerCase(), "satoken", "Sa-Token"};
        for (String headerName : headerNames) {
            String token = request.getHeader(headerName);
            if (StringUtils.hasText(token)) {
                template.header(headerName, token);
                // 同时也设置标准的 Authorization 头
                if (!headerName.equals(tokenName)) {
                    template.header(tokenName, token);
                }
                log.info("==> 从请求头 {} 获取到 Token", headerName);
                return;
            }
        }

        log.warn("==> 未能获取到 Token，Feign 请求将不带认证信息");
    }
}
