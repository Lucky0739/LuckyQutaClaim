package com.lucky.seckill.common.exception;

import lombok.Getter;

/**
 * 不可重试异常：确定性失败，重试多少次都不可能成功。
 * <p>
 * 典型场景：商品不存在、数据格式非法、业务校验不通过（如活动已结束）。
 * MQ 消费者捕获后应直接 ACK 并触发告警，不应投递死信队列浪费资源。
 */
@Getter
public class NonRetryableException extends RuntimeException {

    /** 关联的业务标识（如 orderNo），用于日志追踪 */
    private final String businessKey;

    public NonRetryableException(String message) {
        super(message);
        this.businessKey = null;
    }

    public NonRetryableException(String message, String businessKey) {
        super(message);
        this.businessKey = businessKey;
    }

    public NonRetryableException(String message, Throwable cause) {
        super(message, cause);
        this.businessKey = null;
    }

    public NonRetryableException(String message, String businessKey, Throwable cause) {
        super(message, cause);
        this.businessKey = businessKey;
    }
}
