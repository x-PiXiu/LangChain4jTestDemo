package the_6;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.Map;

/**
 * 多步骤Chain工作流
 * Step1: 审查代码 → Step2: 生成优化 → Step3: 验证结果
 */
public class MultiStepCodeReviewChain {

    private final ChatModel model;

    // ========== 第一步：审查Chain ==========
    private final PromptTemplate reviewTemplate = PromptTemplate.from("""
        你是一个严格的Java代码审查员。
        请审查以下代码，找出所有问题。
        
        代码：
        {{code}}
        
        只输出JSON数组格式：
        [{"line": 行号, "issue": "问题描述", "severity": "HIGH/MEDIUM/LOW"}]
        """);

    public String review(String code) {
        String prompt = reviewTemplate.apply(Map.of("code", code)).text();
        return model.chat(
                ChatRequest.builder()
                        .messages(UserMessage.from(prompt))
                        .build()
        ).aiMessage().text();
    }

    // ========== 第二步：优化Chain ==========
    private final PromptTemplate optimizeTemplate = PromptTemplate.from("""
        你是一个专业的Java开发工程师。
        
        原代码：
        {{code}}
        
        发现的问题：
        {{issues}}
        
        请生成优化后的代码。只输出代码，不要解释。
        """);

    public String optimize(String code, String issues) {
        String prompt = optimizeTemplate.apply(Map.of(
                "code", code,
                "issues", issues
        )).text();
        return model.chat(
                ChatRequest.builder()
                        .messages(UserMessage.from(prompt))
                        .build()
        ).aiMessage().text();
    }

    // ========== 第三步：验证Chain ==========
    private final PromptTemplate verifyTemplate = PromptTemplate.from("""
        你是一个Java编译检查助手。
        
        原代码：
        {{original}}
        
        优化后代码：
        {{optimized}}
        
        验证：
        1. 语法是否正确
        2. 功能是否等价
        3. 性能是否真的提升了
        
        输出JSON：
        {"syntaxCorrect": boolean, "equivalent": boolean, "improved": boolean, "note": string}
        """);

    public String verify(String original, String optimized) {
        String prompt = verifyTemplate.apply(Map.of(
                "original", original,
                "optimized", optimized
        )).text();
        return model.chat(
                ChatRequest.builder()
                        .messages(UserMessage.from(prompt))
                        .build()
        ).aiMessage().text();
    }

    public MultiStepCodeReviewChain(ChatModel model) {
        this.model = model;
    }

    /**
     * 执行完整的三步骤Chain（串联三个步骤）
     */
    public ReviewResult run(String code) {
        System.out.println("========== Step 1: 代码审查 ==========");
        String issues = review(code);
        System.out.println("发现问题：" + issues);

        System.out.println("\n========== Step 2: 生成优化 ==========");
        String optimizedCode = optimize(code, issues);
        System.out.println("优化后代码：" + optimizedCode);

        System.out.println("\n========== Step 3: 验证 ==========");
        String verification = verify(code, optimizedCode);
        System.out.println("验证结果：" + verification);

        return new ReviewResult(code, issues, optimizedCode, verification);
    }

    // 结果封装
    public record ReviewResult(
            String originalCode,
            String issues,
            String optimizedCode,
            String verification
    ) {}

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        MultiStepCodeReviewChain chain = new MultiStepCodeReviewChain(model);

        String code = """
            public int sumOfSquares(int n) {
                int sum = 0;
                for (int i = 1; i <= n; i++) {
                    sum += i * i;
                }
                return sum;
            }
            """;

        ReviewResult result = chain.run(code);

        System.out.println("\n========== 最终汇总 ==========");
        System.out.println("原始代码：" + result.originalCode());
        System.out.println("发现问题：" + result.issues());
        System.out.println("优化后代码：" + result.optimizedCode());
        System.out.println("验证结果：" + result.verification());
    }
}