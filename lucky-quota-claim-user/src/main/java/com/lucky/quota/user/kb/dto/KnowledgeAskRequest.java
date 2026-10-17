package com.lucky.quota.user.kb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeAskRequest {

    @NotBlank(message = "问题不能为空")
    private String question;

    private String chatId;
}
