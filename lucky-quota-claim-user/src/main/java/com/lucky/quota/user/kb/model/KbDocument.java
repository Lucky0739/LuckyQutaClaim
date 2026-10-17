package com.lucky.quota.user.kb.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("kb_document")
public class KbDocument {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String documentName;
    private String originalFileName;
    private Integer fileType;
    private Long fileSize;
    private String objectName;
    private String knowledgeScopeCode;
    private String businessCategory;
    private Integer parseStatus;
    private Integer indexStatus;
    private String parseErrorMsg;
    private Integer chunkCount;
    private LocalDateTime createTime;
    private LocalDateTime editTime;
    private Integer status;
}
