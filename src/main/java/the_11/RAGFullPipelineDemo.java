package the_11;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.nio.file.Path;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

/**
 * RAG 完整链路实战（LangChain4j 1.x 版本）
 *
 * 演示：文件加载 → 分割 → 向量化 → 存储 → 检索 → Prompt注入 → LLM生成
 */
public class RAGFullPipelineDemo {

    public static void main(String[] args) {
        // ========== 1. 初始化模型 ==========
        String apiKey = System.getenv("MINIMAX_API_KEY");
        String baseUrl = "https://api.minimax.chat/v1";

        // ChatModel（用于生成回答，1.x 版本）
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName("MiniMax-M2.5")
                .temperature(0.7)
                .build();

        // EmbeddingModel（用于向量化，Indexing 和 Retrieval 必须用同一个）
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("ZHIPU_API_KEY"))
                .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                .modelName("embedding-3")
                .build();

        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

        // ========== 2. Indexing：加载文档 → 分割 → 向量化 → 存储 ==========
        System.out.println("📦 开始 Indexing（离线阶段）...");
        System.out.println("-".repeat(50));

        DocumentParser parser = new TextDocumentParser();
        Document doc = loadDocument(
                Path.of("src/main/java/the_11/customer-service.txt"), parser
        );
        System.out.println("   文档长度：" + doc.text().length() + " 字符");

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(
                        200, 20, new OpenAiTokenCountEstimator("gpt-4o")
                ))
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .build();

        ingestor.ingest(doc);
        System.out.println("✅ Indexing 完成，知识库已就绪\n");

        // ========== 3. 手动走一遍 RAG 全流程（教学演示）==========
        runRAGDemo(chatModel, embeddingModel, store);

        // ========== 4. 连续提问测试 ==========
        runQuestionTest(chatModel, embeddingModel, store);
    }

    /**
     * RAG 全流程演示
     */
    static void runRAGDemo(ChatModel chatModel,
                           EmbeddingModel embeddingModel,
                           EmbeddingStore<TextSegment> store) {
        System.out.println("🔍 RAG 全流程演示");
        System.out.println("=".repeat(50));

        String question = "买错了想退回去，怎么操作？";
        System.out.println("📝 用户提问：" + question);

        // Step A: Retrieval（检索）
        System.out.println("\n  [Retrieval] 检索相关内容...");
        Embedding queryEmbedding = embeddingModel.embed(question).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(3)
                .minScore(0.5)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

        for (int i = 0; i < matches.size(); i++) {
            String text = matches.get(i).embedded().text();
            String preview = text.length() > 60 ? text.substring(0, 60) + "..." : text;
            System.out.printf("    片段%d [%.4f]: %s%n", i + 1, matches.get(i).score(), preview);
        }

        // Step B: Prompt 组装（Augmented）
        System.out.println("\n  [Augmented] 组装 Prompt...");
        String prompt = buildPrompt(question, matches);
        System.out.println("    Prompt长度：" + prompt.length() + " 字符（含" + matches.size() + "个检索片段）");

        // Step C: Generation（生成）
        System.out.println("\n  [Generation] LLM 生成回答...");
        ChatResponse genResponse = chatModel.chat(
                ChatRequest.builder()
                        .messages(UserMessage.from(prompt))
                        .build()
        );
        String answer = genResponse.aiMessage().text();
        System.out.println("\n💬 AI 回答：");
        System.out.println("  " + answer);
    }

    /**
     * 连续提问测试
     */
    static void runQuestionTest(ChatModel chatModel,
                                EmbeddingModel embeddingModel,
                                EmbeddingStore<TextSegment> store) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("💬 连续提问测试");
        System.out.println("=".repeat(50));

        String[] questions = {
                "优惠券能不能叠加使用？",
                "你们用什么快递？多久能到？",
                "会员积分怎么兑换？",
                "你们的退货政策是什么？我想退一个买了10天的东西",
                "你们有没有卖手机？"  // 故意问知识库没有的
        };

        for (String q : questions) {
            System.out.println("\n📝 问：" + q);

            // 检索
            Embedding qe = embeddingModel.embed(q).content();
            EmbeddingSearchResult<TextSegment> sr = store.search(
                    EmbeddingSearchRequest.builder()
                            .queryEmbedding(qe)
                            .maxResults(3)
                            .minScore(0.3)  // 降低阈值，防止漏检
                            .build()
            );
            List<EmbeddingMatch<TextSegment>> matches = sr.matches();

            if (matches.isEmpty()) {
                System.out.println("  ⚠️ 未检索到相关内容");
                continue;
            }

            // 生成（1.x: ChatRequest → ChatResponse）
            ChatResponse genR = chatModel.chat(
                    ChatRequest.builder()
                            .messages(UserMessage.from(buildPrompt(q, matches)))
                            .build()
            );
            String ans = genR.aiMessage().text();
            System.out.println("💬 答：" + ans);
        }

        System.out.println("\n💡 看到最后一个问题了吗？知识库里没有手机相关内容，" +
                "但 AI 不会瞎编——这就是 Prompt 中\"如果参考资料中没有，请说我不确定\"的威力。");
    }

    /**
     * 构建 Prompt（Augmented 的核心）
     */
    static String buildPrompt(String question, List<EmbeddingMatch<TextSegment>> matches) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个客服助手，根据以下参考资料回答用户的问题。")
                .append("如果参考资料中没有相关内容，请说\"我不确定\"，不要编造。")
                .append("回答要简洁，不超过100字。\n\n")
                .append("参考资料：\n");

        for (int i = 0; i < matches.size(); i++) {
            sb.append("[").append(i + 1).append("] ")
                    .append(matches.get(i).embedded().text().replace("\n", " "))
                    .append("\n");
        }

        sb.append("\n用户问题：").append(question);
        return sb.toString();
    }
}