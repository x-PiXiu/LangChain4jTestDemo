package the_10;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.nio.file.Path;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

/**
 * 文档加载与分割实战
 *
 * 演示：加载Markdown文档 → 分割 → 向量化 → 存储 → 检索
 */
public class DocumentSplitDemo {

    public static void main(String[] args) {
        // ========== 1. 初始化模型和存储 ==========
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("ZHIPU_API_KEY"))
                .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                .modelName("embedding-3")
                .build();

        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

        // ========== 2. 创建Ingestor（加载→分割→向量化→存储 一键完成）==========
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(
                        50,    // 每个片段最大200 Token
                        20,     // 重叠20 Token
                        new OpenAiTokenCountEstimator("gpt-4o")
                ))
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .build();

        // ========== 3. 加载文档 ==========
        DocumentParser parser = new TextDocumentParser();
        Document doc = loadDocument(Path.of("src/main/java/the_10/spring-boot-guide.md"), parser);

        System.out.println("📄 文档加载完成");
        System.out.println("   原文长度：" + doc.text().length() + " 字符");

        // ========== 4. 手动分割（演示用，看分割效果）==========
        DocumentSplitter splitter = DocumentSplitters.recursive(200, 20, new OpenAiTokenCountEstimator("gpt-4o"));
        List<TextSegment> segments = splitter.split(doc);

        System.out.println("\n🔪 分割结果（共 " + segments.size() + " 个片段）：");
        System.out.println("-".repeat(60));
        for (int i = 0; i < segments.size(); i++) {
            String text = segments.get(i).text();
            String preview = text.length() > 60 ? text.substring(0, 60) + "..." : text;
            System.out.printf("  片段%02d（%d字）：%s%n", i + 1, text.length(), preview);
        }

        // ========== 5. Ingestor一键入库 ==========
        System.out.println("\n📥 开始向量化并存储...");
        ingestor.ingest(doc);
        System.out.println("✅ 入库完成");

        // ========== 6. 检索测试 ==========
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔍 检索效果测试");
        System.out.println("=".repeat(60));

        String[] queries = {
                "怎么连接数据库",
                "如何处理全局异常",
                "怎么打包部署到生产环境"
        };

        for (String query : queries) {
            System.out.println("\n📝 查询：「" + query + "」");
            System.out.println("-".repeat(40));

            Embedding queryEmbedding = embeddingModel.embed(query).content();
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(2)
                    .minScore(0.3)
                    .build();

            EmbeddingSearchResult<TextSegment> result = store.search(request);
            List<EmbeddingMatch<TextSegment>> matches = result.matches();

            if (matches.isEmpty()) {
                System.out.println("  （无相关结果）");
            } else {
                for (int i = 0; i < matches.size(); i++) {
                    EmbeddingMatch<TextSegment> match = matches.get(i);
                    String text = match.embedded().text();
                    String preview = text.length() > 80 ? text.substring(0, 80) + "..." : text;
                    System.out.printf("  %d. [%.4f] %s%n", i + 1, match.score(), preview);
                }
            }
        }

        // ========== 7. 分割粒度对比 ==========
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📏 分割粒度对比实验");
        System.out.println("=".repeat(60));

        int[] chunkSizes = {50, 200, 1000};
        String testQuery = "怎么处理异常";

        for (int chunkSize : chunkSizes) {
            EmbeddingStore<TextSegment> tempStore = new InMemoryEmbeddingStore<>();
            DocumentSplitter tempSplitter = DocumentSplitters.recursive(
                    chunkSize, chunkSize / 10, new OpenAiTokenCountEstimator("gpt-4o")
            );
            EmbeddingStoreIngestor tempIngestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(tempSplitter)
                    .embeddingModel(embeddingModel)
                    .embeddingStore(tempStore)
                    .build();

            tempIngestor.ingest(doc);

            Embedding qe = embeddingModel.embed(testQuery).content();
            EmbeddingSearchResult<TextSegment> res = tempStore.search(
                    EmbeddingSearchRequest.builder()
                            .queryEmbedding(qe)
                            .maxResults(1)
                            .minScore(0.0)
                            .build()
            );

            double bestScore = res.matches().isEmpty() ? 0.0 : res.matches().get(0).score();
            System.out.printf("  chunkSize=%4d → 片段数=%2d, 最佳相似度=%.4f%n",
                    chunkSize, tempSplitter.split(doc).size(), bestScore);
        }

        System.out.println("\n💡 粒度太小会丢失上下文，太大会稀释语义，300-500是甜蜜点！");
    }
}
