package com.lucky.quota.common.exception;

import lombok.Getter;

/**
 * 可重试异常：瞬时性故障，延迟重试有望恢复。
 * <p>
 * 典型场景：数据库连接超时、乐观锁版本冲突、Redis 临时不可用、网络闪断。
 * MQ 消费者捕获后应投递到死信队列延迟重试。
 */
@Getter
public class RetryableException extends RuntimeException {

    /** 关联的业务标识（如 orderNo），用于日志追踪 */
    private final String businessKey;

    public RetryableException(String message) {
        super(message);
        this.businessKey = null;
    }

    public RetryableException(String message, String businessKey) {
        super(message);
        this.businessKey = businessKey;
    }

    public RetryableException(String message, Throwable cause) {
        super(message, cause);
        this.businessKey = null;
    }

    public RetryableException(String message, String businessKey, Throwable cause) {
        super(message, cause);
        this.businessKey = businessKey;
    }
}
