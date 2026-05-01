package the_18;

import java.util.Map;
import java.util.Set;

/**
 * 谈判状态机 - 模拟 LangGraph 的 State Graph
 * 定义状态转换规则和条件边逻辑
 */
public class NegotiationStateMachine {

    // 状态转换规则 - 定义合法的状态转换
    private static final Map<NegotiationState, Set<NegotiationState>> TRANSITIONS = Map.of(
        NegotiationState.INITIAL,            Set.of(NegotiationState.PROPOSAL_GENERATED),
        NegotiationState.PROPOSAL_GENERATED, Set.of(
            NegotiationState.UNDER_REVIEW,
            NegotiationState.ACCEPTED,
            NegotiationState.REJECTED,
            NegotiationState.MODIFIED
        ),
        NegotiationState.UNDER_REVIEW,        Set.of(
            NegotiationState.ACCEPTED,
            NegotiationState.REJECTED,
            NegotiationState.MODIFIED
        ),
        NegotiationState.MODIFIED,            Set.of(NegotiationState.PROPOSAL_GENERATED)
    );

    private final LlmDecisionMaker llmDecisionMaker;

    public NegotiationStateMachine(LlmDecisionMaker llmDecisionMaker) {
        this.llmDecisionMaker = llmDecisionMaker;
    }

    /**
     * 执行状态转换 - 对应 LangGraph 的边执行
     */
    public NegotiationState transition(NegotiationContext context, String userMessage) {
        NegotiationState current = context.state;
        Set<NegotiationState> allowed = TRANSITIONS.get(current);

        if (allowed == null || allowed.isEmpty()) {
            throw new IllegalStateException("当前状态 " + current + " 没有可用的转换");
        }

        // LLM 决定下一步 - 对应 LangGraph 的 ConditionalEdge
        String llmDecision = llmDecisionMaker.decideNext(context, userMessage);
        NegotiationState next = parseDecision(llmDecision, current, allowed);

        // 验证转换合法性
        if (!allowed.contains(next)) {
            System.out.println("    [Warning] LLM 决策 " + llmDecision + " -> " + next + " 不在允许的转换中");
            next = allowed.iterator().next(); // fallback 到第一个允许的状态
        }

        context.state = next;
        context.addHistory("状态转换: " + current + " -> " + next + " (LLM决策: " + llmDecision + ")");

        return next;
    }

    /**
     * 解析 LLM 决策为具体状态
     * 对应 LangGraph ConditionalEdge 的路由逻辑
     */
    private NegotiationState parseDecision(String llmDecision,
                                            NegotiationState current,
                                            Set<NegotiationState> allowed) {
        if (llmDecision == null) {
            return allowed.iterator().next();
        }

        return switch (llmDecision.toUpperCase().trim()) {
            case "ACCEPT", "ACCEPTED" -> NegotiationState.ACCEPTED;
            case "REJECT", "REJECTED" -> NegotiationState.REJECTED;
            case "MODIFY", "MODIFIED" -> NegotiationState.MODIFIED;
            case "REVIEW", "UNDER_REVIEW" -> NegotiationState.UNDER_REVIEW;
            case "PROPOSE", "PROPOSAL_GENERATED" -> NegotiationState.PROPOSAL_GENERATED;
            case "CONTINUE" -> {
                // 根据当前状态决定继续的方向
                if (current == NegotiationState.INITIAL) {
                    yield NegotiationState.PROPOSAL_GENERATED;
                } else if (current == NegotiationState.MODIFIED) {
                    yield NegotiationState.PROPOSAL_GENERATED;
                }
                yield allowed.iterator().next();
            }
            default -> {
                System.out.println("    [Warning] 未知的 LLM 决策: " + llmDecision + ", 使用默认转换");
                yield allowed.iterator().next();
            }
        };
    }

    /**
     * 检查是否可以进行转换
     */
    public boolean canTransition(NegotiationState from, NegotiationState to) {
        Set<NegotiationState> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * 获取当前状态允许的所有转换
     */
    public Set<NegotiationState> getAllowedTransitions(NegotiationState state) {
        return TRANSITIONS.getOrDefault(state, Set.of());
    }
}
