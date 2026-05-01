package the_4;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 持久化记忆聊天示例
 * 使用RedisChatMemoryStore实现聊天记忆的持久化存储
 */
public class PersistentMemoryChat {

    public static void main(String[] args) {
        // 1. 初始化模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        // 2. 创建Redis存储
        RedisChatMemoryStore redisStore = new RedisChatMemoryStore("localhost", 6379);

        // 3. 创建带持久化的大脑（记忆）
        ChatMemory memory = MessageWindowChatMemory.builder()
                .id("user_001")
                .maxMessages(20)
                .chatMemoryStore(redisStore)  // 添加Redis存储
                .build();

        // 4. 第一轮对话
        memory.add(UserMessage.from("我叫张三"));
        ChatResponse r1 = model.chat(
                ChatRequest.builder()
                        .messages(memory.messages())
                        .build()
        );
        memory.add(r1.aiMessage());
        System.out.println("AI：" + r1.aiMessage().text());

        // 5. 第二轮对话（AI会记得张三）
        memory.add(UserMessage.from("我叫什么名字？"));
        ChatResponse r2 = model.chat(
                ChatRequest.builder()
                        .messages(memory.messages())
                        .build()
        );
        memory.add(r2.aiMessage());
        System.out.println("AI：" + r2.aiMessage().text());

        // 6. 关闭连接
        redisStore.close();
    }
}
