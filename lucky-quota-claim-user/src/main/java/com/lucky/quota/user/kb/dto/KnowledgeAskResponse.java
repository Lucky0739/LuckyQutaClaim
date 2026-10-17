package com.lucky.quota.user.kb.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class KnowledgeAskResponse {

    private String executor;
    private Double confidence;
    private String reason;
    private List<String> missingInfo = new ArrayList<>();
    private String answer;
    private List<String> clarificationQuestions = new ArrayList<>();
    private Map<String, Object> funnel = new HashMap<>();
    private Map<String, Object> shadowFunnel = new HashMap<>();
    private List<KnowledgeReferenceVO> references = new ArrayList<>();
}
