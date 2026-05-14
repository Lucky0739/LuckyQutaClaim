package com.lucky.seckill.user.controller;

import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.utils.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @date: 2026/4/5
 * @description:
 **/
@RestController
@Slf4j
public class TestController {

    /**
     * 测试公共返参 - 成功响应
     */
    @GetMapping("/test/response")
    @ApiOperationLog(description = "测试公共返参")
    public Response<String> testResponse(@RequestParam String name) {
        return Response.success("Hello, " + name + " !");
    }

    /**
     * 测试业务异常捕获
     */
    @GetMapping("/test/bizException")
    @ApiOperationLog(description = "测试业务异常捕获")
    public Response<String> testBizException() {
        // 模拟抛出业务异常
        throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID);
    }

    /**
     * 测试系统异常捕获
     */
    @GetMapping("/test/systemException")
    @ApiOperationLog(description = "测试系统异常捕获")
    public Response<String> testSystemException() {
        // 模拟抛出系统异常
        int i = 1 / 0;
        return Response.success("不会走到这里");
    }
}
