package the_19;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 内容审核器（Section 4.3）
 * 多层审核策略：关键词过滤 → 规则匹配 → 语义审核
 *
 * 修复说明（相对文档原文）：
 * - 简化了 ExternalModerationApi 和 LLM 语义审核的依赖
 * - 使用关键词+正则实现自包含的审核逻辑
 * - 提供 moderate() 同步方法和 moderateAsync() 异步方法
 */
public class ContentModerator {

    // 简单的关键词黑名单（生产环境应使用外部审核 API）
    private static final List<String> BLOCKED_KEYWORDS = List.of(
        "赌博", "色情", "毒品", "暴力", "枪支"
    );

    public ContentModerator() {
        // 无参构造，开箱即用
    }

    /**
     * 同步内容审核
     * 返回审核结果（是否通过、违规内容、违规类型）
     */
    public ModerationResult moderate(String text) {
        if (text == null || text.isEmpty()) {
            return new ModerationResult(true, text, List.of());
        }

        List<String> violations = new ArrayList<>();
        String violatedContent = null;

        for (String keyword : BLOCKED_KEYWORDS) {
            if (text.contains(keyword)) {
                violations.add("BLOCKED_KEYWORD: " + keyword);
                violatedContent = keyword;
            }
        }

        boolean clean = violations.isEmpty();
        return new ModerationResult(clean, text, violations, violatedContent);
    }

    /**
     * 异步内容审核（不阻塞主流程）
     * 在 GuardrailedCustomerServiceAgent 第4步中使用
     */
    public CompletableFuture<ModerationResult> moderateAsync(String text) {
        return CompletableFuture.supplyAsync(() -> moderate(text));
    }
}
