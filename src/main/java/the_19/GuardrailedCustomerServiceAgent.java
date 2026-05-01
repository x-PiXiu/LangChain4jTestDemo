package the_19;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 带 Guardrails 的客服 Agent（Section 7.2 完整实现）
 *
 * Guardrail 管道流程：
 *   输入 → LLM 处理 → OutputParser 结构化 → PII 脱敏 → 内容审核（异步）→ 边界检查 → 返回
 *
 * 修复说明（相对文档原文）：
 * 1. 添加了 Logger 声明（原文 log 未定义）
 * 2. chat() 方法添加了 conversationId 参数（原文未定义该变量）
 * 3. 添加了构造函数注入所有 final 字段
 * 4. JsonOutputParser 改为 PojoOutputParser（LangChain4j 1.0.0 实际类名）
 * 5. OutputParseException 改为 OutputParsingException（LangChain4j 1.0.0 实际类名）
 * 6. format() 方法不存在于 OutputParser 接口，已移除
 */
public class GuardrailedCustomerServiceAgent {

    private static final Logger log = Logger.getLogger(GuardrailedCustomerServiceAgent.class.getName());
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final CustomerServiceAi ai;
    private final PIIRedactor piiRedactor;
    private final ContentModerator contentModerator;
    private final GuardrailAuditLog auditLog;
    private final ObjectMapper objectMapper;

    public GuardrailedCustomerServiceAgent(
            CustomerServiceAi ai,
            PIIRedactor piiRedactor,
            ContentModerator contentModerator,
            GuardrailAuditLog auditLog) {
        this.ai = ai;
        this.piiRedactor = piiRedactor;
        this.contentModerator = contentModerator;
        this.auditLog = auditLog;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 管道式调用：完整的 Guardrails 处理流程
     *
     * @param conversationId 会话 ID（用于审计追踪）
     * @param userId         用户 ID
     * @param userMessage    用户消息
     * @return Agent 响应
     */
    public AgentResponse chat(String conversationId, String userId, String userMessage) {
        String rawResponse = null;
        GuardrailInterceptEvent interceptEvent = null;

        try {
            // 第 1 步：调用 Agent（获取原始回复）
            rawResponse = ai.chat(userMessage);

            // 第 2 步：OutputParser（结构化）
            ServiceResult result;
            try {
                result = objectMapper.readValue(rawResponse, ServiceResult.class);
            } catch (Exception e) {
                // JSON 解析失败 → 降级：返回纯文本
                log.warning("结构化解析失败，降级为文本回复. error=" + e.getMessage());
                result = ServiceResult.fallback(rawResponse);
            }

            // 第 3 步：PII 脱敏（对结构化字段逐个处理）
            result.setMessage(piiRedactor.redact(result.getMessage()));
            result.setDescription(piiRedactor.redact(result.getDescription()));

            // 第 4 步：内容审核（异步，不阻塞返回）
            final String capturedRaw = rawResponse;
            final ServiceResult capturedResult = result;
            contentModerator.moderateAsync(result.getMessage())
                .thenAccept(moderation -> {
                    if (!moderation.isClean()) {
                        // 触发拦截 → 记录并降级
                        auditLog.log(new GuardrailInterceptEvent(
                            conversationId, userId, Instant.now(),
                            capturedRaw, moderation.getViolatedContent(),
                            "CONTENT_MODERATION", "CONTENT", "REDACT",
                            capturedResult.getMessage()
                        ));
                        capturedResult.setMessage("[当前内容正在审核中，请稍后再试]");
                    }
                });

            // 第 5 步：边界检查（长度超限截断）
            if (result.getMessage().length() > MAX_MESSAGE_LENGTH) {
                interceptEvent = new GuardrailInterceptEvent(
                    conversationId, userId, Instant.now(),
                    rawResponse,
                    result.getMessage().substring(MAX_MESSAGE_LENGTH),
                    "MAX_LENGTH", "LENGTH", "TRUNCATE",
                    result.getMessage().substring(0, MAX_MESSAGE_LENGTH)
                );
                result.setMessage(result.getMessage().substring(0, MAX_MESSAGE_LENGTH));
            }

            // 记录拦截事件
            if (interceptEvent != null) {
                auditLog.log(interceptEvent);
            }

            return AgentResponse.success(result);

        } catch (Exception e) {
            log.severe("Agent 调用异常. userId=" + userId + ", error=" + e.getMessage());
            return AgentResponse.error("服务暂时不可用，请稍后再试");
        }
    }

    /** 便捷方法：自动生成 conversationId */
    public AgentResponse chat(String userId, String userMessage) {
        return chat(UUID.randomUUID().toString(), userId, userMessage);
    }
}
