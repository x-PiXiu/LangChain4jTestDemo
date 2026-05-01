package the_18;

/**
 * LLM 决策接口 - 模拟 LangGraph 的 ConditionalEdge
 * 实际项目中会调用真实的 LLM API
 */
public interface LlmDecisionMaker {

    /**
     * 根据上下文和用户消息，决定下一步操作
     * @param context 谈判上下文
     * @param userMessage 用户消息
     * @return 决策结果 (ACCEPT/REJECT/MODIFY/REVIEW/CONTINUE)
     */
    String decideNext(NegotiationContext context, String userMessage);
}
