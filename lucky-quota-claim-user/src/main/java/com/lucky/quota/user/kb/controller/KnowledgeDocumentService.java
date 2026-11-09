package com.lucky.quota.user.kb.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.lucky.quota.user.kb.dto.KbDocumentVO;
import com.lucky.quota.user.kb.dto.KbReindexItemVO;
import com.lucky.quota.user.kb.dto.KbReindexResultVO;
import com.lucky.quota.user.kb.mapper.KbDocumentMapper;
import com.lucky.quota.user.kb.model.KbDocument;
import com.lucky.quota.user.kb.service.KnowledgeDocumentOfflineIndexService;
import com.lucky.quota.user.kb.storage.DocumentStorageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class KnowledgeDocumentService {

    private static final int PENDING = 1;
    private static final int PROCESSING = 2;
    private static final int SUCCESS = 3;
    private static final int FAILED = 4;
    private static final Map<String, Integer> FILE_TYPES = Map.of(
            "pdf", 1,
            "docx", 3,
            "txt", 4,
            "md", 5,
            "html", 6,
            "htm", 6
    );

    @Resource
    private KbDocumentMapper kbDocumentMapper;
    @Resource
    private DocumentStorageService documentStorageService;
    @Resource
    private KnowledgePythonClient knowledgePythonClient;
    @Resource
    private KnowledgeDocumentOfflineIndexService offlineIndexService;

    /**
     * 重建任务是否正在执行，避免并发触发导致重复写入向量
     */
    private final AtomicBoolean reindexRunning = new AtomicBoolean(false);

    public KbDocumentVO upload(
            MultipartFile file,
            String documentName,
            String knowledgeScopeCode,
            String businessCategory) {
        validate(file);
        String originalName = file.getOriginalFilename();
        int fileType = resolveFileType(originalName);
        long documentId = IdUtil.getSnowflakeNextId();
        String objectName = documentStorageService.upload(documentId, file);

        KbDocument document = KbDocument.builder()
                .id(documentId)
                .documentName(StrUtil.blankToDefault(documentName, originalName))
                .originalFileName(originalName)
                .fileType(fileType)
                .fileSize(file.getSize())
                .objectName(objectName)
                .knowledgeScopeCode(StrUtil.blankToDefault(knowledgeScopeCode, "default"))
                .businessCategory(StrUtil.blankToDefault(businessCategory, "general"))
                .parseStatus(PENDING)
                .indexStatus(PENDING)
                .chunkCount(0)
                .createTime(LocalDateTime.now())
                .editTime(LocalDateTime.now())
                .status(1)
                .build();
        kbDocumentMapper.insert(document);
        offlineIndexService.submit(documentId);
        log.info("知识库文档已上传并进入离线索引队列 documentId={}", documentId);
        return KbDocumentVO.from(document);
    }

    public List<KbDocumentVO> list() {
        return kbDocumentMapper.selectAllDocuments().stream().map(KbDocumentVO::from).toList();
    }

    public void delete(Long documentId) {
        KbDocument document = kbDocumentMapper.selectById(documentId);
        if (document == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "文档不存在");
        }
        knowledgePythonClient.delete(documentId);
        documentStorageService.delete(document.getObjectName());
        kbDocumentMapper.deleteById(documentId);
    }

    /**
     * 从 MinIO 原始文件重建全部有效文档的向量（Milvus 换库 / 升级后使用）
     */
    public KbReindexResultVO reindex() {
        if (!reindexRunning.compareAndSet(false, true)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "知识库向量重建正在进行中，请稍后再试");
        }
        try {
            List<KbDocument> documents = kbDocumentMapper.selectAllDocuments();
            List<KbReindexItemVO> items = new ArrayList<>(documents.size());
            int successCount = 0;
            int failedCount = 0;
            int skippedCount = 0;

            for (KbDocument snapshot : documents) {
                // 重建期间文档可能被删除，逐条复核
                KbDocument document = kbDocumentMapper.selectById(snapshot.getId());
                if (document == null) {
                    skippedCount++;
                    items.add(KbReindexItemVO.builder()
                            .documentId(String.valueOf(snapshot.getId()))
                            .documentName(snapshot.getDocumentName())
                            .skipped(true)
                            .errorMsg("文档已删除，跳过")
                            .build());
                    continue;
                }

                if (StrUtil.isBlank(document.getObjectName()) || document.getFileType() == null) {
                    markReindexFailed(document, "文档元数据不完整（objectName/fileType 为空）");
                    kbDocumentMapper.updateStatus(document);
                    failedCount++;
                    items.add(failedReindexItem(document));
                    continue;
                }

                document.setParseStatus(PENDING);
                document.setIndexStatus(PENDING);
                document.setParseErrorMsg(null);
                kbDocumentMapper.updateStatus(document);
                offlineIndexService.runOfflineIndex(document.getId());

                KbDocument after = kbDocumentMapper.selectById(document.getId());
                if (after == null) {
                    skippedCount++;
                    continue;
                }
                if (after.getIndexStatus() != null && after.getIndexStatus() == SUCCESS) {
                    successCount++;
                    items.add(KbReindexItemVO.builder()
                            .documentId(String.valueOf(after.getId()))
                            .documentName(after.getDocumentName())
                            .success(true)
                            .chunkCount(after.getChunkCount())
                            .build());
                } else {
                    failedCount++;
                    items.add(failedReindexItem(after));
                }
            }

            log.info("知识库向量重建完成 total={}, success={}, failed={}, skipped={}",
                    documents.size(), successCount, failedCount, skippedCount);
            return KbReindexResultVO.builder()
                    .total(documents.size())
                    .successCount(successCount)
                    .failedCount(failedCount)
                    .skippedCount(skippedCount)
                    .items(items)
                    .build();
        } finally {
            reindexRunning.set(false);
        }
    }

    private void markReindexFailed(KbDocument document, String message) {
        document.setParseStatus(FAILED);
        document.setIndexStatus(FAILED);
        document.setParseErrorMsg(message);
        document.setChunkCount(0);
    }

    private KbReindexItemVO failedReindexItem(KbDocument document) {
        return KbReindexItemVO.builder()
                .documentId(String.valueOf(document.getId()))
                .documentName(document.getDocumentName())
                .success(false)
                .chunkCount(0)
                .errorMsg(document.getParseErrorMsg())
                .build();
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "请选择文件");
        }
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "文件不能超过 20MB");
        }
        resolveFileType(file.getOriginalFilename());
    }

    private int resolveFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "无法识别文件类型");
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        Integer type = FILE_TYPES.get(ext);
        if (type == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "仅支持 PDF / DOCX / TXT / MD / HTML");
        }
        return type;
    }
}
