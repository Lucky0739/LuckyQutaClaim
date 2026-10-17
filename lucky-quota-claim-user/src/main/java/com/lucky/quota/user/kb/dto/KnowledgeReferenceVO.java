package com.lucky.quota.user.kb.dto;

import lombok.Data;

@Data
public class KnowledgeReferenceVO {

    private Long documentId;
    private String title;
    private String domain;
    private String topic;
    private String snippet;
    private Integer score;
}
