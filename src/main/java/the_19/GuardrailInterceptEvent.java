package the_19;

import java.time.Instant;

/**
 * Guardrail 拦截事件记录（Section 6.1）
 * 记录每次越界拦截的完整信息，用于统计分析与审计合规
 */
public class GuardrailInterceptEvent {

    private final String conversationId;
    private final String userId;
    private final Instant timestamp;
    private final String rawOutput;          // AI 原始输出
    private final String interceptedContent;  // 被拦截的内容
    private final String triggeredRule;       // 触发的规则名称
    private final String ruleType;            // PII / CONTENT / LENGTH / ENUM
    private final String action;              // REDACT / BLOCK / TRUNCATE
    private final String safeOutput;          // 拦截后的安全输出

    public GuardrailInterceptEvent(
            String conversationId,
            String userId,
            Instant timestamp,
            String rawOutput,
            String interceptedContent,
            String triggeredRule,
            String ruleType,
            String action,
            String safeOutput) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.timestamp = timestamp;
        this.rawOutput = rawOutput;
        this.interceptedContent = interceptedContent;
        this.triggeredRule = triggeredRule;
        this.ruleType = ruleType;
        this.action = action;
        this.safeOutput = safeOutput;
    }

    public String getConversationId() { return conversationId; }
    public String getUserId() { return userId; }
    public Instant getTimestamp() { return timestamp; }
    public String getRawOutput() { return rawOutput; }
    public String getInterceptedContent() { return interceptedContent; }
    public String getTriggeredRule() { return triggeredRule; }
    public String getRuleType() { return ruleType; }
    public String getAction() { return action; }
    public String getSafeOutput() { return safeOutput; }

    /** 用于统计分析和日志输出 */
    public String toLogString() {
        return String.format(
            "[%s] conversation=%s | rule=%s | type=%s | action=%s",
            timestamp, conversationId, triggeredRule, ruleType, action
        );
    }

    @Override
    public String toString() {
        return toLogString();
    }
}
