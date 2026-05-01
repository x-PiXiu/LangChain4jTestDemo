package the_17;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.List;
import java.util.Map;

public class RetrievalEvaluator {

    private final Map<String, List<String>> groundTruth = Map.of(
            "Q1销售数据", List.of("doc_q1_sales", "doc_q1_online", "doc_q1_offline"),
            "项目评审流程", List.of("doc_review_process"),
            "退货率分析", List.of("doc_q1_returns", "doc_quality_report")
    );

    public EvaluationResult evaluate(
            String query,
            List<EmbeddingMatch<TextSegment>> searchResults) {

        List<String> relevantIds = groundTruth.getOrDefault(query, List.of());

        long relevantInResult = searchResults.stream()
                .filter(m -> relevantIds.contains(extractDocId(m)))
                .count();

        double precision = searchResults.isEmpty() ? 0 : (double) relevantInResult / searchResults.size();
        double recall = relevantIds.isEmpty() ? 0 : (double) relevantInResult / relevantIds.size();

        return new EvaluationResult(query, precision, recall, searchResults.size());
    }

    private String extractDocId(EmbeddingMatch<TextSegment> match) {
        if (match.embedded() != null && match.embedded().metadata() != null) {
            String docId = match.embedded().metadata().getString("doc_id");
            if (docId != null) return docId;
        }
        return match.embeddingId();
    }

    record EvaluationResult(String query, double precision, double recall, int k) {}
}
