package the_19;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import java.util.Scanner;

/**
 * 第19篇 Guardrails 与 OutputParser 完整 Demo
 *
 * 演示内容：
 * 1. OutputParser（RobustWeatherOutputParser, EnumConstrainedOutputParser, LengthBoundedOutputParser）
 * 2. PII 脱敏（PIIRedactor）
 * 3. 内容审核（ContentModerator）
 * 4. 完整 Guardrails 管道（GuardrailedCustomerServiceAgent）
 * 5. 拦截日志与审计（GuardrailAuditLog）
 */
public class GuardrailDemo {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  第19篇 Guardrails 与 OutputParser Demo");
        System.out.println("========================================\n");

        // === 第1部分：OutputParser 演示 ===
        demoOutputParsers();

        // === 第2部分：PII 脱敏演示 ===
        demoPIIRedaction();

        // === 第3部分：内容审核演示 ===
        demoContentModeration();

        // === 第4部分：完整 Guardrails 管道演示（需要 API Key） ===
        demoFullPipeline();
    }

    /** 第1部分：OutputParser 演示 */
    private static void demoOutputParsers() {
        System.out.println("━━━ 1. OutputParser 演示 ━━━\n");

        // 1.1 EnumConstrainedOutputParser
        System.out.println("【枚举解析器】");
        EnumConstrainedOutputParser<OrderStatus> enumParser =
            new EnumConstrainedOutputParser<>(OrderStatus.class);

        String[] inputs = {"SUCCESS", "success", "SUCCESS"};
        for (String input : inputs) {
            try {
                OrderStatus status = enumParser.parse(input);
                System.out.println("  输入: \"" + input + "\" → " + status);
            } catch (Exception e) {
                System.out.println("  输入: \"" + input + "\" → 解析失败: " + e.getMessage());
            }
        }
        System.out.println("  格式指令: " + enumParser.formatInstructions());

        // 1.2 LengthBoundedOutputParser
        System.out.println("\n【长度限制解析器】");
        LengthBoundedOutputParser<OrderStatus> lengthParser =
            new LengthBoundedOutputParser<>(enumParser, 10);
        try {
            String longInput = "SUCCESS";
            OrderStatus result = lengthParser.parse(longInput);
            System.out.println("  输入: \"" + longInput + "\" → " + result);
        } catch (Exception e) {
            System.out.println("  解析失败: " + e.getMessage());
        }

        System.out.println();
    }

    /** 第2部分：PII 脱敏演示 */
    private static void demoPIIRedaction() {
        System.out.println("━━━ 2. PII 脱敏演示 ━━━\n");

        PIIRedactor redactor = new PIIRedactor();

        String[] testCases = {
            "我的身份证号是 110101199001011234，请帮我查一下",
            "手机号 13812345678 可以接收验证码吗",
            "我的银行卡号是 6222021234567890123，帮我查余额",
            "发送到 zhangsan@example.com 就行",
            "身份证 110101199001011234，手机 13912345678，邮箱 lisi@test.com"
        };

        for (String testCase : testCases) {
            String redacted = redactor.redact(testCase);
            System.out.println("  原文: " + testCase);
            System.out.println("  脱敏: " + redacted);
            System.out.println();
        }
    }

    /** 第3部分：内容审核演示 */
    private static void demoContentModeration() {
        System.out.println("━━━ 3. 内容审核演示 ━━━\n");

        ContentModerator moderator = new ContentModerator();

        String[] testCases = {
            "你好，我想查询一下我的订单状态",
            "这个产品真不错，推荐购买",
            "我想了解赌博网站的信息",
            "产品的性能如何，暴力测试能通过吗"
        };

        for (String testCase : testCases) {
            ModerationResult result = moderator.moderate(testCase);
            System.out.println("  内容: " + testCase);
            System.out.println("  结果: " + (result.isClean() ? "通过" : "拦截"));
            if (!result.isClean()) {
                System.out.println("  违规: " + result.getViolations());
            }
            System.out.println();
        }
    }

    /** 第4部分：完整 Guardrails 管道演示 */
    private static void demoFullPipeline() {
        System.out.println("━━━ 4. 完整 Guardrails 管道演示 ━━━\n");

        String apiKey = System.getenv("OPENAI_API_KEY");
        String baseUrl = System.getenv("OPENAI_BASE_URL");

        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("  [跳过] 未配置 API Key，使用模拟模式演示\n");
            demoSimulatedPipeline();
            return;
        }

        try {
            // 构建 ChatModel
            ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl != null ? baseUrl : "https://api.openai.com/v1")
                .modelName("gpt-4o-mini")
                .temperature(0.7)
                .build();

            // 构建 AI 服务
            CustomerServiceAi ai = AiServices.builder(CustomerServiceAi.class)
                .chatModel(chatModel)
                .build();

            // 构建 Guardrails 管道
            GuardrailAuditLog auditLog = new GuardrailAuditLog();
            GuardrailedCustomerServiceAgent agent = new GuardrailedCustomerServiceAgent(
                ai,
                new PIIRedactor(),
                new ContentModerator(),
                auditLog
            );

            // 交互测试
            Scanner scanner = new Scanner(System.in);
            System.out.println("  Guardrails 管道已就绪，输入消息测试（输入 'quit' 退出）\n");

            while (true) {
                System.out.print("  你: ");
                String input = scanner.nextLine().trim();
                if ("quit".equals(input) || "exit".equals(input)) {
                    break;
                }

                AgentResponse response = agent.chat("demo-user", input);
                System.out.println("  Agent: " + (response.isSuccess()
                    ? response.getData().getMessage()
                    : "错误: " + response.getErrorMessage()));
                System.out.println();
            }

            // 打印拦截统计
            System.out.println(auditLog.getStatistics());

        } catch (Exception e) {
            System.out.println("  [错误] " + e.getMessage());
            System.out.println("  切换到模拟模式...\n");
            demoSimulatedPipeline();
        }
    }

    /** 模拟模式：无需 API Key，使用模拟的 AI 回复 */
    private static void demoSimulatedPipeline() {
        // 模拟 AI 服务
        CustomerServiceAi mockAi = userMessage -> {
            if (userMessage.contains("身份证")) {
                return "您好，根据您提供的身份证号 110101199001011234，我已查询到您的账户信息。";
            }
            if (userMessage.contains("手机")) {
                return "您好，验证码已发送到手机号 13812345678，请注意查收。";
            }
            return "您好，很高兴为您服务！请问有什么可以帮助您的？";
        };

        // 构建 Guardrails 管道
        GuardrailAuditLog auditLog = new GuardrailAuditLog();
        GuardrailedCustomerServiceAgent agent = new GuardrailedCustomerServiceAgent(
            mockAi,
            new PIIRedactor(),
            new ContentModerator(),
            auditLog
        );

        // 测试用例
        String[] testMessages = {
            "我的身份证号是 110101199001011234，帮我查一下账户",
            "我的手机号 13812345678，帮我发个验证码",
            "你好，请问你们的客服工作时间是什么时候"
        };

        for (String msg : testMessages) {
            System.out.println("  用户: " + msg);
            AgentResponse response = agent.chat("sim-user", msg);
            if (response.isSuccess()) {
                System.out.println("  Agent: " + response.getData().getMessage());
            } else {
                System.out.println("  Agent: [错误] " + response.getErrorMessage());
            }
            System.out.println();
        }

        // 打印拦截统计
        System.out.println(auditLog.getStatistics());
    }
}
