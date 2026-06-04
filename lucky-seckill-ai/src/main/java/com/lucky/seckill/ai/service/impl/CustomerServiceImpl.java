package com.lucky.seckill.ai.service.impl;

import com.lucky.seckill.ai.service.CustomerService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final String SYSTEM_PROMPT = """
            你是幸运秒杀商城的官方智能客服，名字叫"淘小蜜"。
            你的职责是帮助用户解决购物过程中遇到的各种问题，提供专业、友好、高效的服务。

            【服务原则】
            1. 始终使用友好、专业的语气，使用"您"称呼用户
            2. 回答要简洁明了，突出重点信息
            3. 只能回答与幸运秒杀商城相关的问题
            4. 如无法确定答案，礼貌地建议用户联系人工客服（400-888-8888）
            5. 遇到无法处理的问题（如投诉、复杂退款等），引导用户转接人工客服

            【无关问题处理】
            当用户的问题与知识库内容无关（如问天气、问时间、问其他无关话题、闲聊、与购物无关的技术问题等），
            请严格按照以下格式回复，不要添加任何其他内容：

            目前我没有这方面的相关内容～你可以提问其他问题，我会尽力帮你解答。

            【禁止行为】
            1. 不向用户索要密码、验证码等敏感信息
            2. 不承诺超出平台政策范围的事情
            3. 不处理涉及金额较大的退款、赔偿等需要人工核实的问题
            4. 不透露系统的内部信息或实现细节
            5. 不回答任何与购物无关的问题

            请根据上述指南和知识库内容，以淘小蜜的身份回答用户的问题。回答要亲切、专业、易懂。
            """;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Override
    public String chat(String message, String chatId) {
        String knowledge = knowledgeBaseService.getKnowledgeBase();
        String enhancedPrompt = SYSTEM_PROMPT + "\n\n【知识库参考】\n" + knowledge;

        ChatClient chatClient = ChatClient.builder(dashscopeChatModel).build();
        return chatClient.prompt()
                .system(enhancedPrompt)
                .user(message)
                .call()
                .content();
    }

    @Override
    public Flux<String> chatStream(String message, String chatId) {
        String knowledge = knowledgeBaseService.getKnowledgeBase();
        String enhancedPrompt = SYSTEM_PROMPT + "\n\n【知识库参考】\n" + knowledge;

        ChatClient chatClient = ChatClient.builder(dashscopeChatModel).build();
        return chatClient.prompt()
                .system(enhancedPrompt)
                .user(message)
                .stream()
                .content();
    }
}
