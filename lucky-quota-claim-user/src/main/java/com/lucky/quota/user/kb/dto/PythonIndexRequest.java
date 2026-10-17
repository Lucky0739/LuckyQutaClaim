package com.lucky.quota.user.kb.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PythonIndexRequest {
    private Long documentId;
    private String documentName;
    private String knowledgeScopeCode;
    private String businessCategory;
    private String bucketName;
    private String objectName;
    private Integer fileType;
}
