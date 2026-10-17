package com.lucky.quota.user.kb.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.lucky.quota.common.aspect.ApiOperationLog;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.user.kb.dto.KnowledgeAskRequest;
import com.lucky.quota.user.kb.dto.KnowledgeAskResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/kb")
public class KnowledgeAskController {

    @Resource
    private KnowledgePythonClient knowledgePythonClient;

    @SaIgnore
    @PostMapping("/ask")
    @ApiOperationLog(description = "知识库同步问答")
    public Response<KnowledgeAskResponse> ask(@Valid @RequestBody KnowledgeAskRequest request) {
        KnowledgeAskResponse data = knowledgePythonClient.ask(request);
        Object jaccard = data.getShadowFunnel() == null ? null : data.getShadowFunnel().get("jaccard");
        log.info("kb ask executor={} shadowJaccard={}", data.getExecutor(), jaccard);
        return Response.success(data);
    }

    @SaIgnore
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperationLog(description = "知识库流式问答")
    public SseEmitter stream(@Valid @RequestBody KnowledgeAskRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);
        Thread.startVirtualThread(() -> {
            try {
                knowledgePythonClient.stream(request, emitter);
                emitter.complete();
            } catch (Exception ex) {
                log.warn("kb stream failed: {}", ex.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error").data(
                            "{\"message\":\"" + escape(ex.getMessage()) + "\"}"));
                    emitter.complete();
                } catch (Exception ignored) {
                    emitter.completeWithError(ex);
                }
            }
        });
        return emitter;
    }

    private static String escape(String message) {
        if (message == null) {
            return "知识库暂时不可用";
        }
        return message.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
