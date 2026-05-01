package the_7;

import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
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
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

import java.util.*;

/**
 * 智能客服系统
 *
 * 特性：
 * 1. 多用户隔离（每个用户独立Memory）
 * 2. 持久化存储（MySQL）
 * 3. 自动摘要压缩（防止Token爆炸）
 * 4. 企业知识融入（RAG）
 */
public class SmartCustomerService {

    private final ChatModel model;
    private final ChatMemoryStore store;
    private final EmbeddingStore<TextSegment> knowledgeBase;
    private final EmbeddingModel embeddingModel;

    private final Map<String, ChatMemory> memories = new HashMap<>();
    private final Map<String, ConversationalChain> chains = new HashMap<>();

    private static final int SUMMARY_TRIGGER = 15;
    private static final int MAX_KNOWLEDGE_RESULTS = 3;

    private static final String SUMMARY_PROMPT = """
            请将以下对话历史压缩成一段简洁的摘要。

            要求：
            1. 保留关键信息（用户身份、问题类型、解决方案）
            2. 删除冗余表达和重复内容
            3. 输出格式：{"summary": "摘要内容", "keyInfo": ["关键点1", "关键点2"]}

            对话历史：
            %s
            """;

    public SmartCustomerService(ChatModel model,
                                ChatMemoryStore store,
                                EmbeddingStore<TextSegment> knowledgeBase,
                                EmbeddingModel embeddingModel) {
        this.model = model;
        this.store = store;
        this.knowledgeBase = knowledgeBase;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 处理用户消息
     */
    public ServiceResponse handle(String userId, String userMessage) {
        // 1. 获取或创建Memory
        ChatMemory memory = memories.computeIfAbsent(userId, id ->
                MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(30)
                        .chatMemoryStore(store)
                        .build()
        );

        // 2. 检查是否需要摘要压缩
        if (memory.messages().size() >= SUMMARY_TRIGGER) {
            compressHistory(memory, userId);
        }

        // 3. RAG：检索企业知识，注入到Memory上下文
        String knowledgeContext = retrieveKnowledge(userMessage);
        if (!knowledgeContext.isEmpty()) {
            memory.add(SystemMessage.from(
                    "【企业知识库检索结果】\n" + knowledgeContext +
                            "\n请根据上述知识库内容回答用户问题。"
            ));
        }

        // 4. 获取或创建Chain
        ConversationalChain chain = chains.computeIfAbsent(userId, id ->
                ConversationalChain.builder()
                        .chatModel(model)
                        .chatMemory(memory)
                        .build()
        );

        // 5. 执行对话
        String answer = chain.execute(userMessage);

        return new ServiceResponse(answer, memory.messages().size());
    }

    /**
     * 从企业知识库检索相关内容（RAG核心）
     */
    private String retrieveKnowledge(String query) {
        if (knowledgeBase == null || embeddingModel == null) {
            return "";
        }
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(MAX_KNOWLEDGE_RESULTS)
                    .minScore(0.5)
                    .build();
            EmbeddingSearchResult<TextSegment> searchResult = knowledgeBase.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            if (matches.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < matches.size(); i++) {
                EmbeddingMatch<TextSegment> match = matches.get(i);
                sb.append(String.format("[知识%d] %s（相关度：%.2f）\n",
                        i + 1,
                        match.embedded().text(),
                        match.score()));
            }
            return sb.toString();

        } catch (Exception e) {
            System.out.println("知识检索异常：" + e.getMessage());
            return "";
        }
    }

    /**
     * 摘要压缩：当对话太长时，用AI提取关键信息
     */
    private void compressHistory(ChatMemory memory, String userId) {
        System.out.println(">>> 触发摘要压缩，当前消息数：" + memory.messages().size());

        String historyText = formatMessages(memory.messages());
        String summaryPrompt = String.format(SUMMARY_PROMPT, historyText);
        String summary = model.chat(
                ChatRequest.builder()
                        .messages(UserMessage.from(summaryPrompt))
                        .build()
        ).aiMessage().text();

        System.out.println(">>> 摘要生成完成：" + summary);

        memory.clear();
        memory.add(SystemMessage.from(
                "【会话摘要】" + summary + "\n基于以上信息，继续回答用户问题。"
        ));
        store.updateMessages(userId, memory.messages());
    }

    private String formatMessages(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            if (msg instanceof UserMessage userMsg) {
                sb.append("用户：").append(userMsg.singleText()).append("\n");
            } else if (msg instanceof AiMessage aiMsg) {
                sb.append("客服：").append(aiMsg.text()).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 知识库初始化：向量化存入企业知识
     */
    public void initializeKnowledgeBase(List<String> knowledgeDocs) {
        if (knowledgeBase == null || embeddingModel == null) {
            System.out.println("警告：知识库未配置，跳过初始化");
            return;
        }
        for (String doc : knowledgeDocs) {
            TextSegment segment = TextSegment.from(doc);
            Embedding embedding = embeddingModel.embed(doc).content();
            knowledgeBase.add(embedding, segment);
        }
        System.out.println(">>> 知识库初始化完成，共加载 " + knowledgeDocs.size() + " 条知识");
    }

    /**
     * 对话响应封装
     */
    public record ServiceResponse(String answer, int memorySize) {}

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("embo-01")
                .build();

        EmbeddingStore<TextSegment> knowledgeBase = new InMemoryEmbeddingStore<>();
        ChatMemoryStore store = new InMemoryChatMemoryStore();

        SmartCustomerService service = new SmartCustomerService(
                model, store, knowledgeBase, embeddingModel);

        // RAG：初始化企业知识库
        List<String> knowledgeDocs = Arrays.asList(
                "退换货政策：自收到商品之日起7天内可申请无理由退货，需保持商品完好不影响二次销售。",
                "退货流程：联系客服申请 -> 填写退货单 -> 寄回商品 -> 收到退款后完成。",
                "优惠券规则：新人首单满100减20；满200减50；不可叠加使用。",
                "发货时间：下单后24小时内发货；节假日延至48小时内。",
                "快递说明：默认顺丰速运；偏远地区发邮政EMS；生鲜品类使用冷链配送。",
                "会员积分：每消费1元积1分；积分可兑换优惠券或礼品；100积分=1元。",
                "客服时间：人工客服9:00-21:00；智能客服24小时在线。",
                "投诉渠道：拨打400热线 / 联系在线客服 / 发送邮件至 service@example.com"
        );
        service.initializeKnowledgeBase(knowledgeDocs);

        String userId = "user_test_001";

        String[] questions = {
                "我想买一件T恤",
                "有黑色的吗？",
                "XL码有货吗？",
                "多少钱？",
                "支持退货吗？",
                "几天能到？",
                "可以用优惠券吗？",
                "有运费险吗？",
                "怎么联系人工客服？",
                "谢谢",
                "还有什么优惠活动？",
                "新用户有福利吗？",
                "会员有什么权益？",
                "怎么成为会员？",
                "你们的实体店在哪？",
                "可以门店自提吗？",
        };

        for (String q : questions) {
            ServiceResponse r = service.handle(userId, q);
            System.out.println("用户：" + q);
            System.out.println("客服：" + r.answer());
            System.out.println("（当前记忆条数：" + r.memorySize() + "）");
            System.out.println();
        }
    }
}
