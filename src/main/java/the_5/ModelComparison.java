package the_5;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 多模型对比测试
 */
public class ModelComparison {

    // MiniMax配置
    private static ChatModel minimax() {
        return OpenAiChatModel.builder()
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .temperature(0.7)
                .build();
    }

    // GLM配置
    private static ChatModel glm() {
        return OpenAiChatModel.builder()
                .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                .modelName("glm-4-flash")
                .apiKey(System.getenv("ZHIPU_API_KEY"))
                .temperature(0.7)
                .build();
    }

    // 通义千问配置
    private static ChatModel qwen() {
        return OpenAiChatModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .modelName("qwen-plus")
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .temperature(0.7)
                .build();
    }

    public static void main(String[] args) {
        String question = "用Java写一个快速排序算法，并说明时间复杂度";

        System.out.println("=".repeat(50));
        System.out.println("问题：" + question);
        System.out.println("=".repeat(50));

        // 测试MiniMax
        System.out.println("\n【MiniMax M2.5】");
        printResponse(minimax(), question);

        // 测试GLM
        System.out.println("\n【GLM-4 Flash】");
        printResponse(glm(), question);

        // 测试Qwen
        System.out.println("\n【通义千问Plus】");
        //printResponse(qwen(), question);
    }

    private static void printResponse(ChatModel model, String question) {
        long start = System.currentTimeMillis();
        try {
            ChatResponse response = model.chat(
                    ChatRequest.builder()
                            .messages(UserMessage.from(question))
                            .maxOutputTokens(1000)
                            .build()
            );
            long cost = System.currentTimeMillis() - start;
            System.out.println("耗时：" + cost + "ms");
            System.out.println("回答：");
            System.out.println(response.aiMessage().text());
        } catch (Exception e) {
            System.out.println("调用失败：" + e.getMessage());
        }
    }
}