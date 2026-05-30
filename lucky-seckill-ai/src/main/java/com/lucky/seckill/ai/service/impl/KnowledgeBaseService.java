package com.lucky.seckill.ai.service.impl;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    private String knowledgeBase;

    @PostConstruct
    public void init() {
        loadKnowledgeBase();
    }

    private void loadKnowledgeBase() {
        StringBuilder knowledge = new StringBuilder();

        try {
            ClassPathResource resource1 = new ClassPathResource("document/秒杀商城客服知识库.md");
            ClassPathResource resource2 = new ClassPathResource("document/秒杀商城常见问题FAQ.md");

            try (BufferedReader reader1 = new BufferedReader(
                    new InputStreamReader(resource1.getInputStream(), StandardCharsets.UTF_8))) {
                knowledge.append(reader1.lines().collect(Collectors.joining("\n")));
            }

            knowledge.append("\n\n");
            knowledge.append("=".repeat(50));
            knowledge.append("\n\n");

            try (BufferedReader reader2 = new BufferedReader(
                    new InputStreamReader(resource2.getInputStream(), StandardCharsets.UTF_8))) {
                knowledge.append(reader2.lines().collect(Collectors.joining("\n")));
            }

            this.knowledgeBase = knowledge.toString();
        } catch (Exception e) {
            this.knowledgeBase = "";
        }
    }

    public String getKnowledgeBase() {
        return knowledgeBase;
    }
}