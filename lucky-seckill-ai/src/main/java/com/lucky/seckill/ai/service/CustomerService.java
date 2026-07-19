package com.lucky.seckill.ai.service;

import com.lucky.seckill.ai.model.response.CustomerChatResponse;
import reactor.core.publisher.Flux;

public interface CustomerService {

    CustomerChatResponse chat(String message, String chatId);

    Flux<String> chatStream(String message, String chatId);

    /**
     * 解析或生成会话 ID，供流式接口写入响应头
     */
    String resolveChatId(String chatId);
}
