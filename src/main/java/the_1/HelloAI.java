package the_1;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class HelloAI {
    public static void main(String[] args) {
        // 第1行：创建模型（MiniMax OpenAI兼容接口）
        ChatModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("MINIMAX_API_KEY"))  // 👈 MiniMax API Key
            .baseUrl("https://api.minimax.chat/v1")     // 👈 MiniMax OpenAI兼容端点
            .modelName("MiniMax-M2.5")
            .temperature(0.7)
            .build();

        // 第2-4行：发送对话
        String response = model.chat(
            ChatRequest.builder()
                .messages(UserMessage.from("用一句话介绍Java的LangChain4j是什么"))
                .build()
        ).aiMessage().text();

        // 第5行：打印结果
        System.out.println(response);
    }
}
