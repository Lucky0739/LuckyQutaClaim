package com.lucky.quota.user.kb.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.lucky.quota.common.aspect.ApiOperationLog;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.user.kb.dto.KbDocumentVO;
import com.lucky.quota.user.kb.dto.KbReindexResultVO;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/kb/document")
public class KnowledgeDocumentController {

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @SaCheckRole("admin")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperationLog(description = "管理员上传知识库文档")
    public Response<KbDocumentVO> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String documentName,
            @RequestParam(required = false) String knowledgeScopeCode,
            @RequestParam(required = false) String businessCategory) {
        return Response.success(knowledgeDocumentService.upload(
                file, documentName, knowledgeScopeCode, businessCategory));
    }

    @SaCheckRole("admin")
    @GetMapping("/list")
    @ApiOperationLog(description = "管理员查询知识库文档")
    public Response<List<KbDocumentVO>> list() {
        return Response.success(knowledgeDocumentService.list());
    }

    @SaCheckRole("admin")
    @PostMapping("/reindex")
    @ApiOperationLog(description = "管理员重建知识库向量")
    public Response<KbReindexResultVO> reindex() {
        return Response.success(knowledgeDocumentService.reindex());
    }

    @SaCheckRole("admin")
    @DeleteMapping("/{documentId}")
    @ApiOperationLog(description = "管理员删除知识库文档")
    public Response<?> delete(@PathVariable Long documentId) {
        knowledgeDocumentService.delete(documentId);
        return Response.success();
    }
}
