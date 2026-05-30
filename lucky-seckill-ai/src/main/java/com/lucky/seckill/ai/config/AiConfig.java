package com.lucky.seckill.ai.config;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Resource
    private ChatModel dashscopeChatModel;

    public String generateResponse(String userMessage) {
        String systemPrompt = """
                你是幸运秒杀商城的官方客服，你的名字是小幸运。
                请用友好、专业的语气回答用户的问题。
                如果遇到无法回答的问题，请礼貌地告知用户并建议联系人工客服。
                请使用中文回复。
                
                常见问题参考：
                1. 关于订单问题：请用户提供订单号，我们会协助查询
                2. 关于商品问题：可以提供商品链接或商品ID
                3. 关于支付问题：请检查支付方式是否正确，或联系支付平台
                4. 关于配送问题：请提供订单号查询物流信息
                """;

        PromptTemplate promptTemplate = new PromptTemplate("""
                {systemPrompt}
                
                用户问题：{userMessage}
                
                请给出友好、专业的回复：
                """);

        promptTemplate.add("systemPrompt", systemPrompt);
        promptTemplate.add("userMessage", userMessage);

        return dashscopeChatModel.call(promptTemplate.create()).getResult().getOutput().getText();
    }
}