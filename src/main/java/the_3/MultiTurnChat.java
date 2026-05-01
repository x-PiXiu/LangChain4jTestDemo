package the_3;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.ArrayList;
import java.util.List;

public class MultiTurnChat {

    private final ChatModel model;
    private final List<ChatMessage> conversationHistory;

    public MultiTurnChat(ChatModel model) {
        this.model = model;
        this.conversationHistory = new ArrayList<>();
        // 添加系统消息（只需添加一次）
        conversationHistory.add(SystemMessage.from("""
            你是一个专业的Java技术顾问。
            回答问题时优先给出代码示例。
            """));
    }

    public String chat(String userInput) {
        // 1. 把用户消息加入历史
        conversationHistory.add(UserMessage.from(userInput));

        // 2. 构建请求（包含完整历史）
        ChatRequest request = ChatRequest.builder()
                .messages(new ArrayList<>(conversationHistory))  // 复制一份，避免被修改
                .build();

        // 3. 发送请求
        ChatResponse response = model.chat(request);

        // 4. 把AI回复也加入历史
        conversationHistory.add(response.aiMessage());

        // 5. 返回AI回复文本
        return response.aiMessage().text();
    }

    public static void main(String[] args) {
        // 初始化模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        MultiTurnChat chat = new MultiTurnChat(model);

        // 第1轮
        System.out.println("用户：" + "什么是Spring Boot？");
        System.out.println("AI：" + chat.chat("什么是Spring Boot？"));

        System.out.println("---");

        // 第2轮（AI会记住第1轮的内容）
        System.out.println("用户：" + "它和Spring MVC什么关系？");
        System.out.println("AI：" + chat.chat("它和Spring MVC什么关系？"));

        System.out.println("---");

        // 第3轮
        System.out.println("用户：" + "给我一个Spring Boot的Hello World示例");
        System.out.println("AI：" + chat.chat("给我一个Spring Boot的Hello World示例"));
    }
}