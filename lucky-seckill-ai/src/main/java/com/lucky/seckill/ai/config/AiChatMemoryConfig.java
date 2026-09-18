package com.lucky.seckill.ai.config;

import com.lucky.seckill.ai.memory.RedisChatMemoryRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * AI 客服配置：会话记忆（滑动窗口 + Redis 持久化）+ RAG 向量检索（PostgreSQL + pgvector）
 */
@Configuration
public class AiChatMemoryConfig {

    @Value("${lucky-seckill.ai.chat-memory.max-messages:20}")
    private int maxMessages;

    @Value("${lucky-seckill.ai.chat-memory.expire-hours:24}")
    private long expireHours;

    // ==================== pgvector 配置 ====================

    @Value("${lucky-seckill.ai.vector-store.pgvector.host}")
    private String pgHost;

    @Value("${lucky-seckill.ai.vector-store.pgvector.port}")
    private int pgPort;

    @Value("${lucky-seckill.ai.vector-store.pgvector.database}")
    private String pgDatabase;

    @Value("${lucky-seckill.ai.vector-store.pgvector.username}")
    private String pgUsername;

    @Value("${lucky-seckill.ai.vector-store.pgvector.password}")
    private String pgPassword;

    @Value("${lucky-seckill.ai.vector-store.pgvector.table-name}")
    private String pgTableName;

    @Value("${lucky-seckill.ai.vector-store.pgvector.dimensions}")
    private int pgDimensions;

    @Value("${lucky-seckill.ai.vector-store.pgvector.index-type}")
    private String pgIndexType;

    @Value("${lucky-seckill.ai.vector-store.pgvector.distance-type}")
    private String pgDistanceType;

    // ==================== Bean 定义 ====================

    @Bean
    public ChatMemory chatMemory(RedisChatMemoryRepository redisChatMemoryRepository) {
        redisChatMemoryRepository.setExpireHours(expireHours);
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }

    @Bean(destroyMethod = "close")
    public DataSource pgVectorDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgDatabase);
        ds.setUsername(pgUsername);
        ds.setPassword(pgPassword);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(5);
        ds.setMinimumIdle(1);
        ds.setPoolName("PgVectorHikariPool");
        return ds;
    }

    @Bean
    public JdbcTemplate pgVectorJdbcTemplate(DataSource pgVectorDataSource) {
        return new JdbcTemplate(pgVectorDataSource);
    }

    @Bean
    public VectorStore vectorStore(JdbcTemplate pgVectorJdbcTemplate, EmbeddingModel embeddingModel) {
        PgVectorStore.PgDistanceType distanceType = PgVectorStore.PgDistanceType.COSINE_DISTANCE;
        if ("EUCLIDEAN_DISTANCE".equalsIgnoreCase(pgDistanceType)) {
            distanceType = PgVectorStore.PgDistanceType.EUCLIDEAN_DISTANCE;
        } else if ("NEGATIVE_INNER_PRODUCT".equalsIgnoreCase(pgDistanceType)) {
            distanceType = PgVectorStore.PgDistanceType.NEGATIVE_INNER_PRODUCT;
        }

        PgVectorStore.PgIndexType indexType = PgVectorStore.PgIndexType.HNSW;
        if ("IVFFLAT".equalsIgnoreCase(pgIndexType)) {
            indexType = PgVectorStore.PgIndexType.IVFFLAT;
        } else if ("NONE".equalsIgnoreCase(pgIndexType)) {
            indexType = PgVectorStore.PgIndexType.NONE;
        }

        return PgVectorStore.builder(pgVectorJdbcTemplate, embeddingModel)
                .vectorTableName(pgTableName)
                .dimensions(pgDimensions)
                .distanceType(distanceType)
                .indexType(indexType)
                .initializeSchema(true)
                // Spring AI 1.0.0 PgVectorStore 暂不支持 similarityThreshold；
                // 相似度过滤升级 AI 依赖版本后可加，当前靠 Prompt 自检兜底
                .build();
    }

    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore).build();
    }

    @Bean
    public ChatClient customerChatClient(ChatModel dashscopeChatModel,
                                          ChatMemory chatMemory,
                                          QuestionAnswerAdvisor questionAnswerAdvisor) {
        return ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        questionAnswerAdvisor
                )
                .build();
    }
}
