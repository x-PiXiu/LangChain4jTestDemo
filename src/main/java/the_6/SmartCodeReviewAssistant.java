package the_6;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.Map;

/**
 * 智能代码审查助手
 *
 * 功能：
 * 1. 代码审查（发现潜在问题）
 * 2. 代码优化（生成更好实现）
 * 3. 对话历史记忆（ConversationalChain）
 * 4. 多轮讨论
 */
public class SmartCodeReviewAssistant {

    private final ConversationalChain reviewChain;
    private final ConversationalChain optimizeChain;

    public SmartCodeReviewAssistant(ChatModel model) {
        // 审查Chain：自动记忆对话历史
        var reviewMemory = MessageWindowChatMemory.builder()
                .id("review_session")
                .maxMessages(50)
                .build();
        reviewMemory.add(SystemMessage.from("""
                你是一个严格的Java代码审查助手"代码医生"。

                诊断维度：
                1. 正确性：有没有Bug？
                2. 性能：有没有性能问题？
                3. 可读性：代码是否清晰易懂？
                4. 最佳实践：符合Java编码规范吗？

                回答要友好、专业，包含代码示例。
                """));
        this.reviewChain = ConversationalChain.builder()
                .chatModel(model)
                .chatMemory(reviewMemory)
                .build();

        // 优化Chain：独立的记忆上下文
        var optimizeMemory = MessageWindowChatMemory.builder()
                .id("optimize_session")
                .maxMessages(30)
                .build();
        optimizeMemory.add(SystemMessage.from("""
                你是一个专业的Java代码优化助手"代码整容师"。

                根据审查发现的问题，生成优化后的代码。
                每次输出：优化后的代码 + 改进说明。
                """));
        this.optimizeChain = ConversationalChain.builder()
                .chatModel(model)
                .chatMemory(optimizeMemory)
                .build();
    }

    /**
     * 审查代码
     */
    public String review(String code) {
        System.out.println("【代码医生】收到代码，开始诊断...");
        return reviewChain.execute("请审查以下代码：\n" + code);
    }

    /**
     * 优化代码
     */
    public String optimize(String code, String issues) {
        System.out.println("【代码整容师】收到问题，开始优化...");
        return optimizeChain.execute(
                "原代码：\n" + code + "\n\n发现的问题：\n" + issues + "\n\n请生成优化后的代码"
        );
    }

    /**
     * 继续对话
     */
    public String continueChat(String message) {
        return reviewChain.execute(message);
    }

    public static void main(String[] args) {
        // 1. 初始化
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        SmartCodeReviewAssistant assistant = new SmartCodeReviewAssistant(model);

        // 2. 审查代码
        System.out.println("=== 第1轮：审查代码 ===");
        String code1 = """
            public void process(List<String> list) {
                for (int i = 0; i < list.size(); i++) {
                    System.out.println(list.get(i));
                }
            }
            """;
        String result1 = assistant.review(code1);
        System.out.println("审查结果：" + result1);

        // 3. 优化代码
        System.out.println("\n=== 第2轮：优化代码 ===");
        String result2 = assistant.optimize(code1, result1);
        System.out.println("优化结果：" + result2);

        // 4. 多轮对话
        System.out.println("\n=== 第3轮：追问 ===");
        String result3 = assistant.continueChat("刚才说的性能问题，怎么用Java8改造？");
        System.out.println("追问回答：" + result3);
    }
}
