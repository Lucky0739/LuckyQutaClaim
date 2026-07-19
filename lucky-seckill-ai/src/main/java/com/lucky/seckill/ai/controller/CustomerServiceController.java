package com.lucky.seckill.ai.controller;

import com.lucky.seckill.ai.model.response.CustomerChatResponse;
import com.lucky.seckill.ai.service.CustomerService;
import com.lucky.seckill.common.utils.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai/customer")
@Tag(name = "官方客服聊天接口")
public class CustomerServiceController {

    public static final String CHAT_ID_HEADER = "X-Chat-Id";

    private static final long SSE_TIMEOUT_MS = 300_000L;

    @Resource
    private CustomerService customerService;

    @GetMapping("/chat")
    @Operation(summary = "同步聊天", description = "官方客服同步聊天接口，返回 chatId 用于多轮对话")
    public Response<CustomerChatResponse> chat(@RequestParam String message,
                                               @RequestParam(required = false) String chatId) {
        return Response.success(customerService.chat(message, chatId));
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式聊天", description = "官方客服流式聊天接口（SSE），响应头 X-Chat-Id 为会话 ID")
    public SseEmitter chatStream(@RequestParam String message,
                                 @RequestParam(required = false) String chatId,
                                 HttpServletResponse response) {
        String conversationId = customerService.resolveChatId(chatId);
        response.setHeader(CHAT_ID_HEADER, conversationId);
        return subscribeToFlux(customerService.chatStream(message, conversationId));
    }

    @GetMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式聊天", description = "官方客服SSE流式聊天接口，响应头 X-Chat-Id 为会话 ID")
    public SseEmitter chatSSE(@RequestParam String message,
                              @RequestParam(required = false) String chatId,
                              HttpServletResponse response) {
        String conversationId = customerService.resolveChatId(chatId);
        response.setHeader(CHAT_ID_HEADER, conversationId);
        return subscribeToFlux(customerService.chatStream(message, conversationId));
    }

    private SseEmitter subscribeToFlux(Flux<String> flux) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        flux.subscribe(
                chunk -> {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::completeWithError,
                emitter::complete
        );
        emitter.onTimeout(emitter::complete);
        emitter.onError(ex -> emitter.complete());
        return emitter;
    }
}
