package com.lucky.seckill.ai.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucky.seckill.common.constant.RedisKeyConstants;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的会话记忆仓储，支持多实例网关共享上下文
 */
@Repository
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final TypeReference<List<ChatMemoryRecord>> RECORD_LIST_TYPE = new TypeReference<>() {
    };

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    private long expireHours = 24L;

    public void setExpireHours(long expireHours) {
        this.expireHours = expireHours;
    }

    @Override
    public List<String> findConversationIds() {
        return Collections.emptyList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return List.of();
        }
        String json = stringRedisTemplate.opsForValue().get(RedisKeyConstants.aiChatMemoryKey(conversationId));
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<ChatMemoryRecord> records = objectMapper.readValue(json, RECORD_LIST_TYPE);
            return toMessages(records);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        String key = RedisKeyConstants.aiChatMemoryKey(conversationId);
        if (messages == null || messages.isEmpty()) {
            stringRedisTemplate.delete(key);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(toRecords(messages));
            stringRedisTemplate.opsForValue().set(key, json, expireHours, TimeUnit.HOURS);
        } catch (Exception ignored) {
            // 序列化失败时不阻断主流程
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            stringRedisTemplate.delete(RedisKeyConstants.aiChatMemoryKey(conversationId));
        }
    }

    private List<ChatMemoryRecord> toRecords(List<Message> messages) {
        List<ChatMemoryRecord> records = new ArrayList<>();
        for (Message message : messages) {
            if (message == null || message.getMessageType() == null) {
                continue;
            }
            MessageType type = message.getMessageType();
            if (type == MessageType.TOOL) {
                continue;
            }
            String text = message.getText();
            if (!StringUtils.hasText(text)) {
                continue;
            }
            records.add(new ChatMemoryRecord(type.name(), text));
        }
        return records;
    }

    private List<Message> toMessages(List<ChatMemoryRecord> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        List<Message> messages = new ArrayList<>();
        for (ChatMemoryRecord record : records) {
            if (record == null || !StringUtils.hasText(record.getType()) || !StringUtils.hasText(record.getContent())) {
                continue;
            }
            MessageType type;
            try {
                type = MessageType.valueOf(record.getType());
            } catch (IllegalArgumentException ex) {
                continue;
            }
            Message message = switch (type) {
                case USER -> new UserMessage(record.getContent());
                case ASSISTANT -> new AssistantMessage(record.getContent());
                case SYSTEM -> new SystemMessage(record.getContent());
                default -> null;
            };
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }
}
