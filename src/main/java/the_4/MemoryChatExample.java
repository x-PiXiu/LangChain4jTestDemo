package the_4;

import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class MemoryChatExample {

    public static void main(String[] args) {
        // 1. 初始化模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        // 2. 创建记忆存储（只保留最近20条消息）
        ChatMemory memory = MessageWindowChatMemory.builder()
                .id("user_001")  // 对话ID，不同用户用不同ID
                .maxMessages(20)  // 最多保留20条消息
                .build();

        // 3. 发送第1条消息
        memory.add(UserMessage.from("我叫张三"));
        ChatResponse r1 = model.chat(
                ChatRequest.builder()
                        .messages(memory.messages())
                        .build()
        );
        memory.add(r1.aiMessage());
        System.out.println("AI：" + r1.aiMessage().text());

        // 4. 发送第2条消息（AI会自动记得"张三"）
        memory.add(UserMessage.from("我叫什么名字？"));
        ChatResponse r2 = model.chat(
                ChatRequest.builder()
                        .messages(memory.messages())
                        .build()
        );
        memory.add(r2.aiMessage());
        System.out.println("AI：" + r2.aiMessage().text());
        // 输出：您叫张三！
    }
}
