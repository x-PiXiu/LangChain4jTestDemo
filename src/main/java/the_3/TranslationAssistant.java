package the_3;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.Map;

public class TranslationAssistant {

    private final ChatModel model;

    public TranslationAssistant(ChatModel model) {
        this.model = model;
    }

    public String translate(String text, String targetLanguage) {
        // 1. 定义Prompt模板
        PromptTemplate template = PromptTemplate.from("""
            你是一位专业翻译专家。
            
            请将以下文本翻译成{{targetLanguage}}：
            
            原文：{{text}}
            
            翻译要求：
            - 保持原文风格和语气
            - 专业术语使用标准译法
            - 结果直接输出翻译内容，不要多余解释
            """);

        // 2. 渲染模板（替换变量）- 使用 apply() 而不是 render()
        String promptText = template.apply(Map.of(
                "text", text,
                "targetLanguage", targetLanguage
        )).text();

        // 3. 构建请求
        ChatRequest request = ChatRequest.builder()
                .messages(
                        SystemMessage.from("你是一个专业翻译专家"),
                        UserMessage.from(promptText)
                )
                .build();

        // 4. 发送并返回
        ChatResponse response = model.chat(request);
        return response.aiMessage().text();
    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        TranslationAssistant assistant = new TranslationAssistant(model);

        String result = assistant.translate(
                "Spring Boot makes Java development simpler and more productive.",
                "中文"
        );
        System.out.println(result);
    }
}