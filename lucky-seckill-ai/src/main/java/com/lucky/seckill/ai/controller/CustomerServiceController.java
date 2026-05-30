package com.lucky.seckill.ai.controller;

import com.lucky.seckill.ai.service.CustomerService;
import com.lucky.seckill.common.utils.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai/customer")
@Tag(name = "官方客服聊天接口")
public class CustomerServiceController {

    @Resource
    private CustomerService customerService;

    @GetMapping("/chat")
    @Operation(summary = "同步聊天", description = "官方客服同步聊天接口")
    public Response<String> chat(@RequestParam String message, @RequestParam(required = false) String chatId) {
        String response = customerService.chat(message, chatId);
        return Response.success(response);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式聊天", description = "官方客服流式聊天接口（SSE）")
    public Flux<String> chatStream(@RequestParam String message, @RequestParam(required = false) String chatId) {
        return customerService.chatStream(message, chatId);
    }

    @GetMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式聊天", description = "官方客服SSE流式聊天接口")
    public Flux<ServerSentEvent<String>> chatSSE(@RequestParam String message, @RequestParam(required = false) String chatId) {
        return customerService.chatStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }
}