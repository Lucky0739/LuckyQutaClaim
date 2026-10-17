package com.lucky.quota.common.model.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis 逻辑过期缓存包装：data 为业务数据，expireTime 为逻辑过期时间戳（毫秒）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisData<T> {

    private T data;

    /**
     * 逻辑过期时间戳（毫秒）
     */
    private Long expireTime;
}
