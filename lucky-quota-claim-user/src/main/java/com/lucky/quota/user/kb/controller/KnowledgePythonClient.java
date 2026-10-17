package com.lucky.quota.user.kb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.lucky.quota.user.kb.dto.KnowledgeAskRequest;
import com.lucky.quota.user.kb.dto.KnowledgeAskResponse;
import com.lucky.quota.user.kb.dto.PythonIndexRequest;
import com.lucky.quota.user.kb.dto.PythonIndexResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class KnowledgePythonClient {

    private final String baseUrl;
    private final RestClient restClient;
    private final RestClient indexClient;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public KnowledgePythonClient(
            @Value("${kb.python.base-url:http://127.0.0.1:8000}") String baseUrl,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.objectMapper = objectMapper;
        this.restClient = restClient(Duration.ofSeconds(2), Duration.ofSeconds(15));
        this.indexClient = restClient(Duration.ofSeconds(3), Duration.ofSeconds(120));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        log.info("kb python base-url={}", this.baseUrl);
    }

    public PythonIndexResponse index(PythonIndexRequest request) {
        try {
            PythonIndexResponse response = indexClient.post()
                    .uri("/internal/kb/index")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(PythonIndexResponse.class);
            if (response == null) {
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "知识库索引没有返回结果");
            }
            return response;
        } catch (ResourceAccessException ex) {
            throw pythonDown();
        } catch (RestClientException ex) {
            log.error("调用知识库索引失败", ex);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "文档清洗或向量入库失败");
        }
    }

    public void delete(Long documentId) {
        try {
            restClient.post()
                    .uri("/internal/kb/delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("documentId", documentId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException ex) {
            throw pythonDown();
        } catch (RestClientException ex) {
            log.warn("删除向量失败 documentId={}: {}", documentId, ex.getMessage());
        }
    }

    public KnowledgeAskResponse ask(KnowledgeAskRequest request) {
        try {
            KnowledgeAskResponse response = restClient.post()
                    .uri("/internal/kb/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(KnowledgeAskResponse.class);
            if (response == null || response.getAnswer() == null) {
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "知识库没有返回答案");
            }
            return response;
        } catch (ResourceAccessException ex) {
            throw pythonDown();
        } catch (RestClientException ex) {
            log.error("调用知识库问答失败", ex);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "知识库暂时不可用，请稍后重试");
        }
    }

    public void stream(KnowledgeAskRequest request, SseEmitter emitter) {
        try {
            String body = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/internal/kb/stream"))
                    .timeout(Duration.ofMinutes(2))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "知识库流式接口返回 " + response.statusCode());
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String event = "message";
                StringBuilder data = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("event:")) {
                        event = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        if (!data.isEmpty()) {
                            data.append('\n');
                        }
                        data.append(line.substring(5).trim());
                    } else if (line.isEmpty()) {
                        if (!data.isEmpty()) {
                            emitter.send(SseEmitter.event().name(event).data(data.toString()));
                        }
                        event = "message";
                        data.setLength(0);
                    }
                }
                if (!data.isEmpty()) {
                    emitter.send(SseEmitter.event().name(event).data(data.toString()));
                }
            }
        } catch (BizException ex) {
            throw ex;
        } catch (java.net.ConnectException ex) {
            throw pythonDown();
        } catch (Exception ex) {
            log.error("转发知识库 SSE 失败", ex);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "知识库流式对话失败");
        }
    }

    private RestClient restClient(Duration connect, Duration read) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connect);
        factory.setReadTimeout(read);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    private BizException pythonDown() {
        return new BizException(ResponseCodeEnum.SYSTEM_ERROR, "知识库服务未启动，请先运行 lucky-quota-claim-kb（端口 8000）");
    }
}
