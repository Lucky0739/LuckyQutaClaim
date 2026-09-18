package com.lucky.seckill.ai.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 知识库：启动时读取 Markdown 文档 → 按 ## 标题分块 → Embedding → 写入 PostgreSQL pgvector
 */
@Service
@Slf4j
public class KnowledgeBaseService {

    @Resource
    private VectorStore vectorStore;

    @Resource
    private JdbcTemplate pgVectorJdbcTemplate;

    @Value("${lucky-seckill.ai.vector-store.pgvector.table-name}")
    private String tableName;

    private int chunkCount;

    @PostConstruct
    public void init() {
        if (isAlreadyIndexed()) {
            chunkCount = countRows();
            log.info("==> 向量表 {} 已有 {} 条数据，跳过索引", tableName, chunkCount);
            return;
        }
        loadAndIndexDocuments();
    }

    private boolean isAlreadyIndexed() {
        try {
            Integer count = pgVectorJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tableName, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            // 表可能还不存在，PgVectorStore 初始化后会创建
            return false;
        }
    }

    private int countRows() {
        try {
            Integer count = pgVectorJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tableName, Integer.class);
            return count == null ? 0 : count;
        } catch (Exception e) {
            return 0;
        }
    }

    private static final int DASHSCOPE_EMBEDDING_BATCH_LIMIT = 10;

    /** TokenTextSplitter 兜底切块：最大 800 token，重叠 100 token，防长文档被稀释 */
    private static final int CHUNK_MAX_TOKENS = 800;
    private static final int CHUNK_OVERLAP_TOKENS = 100;

    private void loadAndIndexDocuments() {
        List<Document> documents = new ArrayList<>();

        documents.addAll(readAndChunk("document/秒杀商城客服知识库.md", "知识库"));
        documents.addAll(readAndChunk("document/秒杀商城常见问题FAQ.md", "FAQ"));

        if (documents.isEmpty()) {
            log.warn("==> 知识库文档为空，跳过向量索引");
            return;
        }

        // 按标题切块后，再按 token 数兜底切分（防 5000 字长段落变单个 chunk）
        // TokenTextSplitter(int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed,
        //                   int maxNumChunks, boolean keepSeparator)
        TokenTextSplitter splitter = new TokenTextSplitter(
                CHUNK_MAX_TOKENS,        // 800 token
                CHUNK_OVERLAP_TOKENS,    // 100 token 重叠
                50,                       // 最小嵌入长度
                10000,                    // 最大块数
                true);                    // 保留分隔符
        List<Document> finalChunks = splitter.apply(documents);
        log.info("==> TokenTextSplitter 兜底切分：{} 个原始块 → {} 个最终块",
                documents.size(), finalChunks.size());

        for (int i = 0; i < finalChunks.size(); i += DASHSCOPE_EMBEDDING_BATCH_LIMIT) {
            int end = Math.min(i + DASHSCOPE_EMBEDDING_BATCH_LIMIT, finalChunks.size());
            List<Document> batch = finalChunks.subList(i, end);
            vectorStore.add(batch);
            log.info("==> 批次 {}/{} 写入完成 ({} 条)", (i / DASHSCOPE_EMBEDDING_BATCH_LIMIT) + 1,
                    (finalChunks.size() + DASHSCOPE_EMBEDDING_BATCH_LIMIT - 1) / DASHSCOPE_EMBEDDING_BATCH_LIMIT,
                    batch.size());
        }

        chunkCount = finalChunks.size();
        log.info("==> RAG 知识库索引完成，共 {} 个文档块写入 pgvector", chunkCount);
    }

    private List<Document> readAndChunk(String classpath, String sourceName) {
        List<Document> chunks = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource(classpath);
            String fullText;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                fullText = reader.lines().collect(Collectors.joining("\n"));
            }

            String[] sections = fullText.split("\n(?=## )");
            for (String section : sections) {
                String trimmed = section.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String title = trimmed.lines().findFirst().orElse("").replaceFirst("^#+\\s*", "").trim();
                chunks.add(new Document(trimmed, java.util.Map.of(
                        "source", sourceName,
                        "title", title
                )));
            }

            if ("FAQ".equals(sourceName)) {
                chunks = splitFaqSection(chunks);
            }

            log.info("==> 读取并分块 {}: {} 个块", classpath, chunks.size());
        } catch (Exception e) {
            log.error("==> 读取知识库文档失败: {}", classpath, e);
        }
        return chunks;
    }

    private List<Document> splitFaqSection(List<Document> chunks) {
        List<Document> result = new ArrayList<>();
        for (Document doc : chunks) {
            String title = String.valueOf(doc.getMetadata().getOrDefault("title", ""));
            if (!"快速问答".equals(title)) {
                result.add(doc);
                continue;
            }
            String[] subSections = doc.getText().split("\n(?=### )");
            for (String sub : subSections) {
                String trimmed = sub.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String subTitle = trimmed.lines().findFirst().orElse("").replaceFirst("^#+\\s*", "").trim();
                result.add(new Document(trimmed, java.util.Map.of(
                        "source", "FAQ",
                        "title", subTitle
                )));
            }
        }
        return result;
    }

    public int getChunkCount() {
        return chunkCount;
    }
}
