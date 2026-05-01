package the_5;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class MiniMaxConfig {

    public static ChatModel create() {
        return OpenAiChatModel.builder()
                // API地址（OpenAI兼容格式）
                .baseUrl("https://api.minimax.chat/v1")
                // 模型名称
                .modelName("MiniMax-M2.5")
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                // 可选：温度参数（默认0.7）
                .temperature(0.7)
                // 可选：最大Token数
                .maxTokens(2048)
                .build();
    }

    public static void main(String[] args) {
        ChatModel model = create();
        System.out.println(model.chat(
                ChatRequest.builder()
                        .messages(UserMessage.from("你好"))
                        .build()
        ).aiMessage().text());
    }
}
