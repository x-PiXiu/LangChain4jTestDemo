package the_4;

import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 带记忆的智能客服系统
 *
 * 功能：
 * 1. 记住用户信息（姓名、历史问题）
 * 2. 支持多轮对话上下文
 * 3. 对话历史持久化到Redis
 */
public class MemoryCustomerService {

    private final ChatModel model;
    private final RedisChatMemoryStore store;

    public MemoryCustomerService(ChatModel model, RedisChatMemoryStore store) {
        this.model = model;
        this.store = store;
    }

    /**
     * 创建指定用户的对话记忆
     */
    public ChatMemory createMemory(String userId) {
        // 先从Redis加载历史
        List<ChatMessage> history = store.getMessages(userId);

        ChatMemory memory = MessageWindowChatMemory.builder()
                .id(userId)
                .maxMessages(30)  // 保留最近30条消息
                .chatMemoryStore(store)
                .build();

        // 预加载历史
        for (ChatMessage msg : history) {
            memory.add(msg);
        }

        return memory;
    }

    /**
     * 处理用户消息
     */
    public String handle(String userId, String userMessage) {
        // 1. 获取或创建记忆
        ChatMemory memory = createMemory(userId);

        // 2. 构建Prompt（带上下文提示）
        PromptTemplate template = PromptTemplate.from("""
            你是奶茶店智能客服"小貔貅"。

            服务须知：
            1. 亲切友好，像朋友一样交流
            2. 如果用户自我介绍（如告诉名字），记住并在后续称呼
            3. 如果用户问产品推荐，根据季节推荐适合的奶茶
            4. 回答尽量简短，不超过50字

            历史记忆：
            {{history}}

            当前对话：
            用户：{{input}}
            小貔貅：
            """);

        // 3. 获取历史消息作为上下文
        String history = memory.messages().stream()
                .map(msg -> msg instanceof UserMessage
                        ? "用户：" + ((UserMessage) msg).singleText()
                        : "小貔貅：" + ((AiMessage) msg).text())
                .collect(Collectors.joining("\n"));

        // 4. 渲染Prompt - 使用 apply() 而不是 render()
        String prompt = template.apply(Map.of(
                "history", history.isEmpty() ? "（首次对话）" : history,
                "input", userMessage
        )).text();

        // 5. 添加用户消息
        memory.add(UserMessage.from(userMessage));

        // 6. 发送请求
        ChatRequest request = ChatRequest.builder()
                .messages(
                        SystemMessage.from("你是一个奶茶店智能客服"),
                        UserMessage.from(prompt)
                )
                .build();
        ChatResponse response = model.chat(request);

        // 7. 保存AI回复
        memory.add(response.aiMessage());

        return response.aiMessage().text();
    }

    public static void main(String[] args) {
        // 1. 初始化
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        // 使用JedisPool（推荐方式）
        RedisChatMemoryStore store = new RedisChatMemoryStore("localhost", 6379);
        MemoryCustomerService service = new MemoryCustomerService(model, store);

        String userId = "customer_001";

        // 2. 第1轮：用户自我介绍
        System.out.println("=== 第1轮 ===");
        System.out.println("用户：你好，我叫小明");
        String r1 = service.handle(userId, "你好，我叫小明");
        System.out.println("AI：" + r1);

        // 3. 第2轮：问推荐
        System.out.println("\n=== 第2轮 ===");
        System.out.println("用户：推荐一款奶茶给我");
        String r2 = service.handle(userId, "推荐一款奶茶给我");
        System.out.println("AI：" + r2);

        // 4. 第3轮：继续对话（AI应该记得小明）
        System.out.println("\n=== 第3轮 ===");
        System.out.println("用户：我刚才说叫什么名字？");
        String r3 = service.handle(userId, "我刚才说叫什么名字？");
        System.out.println("AI：" + r3);

        // 5. 第4轮：程序重启后（模拟）
        System.out.println("\n=== 模拟重启后 ===");
        MemoryCustomerService service2 = new MemoryCustomerService(model, store);
        System.out.println("用户：我的订单到哪了？");
        String r4 = service2.handle(userId, "我的订单到哪了？");
        System.out.println("AI：" + r4);
        // AI应该说"小明"或相关内容，说明记忆恢复成功

        // 6. 关闭连接
        store.close();
    }
}