package the_5;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;

public class AIClient {

    private ChatModel model;

    // 切换模型只需要改这里
    public void setModel(ChatModel model) {
        this.model = model;
    }

    // 其他代码完全不用改
    public String chat(String message) {
        return model.chat(
                ChatRequest.builder()
                        .messages(UserMessage.from(message))
                        .build()
        ).aiMessage().text();
    }

    public static void main(String[] args) {
        AIClient client = new AIClient();

        // 切换到MiniMax
        client.setModel(ModelSelector.cheapModel());
        System.out.println(client.chat("今天天气怎么样？"));

        // 切换到GLM
        client.setModel(GLMConfig.create());
        System.out.println(client.chat("请解释量子计算"));
    }
}