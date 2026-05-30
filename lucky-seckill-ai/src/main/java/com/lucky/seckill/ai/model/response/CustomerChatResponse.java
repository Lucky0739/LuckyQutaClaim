package com.lucky.seckill.ai.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客服聊天响应")
public class CustomerChatResponse {

    @Schema(description = "回复内容")
    private String content;

    @Schema(description = "会话ID")
    private String chatId;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "错误信息")
    private String errorMessage;
}