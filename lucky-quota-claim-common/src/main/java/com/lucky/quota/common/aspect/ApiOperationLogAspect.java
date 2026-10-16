
package com.lucky.quota.common.aspect;

import com.lucky.quota.common.utils.JsonUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class ApiOperationLogAspect {

    /** 以自定义 @ApiOperationLog 注解为切点，凡是添加 @ApiOperationLog 的方法，都会执行环绕中的代码 */
    @Pointcut("@annotation(com.lucky.quota.common.aspect.ApiOperationLog)")
    public void apiOperationLog() {}

    /**
     * 环绕
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("apiOperationLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 请求开始时间
        long startTime = System.currentTimeMillis();

        // 获取被请求的类和方法
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 请求入参
        Object[] args = joinPoint.getArgs();
        // 入参转 JSON 字符串
        String argsJsonStr = Arrays.stream(args).map(toJsonStr()).collect(Collectors.joining(", "));

        // 功能描述信息
        String description = getApiOperationLogDescription(joinPoint);

        // 打印请求相关参数
        log.info("====== 请求开始: [{}], 入参: {}, 请求类: {}, 请求方法: {} =================================== ",
                description, argsJsonStr, className, methodName);

        // 执行切点方法
        Object result = joinPoint.proceed();

        // 执行耗时
        long executionTime = System.currentTimeMillis() - startTime;

        // 打印出参等相关信息
        log.info("====== 请求结束: [{}], 耗时: {}ms, 出参: {} =================================== ",
                description, executionTime, safeToJsonStr(result));

        return result;
    }

    /**
     * 获取注解的描述信息
     * @param joinPoint
     * @return
     */
    private String getApiOperationLogDescription(ProceedingJoinPoint joinPoint) {
        // 1. 从 ProceedingJoinPoint 获取 MethodSignature
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 2. 使用 MethodSignature 获取当前被注解的 Method
        Method method = signature.getMethod();

        // 3. 从 Method 中提取 LogExecution 注解
        ApiOperationLog apiOperationLog = method.getAnnotation(ApiOperationLog.class);

        // 4. 从 LogExecution 注解中获取 description 属性
        return apiOperationLog.description();
    }

    /**
     * 转 JSON 字符串（跳过不可序列化的参数，如 MultipartFile）
     */
    private Function<Object, String> toJsonStr() {
        return this::safeToJsonStr;
    }

    private String safeToJsonStr(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof MultipartFile file) {
            return formatMultipartFile(file);
        }
        if (arg instanceof Part part) {
            return String.format("[Part: name=%s, size=%d, contentType=%s]",
                    part.getName(), part.getSize(), part.getContentType());
        }
        if (arg instanceof ServletRequest || arg instanceof ServletResponse
                || arg instanceof InputStream || arg instanceof OutputStream) {
            return "[" + arg.getClass().getSimpleName() + "]";
        }
        try {
            return JsonUtils.toJsonString(arg);
        } catch (Exception e) {
            log.debug("参数序列化失败, type: {}", arg.getClass().getName(), e);
            return "[" + arg.getClass().getSimpleName() + "]";
        }
    }

    private String formatMultipartFile(MultipartFile file) {
        return String.format("[MultipartFile: name=%s, originalFilename=%s, size=%d, contentType=%s]",
                file.getName(), file.getOriginalFilename(), file.getSize(), file.getContentType());
    }

}
