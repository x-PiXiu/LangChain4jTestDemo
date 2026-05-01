package the_7;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多用户客服系统
 *
 * 关键设计：每个用户有独立的Memory实例
 */
public class MultiUserCustomerService {

    // 用户ID → Memory 实例（生产环境用Redis分布式存储）
    private final Map<String, MessageWindowChatMemory> userMemories = new ConcurrentHashMap<>();

    private final ChatModel model;

    public MultiUserCustomerService(ChatModel model) {
        this.model = model;
    }

    private static final String SYSTEM_PROMPT = """
            你是一个电商平台的智能客服。
            回答与订单、物流、退换货相关的问题。
            """;

    /**
     * 获取某用户的专属Memory
     * 如果不存在则创建（懒加载），并注入系统消息
     */
    private MessageWindowChatMemory getMemory(String userId) {
        return userMemories.computeIfAbsent(userId, id -> {
            var memory = MessageWindowChatMemory.builder()
                    .id(id)
                    .maxMessages(30)
                    .build();
            memory.add(SystemMessage.from(SYSTEM_PROMPT));
            return memory;
        });
    }

    /**
     * 处理用户消息
     */
    public String handle(String userId, String userMessage) {
        // 获取该用户的Memory（已包含系统消息）
        MessageWindowChatMemory memory = getMemory(userId);

        // 创建该用户的专属Chain
        ConversationalChain chain = ConversationalChain.builder()
                .chatModel(model)
                .chatMemory(memory)
                .build();

        // 执行对话
        return chain.execute(userMessage);
    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        MultiUserCustomerService service = new MultiUserCustomerService(model);

        // 用户A的对话
        System.out.println("【用户A】");
        System.out.println("客服：" + service.handle("user_A", "我的订单多久能到？"));
        System.out.println("客服：" + service.handle("user_A", "订单号是123"));

        // 用户B的对话（完全独立）
        System.out.println("\n【用户B】");
        System.out.println("客服：" + service.handle("user_B", "我要退货"));
    }
}