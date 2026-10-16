package com.lucky.quota.common.config;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sa-Token 全局配置（各微服务共享 Redis 中的 token）
 */
@Configuration
public class SaTokenWebConfig implements WebMvcConfigurer {

    @Value("${sa-token.ignore-paths:/user/login,/user/register,/user/code/send,/captcha/**}")
    private String ignorePaths;

    @Value("${sa-token.token-name:Authorization}")
    private String tokenName;

    @Value("${sa-token.token-prefix:Bearer}")
    private String tokenPrefix;

    @Bean
    @Primary
    public SaTokenConfig saTokenConfig() {
        SaTokenConfig config = new SaTokenConfig();
        config.setTokenName(tokenName);
        config.setTokenPrefix(tokenPrefix);
        config.setIsReadHeader(true);
        config.setIsReadCookie(false);
        config.setTimeout(2592000);
        config.setActiveTimeout(-1);
        config.setIsConcurrent(true);
        config.setIsShare(true);
        config.setTokenStyle("uuid");
        config.setIsLog(true);
        config.setRightNowCreateTokenSession(false);
        return config;
    }

    @Bean
    public SaInterceptor saInterceptor() {
        return new SaInterceptor(handle -> {
            String requestPath = SaHolder.getRequest().getRequestPath();

            List<String> excludePaths = Arrays.stream(ignorePaths.split(","))
                    .map(String::trim)
                    .filter(path -> !path.isEmpty())
                    .collect(Collectors.toList());

            for (String path : excludePaths) {
                if (path.endsWith("/**")) {
                    String prefix = path.substring(0, path.length() - 3);
                    if (requestPath.startsWith(prefix)) {
                        return;
                    }
                } else if (requestPath.startsWith(path) || requestPath.equals(path)) {
                    return;
                }
            }

            StpUtil.checkLogin();
        });
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(saInterceptor())
                .addPathPatterns("/**");
    }
}
