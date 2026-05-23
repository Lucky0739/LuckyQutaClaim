package com.lucky.seckill.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.utils.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 全局异常处理器
 * 统一处理权限认证相关的异常
 */
@RestControllerAdvice
@Slf4j
@Order(1)
public class SaTokenExceptionHandler {

    /**
     * 处理未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Response<?> handleNotLoginException(NotLoginException e) {
        log.warn("==> 用户未登录: {}", e.getMessage());
        return Response.fail(ResponseCodeEnum.USER_NOT_LOGIN);
    }

    /**
     * 处理角色验证失败异常
     */
    @ExceptionHandler(NotRoleException.class)
    public Response<?> handleNotRoleException(NotRoleException e) {
        log.warn("==> 用户没有权限，缺少角色: {}", e.getRole());
        return Response.fail(ResponseCodeEnum.USER_PERMISSION_DENIED);
    }
}
