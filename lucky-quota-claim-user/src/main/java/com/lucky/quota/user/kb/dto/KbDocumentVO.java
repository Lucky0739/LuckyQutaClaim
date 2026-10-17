package com.lucky.quota.user.kb.dto;

import com.lucky.quota.user.kb.model.KbDocument;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KbDocumentVO {
    private String id;
    private String documentName;
    private String originalFileName;
    private Integer fileType;
    private Long fileSize;
    private String knowledgeScopeCode;
    private String businessCategory;
    private Integer parseStatus;
    private Integer indexStatus;
    private String parseErrorMsg;
    private Integer chunkCount;
    private LocalDateTime createTime;

    public static KbDocumentVO from(KbDocument document) {
        KbDocumentVO vo = new KbDocumentVO();
        vo.setId(String.valueOf(document.getId()));
        vo.setDocumentName(document.getDocumentName());
        vo.setOriginalFileName(document.getOriginalFileName());
        vo.setFileType(document.getFileType());
        vo.setFileSize(document.getFileSize());
        vo.setKnowledgeScopeCode(document.getKnowledgeScopeCode());
        vo.setBusinessCategory(document.getBusinessCategory());
        vo.setParseStatus(document.getParseStatus());
        vo.setIndexStatus(document.getIndexStatus());
        vo.setParseErrorMsg(document.getParseErrorMsg());
        vo.setChunkCount(document.getChunkCount());
        vo.setCreateTime(document.getCreateTime());
        return vo;
    }
}
