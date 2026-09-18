package com.lucky.seckill.ai.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.lucky.seckill.ai.memory.ChatMemoryManager;
import com.lucky.seckill.ai.memory.RedisChatMemoryRepository;
import com.lucky.seckill.ai.model.response.CustomerChatResponse;
import com.lucky.seckill.ai.service.CustomerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
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
            6. 结合本会话的历史对话理解用户追问（如「那退款呢」「刚才说的订单」），保持上下文连贯
            7. 如果用户使用「刚才那个」「前面提到的」「那个东西」等指代词，而对话历史中无法确定具体指代对象，
               请礼貌地请用户重新描述，例如「抱歉，我可能记不清刚才的具体内容了，能麻烦您再描述一下吗？」
            8. 回答时优先参考系统检索到的知识库内容，如果检索内容不足以回答问题，如实告知

            【无关问题处理】
            当用户的问题与知识库内容无关（如问天气、问时间、问其他无关话题、闲聊、与购物无关的技术问题等），
            请严格按照以下格式回复，不要添加任何其他内容：

            目前我没有这方面的相关内容～你可以提问其他问题，我会尽力帮你解答。

            【上下文断裂处理】
            用户有时会用「刚才那个」「前面说的」等指代词。当无法从对话历史中确定具体指代对象时，
            礼貌地请用户重新描述，不要猜测。例如：「抱歉，我可能记不太清楚了，能麻烦您再描述一下吗？」

            【禁止行为】
            1. 不向用户索要密码、验证码等敏感信息
            2. 不承诺超出平台政策范围的事情
            3. 不处理涉及金额较大的退款、赔偿等需要人工核实的问题
            4. 不透露系统的内部信息或实现细节
            5. 不回答任何与购物无关的问题

            请根据上述指南和系统检索到的知识库内容，以淘小蜜的身份回答用户的问题。回答要亲切、专业、易懂。
            """;

    @Resource
    private ChatClient customerChatClient;

    @Resource
    private RedisChatMemoryRepository chatMemoryRepository;

    @Resource
    private ChatMemoryManager chatMemoryManager;

    @Override
    public CustomerChatResponse chat(String message, String chatId) {
        String conversationId = resolveChatId(chatId);
        String systemPrompt = buildPromptWithSummary(conversationId);

        String content = customerChatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        // 异步检查是否需要压缩旧消息
        triggerSummaryCheck(conversationId);

        return CustomerChatResponse.builder()
                .chatId(conversationId)
                .content(content)
                .success(true)
                .build();
    }

    @Override
    public Flux<String> chatStream(String message, String chatId) {
        String conversationId = resolveChatId(chatId);
        String systemPrompt = buildPromptWithSummary(conversationId);

        Flux<String> result = customerChatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();

        // 流式响应完成后异步触发摘要检查
        result = result.doOnComplete(() -> triggerSummaryCheck(conversationId));

        return result;
    }

    /**
     * 把 Redis 中的历史摘要拼入系统提示词（解决长对话上下文断裂）。
     */
    private String buildPromptWithSummary(String conversationId) {
        String summary = chatMemoryManager.getSummary(conversationId);
        if (StrUtil.isBlank(summary)) {
            return SYSTEM_PROMPT;
        }
        return SYSTEM_PROMPT
                + "\n\n【历史对话摘要】\n"
                + summary
                + "\n\n"
                + "摘要覆盖的对话内容已从窗口中移除。回答时请结合摘要和当前对话来理解上下文。";
    }

    /**
     * 异步触发摘要检查（不阻塞当前请求）。
     */
    private void triggerSummaryCheck(String conversationId) {
        try {
            List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);
            chatMemoryManager.summarizeIfNeeded(conversationId, messages);
        } catch (Exception e) {
            log.warn("==> 摘要检查触发失败，不影响主流程, conversationId={}", conversationId, e);
        }
    }

    @Override
    public String resolveChatId(String chatId) {
        if (StrUtil.isNotBlank(chatId)) {
            return chatId.trim();
        }
        return IdUtil.simpleUUID();
    }
}
