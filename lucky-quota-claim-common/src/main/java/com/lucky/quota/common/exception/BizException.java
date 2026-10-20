package com.lucky.quota.common.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * @author: Lucky
 * @date: 2023-08-15 9:52
 * @description: 业务异常
 **/
@Getter
@Setter
public class BizException extends RuntimeException {
    // 异常码
    private String errorCode;
    // 错误信息
    private String errorMessage;

    public BizException(BaseExceptionInterface baseExceptionInterface) {
        this.errorCode = baseExceptionInterface.getErrorCode();
        this.errorMessage = baseExceptionInterface.getErrorMessage();
    }
    /**
     * 自定义错误码和错误信息
     * @param errorMessage 错误信息
     */
    public BizException(BaseExceptionInterface baseExceptionInterface, String errorMessage) {
        this.errorCode = baseExceptionInterface.getErrorCode();
        this.errorMessage = errorMessage;
    }

}
