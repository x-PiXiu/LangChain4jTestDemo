package the_15;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SuperAssistantDemo {

    // ==================== Agent 接口定义 ====================

    @SystemMessage("""
        你是一个高效的私人助手，名叫"小助"。你同时管理日历、邮件和知识库。

        ## 工具使用规则：

        1. 日程查询：用户问到"有没有会议"、"明天有什么安排"时，先查日历
        2. 邮件发送：
           - 发邮件前，先用 lookupEmail 查收件人的邮箱地址
           - 不要编造邮箱地址，只使用 lookupEmail 返回的地址
           - 邮件内容要简洁专业
        3. 知识检索：用户问到公司文档、报告、数据时，搜索知识库
        4. 组合操作：用户说"如果...就..."时，先查条件，再根据结果决定下一步

        ## 终止条件：
        - 当你已经完成了用户要求的所有操作，输出总结性回复
        - 不要重复调用已经成功执行的工具

        ## 出错处理：
        - 如果工具返回错误，向用户说明情况并建议替代方案
        - 不要因为一个工具失败就放弃整个任务
        """)
    interface SuperAssistant {
        String chat(@MemoryId String userId, @UserMessage String message);
    }

    // ==================== 启动入口 ====================

    public static void main(String[] args) {
        // 1. 创建 ChatModel
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .listeners(List.of(new TraceListener()))
                .build();

        // 2. 创建 Embedding 模型和向量库（通过 API 调用，无需本地 ONNX）
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("ZHIPU_API_KEY"))
                .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                .modelName("embedding-3")
                .build();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 3. 索引测试文档
        indexSampleDocuments(embeddingModel, embeddingStore);

        // 4. 构建 Agent
        SuperAssistant assistant = AiServices.builder(SuperAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(30))
                .tools(
                        new CalendarTool(),
                        new EmailTool(),
                        new KnowledgeTool(embeddingStore, embeddingModel)
                )
                .build();

        // 5. 测试对话
        System.out.println("===== 测试1：查日历 + 发邮件（组合任务）=====");
        String r1 = assistant.chat("user_001",
                "帮我查一下2026-04-25有没有会议，如果有的话给张三发邮件确认");
        System.out.println("AI：" + r1);

        System.out.println("\n===== 测试2：知识检索 =====");
        String r2 = assistant.chat("user_001",
                "上次Q1的销售数据怎么样？报告文件在哪？");
        System.out.println("AI：" + r2);

        System.out.println("\n===== 测试3：添加日程 + 通知 =====");
        String r3 = assistant.chat("user_001",
                "帮我下周一上午10点安排一个产品评审会，参与者是张三和李四，" +
                        "然后发邮件通知他们");
        System.out.println("AI：" + r3);

        System.out.println("\n===== 测试4：综合查询 =====");
        String r4 = assistant.chat("user_001",
                "帮我看下2026-04-25的安排，如果有空的话帮我安排下午2点技术分享");
        System.out.println("AI：" + r4);
    }

    private static void indexSampleDocuments(EmbeddingModel model,
                                             EmbeddingStore<TextSegment> store) {
        List<TextSegment> docs = List.of(
                TextSegment.from("2026年Q1销售报告：总销售额580万元，同比增长15%。" +
                        "线上渠道320万，线下渠道260万。文件位于 /reports/2026-Q1-销售报告.pdf"),
                TextSegment.from("项目评审流程：每次评审需提前2个工作日提交材料，" +
                        "评审委员会由3名技术专家和2名业务专家组成。")
        );
        for (TextSegment doc : docs) {
            Embedding embedding = model.embed(doc).content();
            store.add(embedding, doc);
        }
    }

    // ==================== 调用追踪 Listener ====================

    static class TraceListener implements ChatModelListener {
        private final AtomicInteger round = new AtomicInteger(0);

        @Override
        public void onRequest(ChatModelRequestContext context) {
            System.out.printf("  🔷 [第%d轮] → LLM%n", round.incrementAndGet());
        }

        @Override
        public void onResponse(ChatModelResponseContext context) {
            var ai = context.chatResponse().aiMessage();
            if (ai.hasToolExecutionRequests()) {
                for (var req : ai.toolExecutionRequests()) {
                    System.out.printf("  🔧 工具调用：%s(%s)%n",
                            req.name(), req.arguments());
                }
            }
        }

        @Override
        public void onError(ChatModelErrorContext context) {
            System.err.println("  ❌ " + context.error().getMessage());
        }
    }
}
