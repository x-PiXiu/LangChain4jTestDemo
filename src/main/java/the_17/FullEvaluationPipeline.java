package the_17;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import the_15.CalendarTool;
import the_15.EmailTool;
import the_15.KnowledgeTool;

import java.util.List;

public class FullEvaluationPipeline {

    private static final String API_KEY = System.getenv("MINIMAX_API_KEY");
    private static final String BASE_URL = "https://api.minimax.chat/v1";

    record TestCase(
            String input,
            List<String> expectedTools,
            String expectedContent,
            int maxToolCalls
    ) {}

    public static void main(String[] args) {
        // 1. 构建被评估的 Agent
        ChatModel chatModel = createChatModel();
        SuperAssistant agent = buildAgent(chatModel);

        // 2. 定义测试集
        List<TestCase> testCases = List.of(
                new TestCase("Q1销售数据怎么样", List.of("searchKnowledge"), "580万", 2),
                new TestCase("帮我查明天会议", List.of("queryEvents"), "日程", 1),
                new TestCase("查明天会议，有的话通知张三",
                        List.of("queryEvents", "lookupEmail", "sendEmail"), "邮件", 4),
                new TestCase("项目评审流程是什么", List.of("searchKnowledge"), "评审", 2),
                new TestCase("帮我添加下周一10点技术分享", List.of("addEvent"), "已添加", 1)
        );

        // 3. 运行评估
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     RAG + Agent 完整评估报告            ║");
        System.out.println("╚══════════════════════════════════════════╝");

        int completed = 0;

        for (TestCase tc : testCases) {
            String result = agent.chat("eval_user", tc.input);
            boolean passed = result.contains(tc.expectedContent());
            if (passed) completed++;

            System.out.printf("%n── 测试： %s ──%n", tc.input);
            System.out.printf("  状态：%s%n", passed ? "✅ 通过" : "❌ 未通过");
            System.out.printf("  预期关键词：'%s'%n", tc.expectedContent());
            System.out.printf("  回答摘要：%s%n",
                    result.length() > 80 ? result.substring(0, 80) + "..." : result);
        }

        // 4. 汇总
        System.out.println("\n════════════════ 汇总 ════════════════");
        System.out.printf("任务完成率：%d/%d（%.0f%%）%n",
                completed, testCases.size(), (double) completed / testCases.size() * 100);
        System.out.println("建议：未通过的用例需要检查工具描述和 SystemMessage");
    }

    // ==================== 构建工具方法 ====================

    private static ChatModel createChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .modelName("MiniMax-M2.5")
                .listeners(List.of(new DebugListener()))
                .build();
    }

    private static SuperAssistant buildAgent(ChatModel chatModel) {
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("ZHIPU_API_KEY"))
                .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                .modelName("embedding-3")
                .build();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        indexDocuments(embeddingModel, embeddingStore);

        return AiServices.builder(SuperAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(30))
                .tools(
                        new CalendarTool(),
                        new EmailTool(),
                        new KnowledgeTool(embeddingStore, embeddingModel)
                )
                .build();
    }

    private static void indexDocuments(EmbeddingModel model,
                                        EmbeddingStore<TextSegment> store) {
        List<TextSegment> docs = List.of(
                TextSegment.from("2026年Q1销售报告：总销售额580万元，同比增长15%。" +
                        "线上渠道320万，线下渠道260万。文件位于 /reports/2026-Q1-销售报告.pdf"),
                TextSegment.from("项目评审流程：每次评审需提前2个工作日提交材料，" +
                        "评审委员会由3名技术专家和2名业务专家组成。")
        );
        try {
            for (TextSegment doc : docs) {
                Embedding embedding = model.embed(doc).content();
                store.add(embedding, doc);
            }
            System.out.println("📄 知识文档索引完成（" + docs.size() + " 篇）");
        } catch (Exception e) {
            System.err.println("⚠️ 知识文档索引失败：" + e.getMessage());
            System.err.println("   知识检索相关测试将无法通过，其余测试正常执行");
        }
    }
}
