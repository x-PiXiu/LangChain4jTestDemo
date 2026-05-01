package the_15;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.List;

public class KnowledgeTool {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public KnowledgeTool(EmbeddingStore<TextSegment> store, EmbeddingModel model) {
        this.embeddingStore = store;
        this.embeddingModel = model;
    }

    @Tool("从知识库中搜索与查询相关的文档片段，返回最匹配的内容")
    public String searchKnowledge(
            @P("搜索关键词或问题，如'销售报告'、'Q1数据'") String query
    ) {
        try {
            // 生成查询向量
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            // 搜索最相关的 3 个文档片段
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(
                    EmbeddingSearchRequest.builder()
                            .queryEmbedding(queryEmbedding)
                            .maxResults(3)
                            .minScore(0.5)
                            .build()
            ).matches();

            if (matches.isEmpty()) {
                return "知识库中未找到与'" + query + "'相关的内容";
            }

            StringBuilder sb = new StringBuilder("找到以下相关内容：\n");
            for (int i = 0; i < matches.size(); i++) {
                EmbeddingMatch<TextSegment> match = matches.get(i);
                sb.append(String.format("\n[片段%d]（相关度：%.0f%%）\n%s\n",
                        i + 1, match.score() * 100, match.embedded().text()));
            }
            return sb.toString();

        } catch (Exception e) {
            return "知识检索出错：" + e.getMessage();
        }
    }
}
