package com.lucky.seckill.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置类
 * 配置自定义的编码器和解码器，支持泛型反序列化
 */
@Configuration
public class FeignConfig {

    /**
     * Feign 编码器配置
     * 使用项目中已有的 ObjectMapper
     */
    @Bean
    public Encoder feignEncoder(@Autowired(required = false) ObjectMapper objectMapper) {
        return new JacksonEncoder(objectMapper);
    }

    /**
     * Feign 解码器配置
     * 支持泛型类型反序列化
     * 使用项目中已有的 ObjectMapper
     */
    @Bean
    public Decoder feignDecoder(@Autowired(required = false) ObjectMapper objectMapper) {
        return new JacksonDecoder(objectMapper);
    }
}