package com.lucky.seckill.ai.service;

import reactor.core.publisher.Flux;

public interface CustomerService {

    String chat(String message, String chatId);

    Flux<String> chatStream(String message, String chatId);
}