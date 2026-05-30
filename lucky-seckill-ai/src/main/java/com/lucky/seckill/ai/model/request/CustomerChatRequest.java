package com.lucky.seckill.ai.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "客服聊天请求")
public class CustomerChatRequest {

    @Schema(description = "用户消息", example = "我的订单什么时候发货？")
    @NotBlank(message = "消息内容不能为空")
    private String message;

    @Schema(description = "会话ID，用于维护对话上下文", example = "123456")
    private String chatId;

    @Schema(description = "用户ID", example = "10001")
    private Long userId;
}