package the_8;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 最简RAG系统
 */
public class SimpleRAG {

    private final ChatModel model;
    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingModel embeddingModel;

    public SimpleRAG(ChatModel model, EmbeddingModel embeddingModel) {
        this.model = model;
        this.embeddingModel = embeddingModel;
        this.store = new InMemoryEmbeddingStore<>();
    }

    /**
     * 添加知识到知识库
     */
    public void addKnowledge(String text) {
        TextSegment segment = TextSegment.from(text);
        Embedding embedding = embeddingModel.embed(segment).content();
        store.add(embedding, segment);
    }

    /**
     * 批量添加知识
     */
    public void addKnowledgeBatch(List<String> texts) {
        List<TextSegment> segments = new ArrayList<>();
        List<Embedding> embeddings = new ArrayList<>();
        for (String text : texts) {
            TextSegment segment = TextSegment.from(text);
            segments.add(segment);
            embeddings.add(embeddingModel.embed(segment).content());
        }
        store.addAll(embeddings, segments);
    }

    /**
     * 检索最相关的知识
     */
    public String retrieve(String question, int topK) {
        Embedding queryEmbedding = embeddingModel.embed(question).content();
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .minScore(0.5)
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

        StringBuilder context = new StringBuilder();
        for (EmbeddingMatch<TextSegment> match : matches) {
            context.append("- ").append(match.embedded().text()).append("\n");
        }
        return context.toString();
    }

    /**
     * RAG回答：检索 + 生成
     */
    public String answer(String question) {
        // Step 1: 检索相关知识
        String context = retrieve(question, 3);

        // Step 2: 构建Prompt（把知识塞进去）
        String prompt = """
            你是一个客服助手。请根据以下知识回答用户问题。
            如果知识库中没有相关信息，请回答"这个问题我需要进一步确认"。

            知识库：
            %s

            用户问题：%s
            """.formatted(context, question);

        // Step 3: 调用LLM生成回答
        return model.chat(
                ChatRequest.builder()
                        .messages(UserMessage.from(prompt))
                        .build()
        ).aiMessage().text();
    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("ZHIPU_API_KEY"))
                .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                .modelName("embedding-3")
                .build();

        SimpleRAG rag = new SimpleRAG(model, embeddingModel);

        // 添加企业知识
        rag.addKnowledgeBatch(List.of(
                "退货政策：自收到商品之日起7天内可申请退货",
                "换货政策：自收到商品之日起15天内可申请换货",
                "优惠券规则：每笔订单只能使用一张优惠券",
                "会员权益：100积分可兑换1元优惠券",
                "发货时间：下单后24小时内发货"
        ));

        // 测试RAG
        String[] questions = {
                "我几天可以申请退货？",
                "可以用两张优惠券吗？",
                "积分怎么兑换？"
        };

        for (String q : questions) {
            System.out.println("用户：" + q);
            System.out.println("AI：" + rag.answer(q));
            System.out.println();
        }
    }
}
