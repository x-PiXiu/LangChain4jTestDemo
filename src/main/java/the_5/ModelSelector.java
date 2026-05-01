package the_5;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class ModelSelector {

    // 便宜模型：日常对话
    public static ChatModel cheapModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2")
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .temperature(0.7)
                .build();
    }

    // 贵模型：复杂任务
    public static ChatModel expensiveModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://api.minimax.io/v1")
                .modelName("MiniMax-M2.5")
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .temperature(0.3)
                .build();
    }

    // 根据问题复杂度自动选择
    public static ChatModel select(String question) {
        // 简单问题用便宜模型
        if (question.length() < 50) {
            return cheapModel();
        }
        // 复杂问题用贵模型
        return expensiveModel();
    }
}
