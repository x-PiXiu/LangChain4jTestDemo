package the_9;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 语义搜索引擎
 *
 * 演示：Embedding + EmbeddingStore + 相似度搜索的完整流程
 */
public class SemanticSearchEngine {

    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingModel embeddingModel;

    public SemanticSearchEngine(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.store = new InMemoryEmbeddingStore<>();
    }

    /**
     * 索引知识库：把文本变成向量并存入EmbeddingStore
     */
    public void index(List<String> documents) {
        List<TextSegment> segments = new ArrayList<>();
        List<Embedding> embeddings = new ArrayList<>();

        for (String doc : documents) {
            TextSegment segment = TextSegment.from(doc);
            Embedding embedding = embeddingModel.embed(segment).content();
            segments.add(segment);
            embeddings.add(embedding);
        }

        // 批量存储（比一条条存快很多）
        store.addAll(embeddings, segments);
        System.out.println("✅ 索引完成，共 " + documents.size() + " 条文档");
    }

    /**
     * 语义搜索：输入一句话，返回最相关的N条
     */
    public List<SearchResult> search(String query, int topK, double minScore) {
        // Step 1: 把查询变成向量
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // Step 2: 构建搜索请求
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .minScore(minScore)
                .build();

        // Step 3: 执行搜索
        EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);

        // Step 4: 格式化结果
        List<SearchResult> results = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
            results.add(new SearchResult(
                    match.embedded().text(),
                    match.score()
            ));
        }
        return results;
    }

    /**
     * 搜索结果记录
     */
    public record SearchResult(String text, double score) {}

    // ==================== Main ====================

    public static void main(String[] args) {
        // 创建Embedding模型（使用智谱embedding-3）
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("ZHIPU_API_KEY"))
                .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                .modelName("embedding-3")
                .build();

        SemanticSearchEngine engine = new SemanticSearchEngine(embeddingModel);

        // 索引知识库
        engine.index(List.of(
                // 退换货类
                "退货政策：自收到商品之日起7天内可申请无理由退货",
                "退货流程：联系客服申请 → 填写退货单 → 寄回商品 → 3-5个工作日退款",
                "换货政策：自收到商品之日起15天内可申请换货",
                "生鲜商品不支持无理由退货，如有质量问题请拍照联系客服",

                // 物流类
                "发货时间：下单后24小时内发货，节假日顺延",
                "快递说明：默认顺丰速运，偏远地区可能使用中通",
                "物流时效：一线城市1-2天，二三线城市2-3天",

                // 优惠类
                "优惠券规则：每笔订单只能使用一张优惠券，不可叠加",
                "新人首单优惠：注册即送满100减20优惠券，7天内有效",
                "会员积分：每消费1元积1分，100积分可兑换1元优惠券",

                // 服务类
                "客服时间：人工客服 9:00-21:00，智能客服24小时在线",
                "投诉渠道：拨打400-XXX-XXXX热线，或联系在线客服",
                "发票说明：订单完成后可在'我的订单'中申请电子发票"
        ));

        // 测试语义搜索
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔍 语义搜索测试");
        System.out.println("=".repeat(60));

        String[] queries = {
                "我不想要了，怎么退",
                "东西坏了想换一个",
                "多久能送到",
                "怎么用优惠券",
                "怎么开发票"
        };

        for (String query : queries) {
            System.out.println("\n📝 查询：「" + query + "」");
            System.out.println("-".repeat(40));

            List<SearchResult> results = engine.search(query, 3, 0.5);
            if (results.isEmpty()) {
                System.out.println("  （未找到相关结果，可能 minScore 阈值过高）");
            } else {
                for (int i = 0; i < results.size(); i++) {
                    SearchResult r = results.get(i);
                    System.out.printf("  %d. [%.4f] %s%n", i + 1, r.score(), r.text());
                }
            }
        }

        // 对比：关键词搜索 vs 语义搜索
        System.out.println("\n" + "=".repeat(60));
        System.out.println("⚡ 关键词搜索 vs 语义搜索 对比");
        System.out.println("=".repeat(60));

        String trickyQuery = "买错了想退回去";
        System.out.println("\n📝 查询：「" + trickyQuery + "」");
        System.out.println("  （这句话里没有'退货'两个字，但语义是退货）");
        System.out.println("-".repeat(40));

        List<SearchResult> results = engine.search(trickyQuery, 3, 0.3);
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            System.out.printf("  %d. [%.4f] %s%n", i + 1, r.score(), r.text());
        }

        System.out.println("\n💡 看到「买错了想退回去」能匹配到退货政策——这就是语义搜索的威力！");
    }
}
