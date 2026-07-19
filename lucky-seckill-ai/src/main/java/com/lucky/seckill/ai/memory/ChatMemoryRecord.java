package com.lucky.seckill.ai.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis 中持久化的单条对话记录（仅保留文本类消息）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemoryRecord {

    private String type;

    private String content;
}
