package com.lucky.quota.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 登录接口返回的 token 是裸 uuid。Sa-Token 要求请求头为 {@code Authorization: Bearer <token>}。
 * 压测脚本如果只把提取到的 token 放进 Authorization，会被当成未登录，库存扣减不会执行。
 * 这里在进入鉴权前补上前缀；前端已经带了 Bearer 的请求保持原样。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BearerTokenNormalizeFilter extends OncePerRequestFilter {

    static final String TOKEN_HEADER = "Authorization";
    static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || token.isBlank() || hasBearerPrefix(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(new BearerRequestWrapper(request, token.trim()), response);
    }

    static boolean hasBearerPrefix(String token) {
        return token.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length());
    }

    private static final class BearerRequestWrapper extends HttpServletRequestWrapper {

        private final String authorization;

        private BearerRequestWrapper(HttpServletRequest request, String rawToken) {
            super(request);
            this.authorization = BEARER_PREFIX + rawToken;
        }

        @Override
        public String getHeader(String name) {
            if (TOKEN_HEADER.equalsIgnoreCase(name)) {
                return authorization;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (TOKEN_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(authorization));
            }
            return super.getHeaders(name);
        }
    }
}
