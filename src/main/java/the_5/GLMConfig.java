package the_5;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class GLMConfig {

    public static ChatModel create() {
        return OpenAiChatModel.builder()
                .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                .modelName("glm-4-flash")
                .apiKey(System.getenv("ZHIPU_API_KEY"))
                .temperature(0.7)
                .maxTokens(2048)
                .build();
    }
}
