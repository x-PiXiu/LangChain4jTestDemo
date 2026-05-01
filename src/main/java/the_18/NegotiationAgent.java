package the_18;

import java.util.UUID;

/**
 * 多轮谈判 Agent - 主控制器
 * 模拟 LangGraph 的 Runtime
 * 对应文档7.5节的完整流程
 */
public class NegotiationAgent {

    private final NegotiationStateMachine stateMachine;
    private HumanInTheLoop humanInTheLoop;
    private NegotiationContext context;

    public NegotiationAgent(LlmDecisionMaker llmDecisionMaker) {
        this.stateMachine = new NegotiationStateMachine(llmDecisionMaker);
    }

    /**
     * 启动新的谈判 - 对应 LangGraph 的 compile().invoke()
     */
    public void start(String userRequest, long budget) {
        String negotiationId = UUID.randomUUID().toString().substring(0, 8);
        System.out.println("\n" + "=".repeat(50));
        System.out.println("    多轮谈判 Agent 启动");
        System.out.println("=".repeat(50));
        System.out.println("谈判ID: " + negotiationId);
        System.out.println("用户请求: " + userRequest);
        System.out.println("预算: " + budget + " 元");
        System.out.println();

        // 初始化上下文
        context = new NegotiationContext(negotiationId);
        context.setBudget(budget);
        context.addHistory("谈判开始: " + userRequest);

        // 创建 HiTL 实例
        humanInTheLoop = new HumanInTheLoop(context);

        // 执行谈判流程
        execute(userRequest);
    }

    /**
     * 恢复中断的谈判 - 对应 LangGraph 的 resume from checkpoint
     */
    public void resume(String negotiationId) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("    从检查点恢复谈判");
        System.out.println("=".repeat(50));

        context = NegotiationContext.restore(negotiationId);
        humanInTheLoop = new HumanInTheLoop(context);

        System.out.println("已恢复状态: " + context);
    }

    /**
     * 执行谈判流程 - 对应 LangGraph 的图执行
     */
    private void execute(String userMessage) {
        int maxRounds = 10; // 最大轮次，防止无限循环

        while (context.round < maxRounds) {
            context.incrementRound();
            System.out.println("-".repeat(40));
            System.out.println("第 " + context.round + " 轮谈判");
            System.out.println("-".repeat(40));

            // 根据当前状态执行对应的 Node
            switch (context.state) {
                case INITIAL -> handleInitialState(userMessage);
                case PROPOSAL_GENERATED -> handleProposalGeneratedState(userMessage);
                case UNDER_REVIEW -> handleUnderReviewState();
                case ACCEPTED, REJECTED -> {
                    handleFinalState();
                    return;
                }
                case MODIFIED -> handleModifiedState(userMessage);
            }

            // 检查是否结束
            if (context.state == NegotiationState.ACCEPTED ||
                context.state == NegotiationState.REJECTED) {
                break;
            }

            // 询问下一轮输入
            System.out.print("\n[用户] 请输入回复 (或 '退出' 结束): ");
        }

        if (context.round >= maxRounds) {
            System.out.println("[系统] 达到最大轮次限制，谈判结束");
            context.state = NegotiationState.REJECTED;
        }
    }

    /**
     * [1] INITIAL -> PROPOSAL_GENERATED
     * 对应文档7.5节的步骤[1]
     */
    private void handleInitialState(String userMessage) {
        System.out.println("[Node] generate_proposal - 生成提案");

        // 生成提案
        long budget = Long.parseLong(context.getBudget().toString());
        String proposal = generateProposal(budget, userMessage);
        context.setProposal(proposal);

        context.addHistory("生成提案: " + proposal);
        System.out.println("[提案] " + proposal);

        // 执行状态转换: INITIAL -> PROPOSAL_GENERATED
        stateMachine.transition(context, userMessage);
    }

    /**
     * [2] PROPOSAL_GENERATED -> UNDER_REVIEW / ACCEPTED / REJECTED / MODIFIED
     * 对应文档7.5节的步骤[2]
     */
    private void handleProposalGeneratedState(String userMessage) {
        System.out.println("[Node] evaluate_proposal - 评估提案");
        System.out.println("[边缘路由] LLM ConditionalEdge 决定下一步");

        long budget = Long.parseLong(context.getBudget().toString());
        long proposalAmount = extractProposalAmount(context.getProposal());

        // LLM 决策 - 是否需要审批
        NegotiationState nextState = stateMachine.transition(context, userMessage);

        System.out.println("[LLM决策] " + nextState);

        if (context.state == NegotiationState.UNDER_REVIEW) {
            // 提案超出预算，触发 HiTL 中断
            System.out.println("[中断] 提案超出预算，触发 Human-in-the-Loop");
        }
    }

    /**
     * [3] UNDER_REVIEW - 等待人类审批
     * 对应文档7.5节的步骤[3]
     */
    private void handleUnderReviewState() {
        System.out.println("[Node] human_approval - 等待人类审批");
        humanInTheLoop.pauseForApproval(context.getProposal());
    }

    /**
     * [4] ACCEPTED / REJECTED - 谈判结束
     * 对应文档7.5节的步骤[4]
     */
    private void handleFinalState() {
        System.out.println("[Node] finalize - 结束谈判");

        if (context.state == NegotiationState.ACCEPTED) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("    谈判成功！");
            System.out.println("=".repeat(50));
            System.out.println("最终提案: " + context.getProposal());
            context.addHistory("谈判成功！");
        } else {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("    谈判破裂");
            System.out.println("=".repeat(50));
            context.addHistory("谈判失败/拒绝");
        }

        printHistory();
    }

    /**
     * MODIFIED -> 重新生成提案
     */
    private void handleModifiedState(String userMessage) {
        System.out.println("[Node] modify_proposal - 修改提案");

        // 重新生成更优惠的提案
        long budget = Long.parseLong(context.getBudget().toString());
        long currentProposal = extractProposalAmount(context.getProposal());
        long newProposal = (long) (currentProposal * 0.9); // 降价10%

        String proposal = String.format("【修订提案】服务器采购方案 - 总价: %d 元 (原: %d 元, 降价 %d 元)",
                newProposal, currentProposal, currentProposal - newProposal);
        context.setProposal(proposal);

        context.addHistory("修订提案: " + proposal);
        System.out.println("[新提案] " + proposal);

        // 转换到 PROPOSAL_GENERATED
        stateMachine.transition(context, userMessage);
    }

    /**
     * 生成提案 - 对应 LangGraph 的 Node
     */
    private String generateProposal(long budget, String userRequest) {
        // 简化：提案价格略高于预算
        long proposalAmount = (long) (budget * 1.1);
        return String.format("【初始提案】服务器采购方案 - 总价: %d 元 (预算: %d 元, 超支: %d 元)",
                proposalAmount, budget, proposalAmount - budget);
    }

    private long extractProposalAmount(String proposal) {
        if (proposal == null) return 0;
        try {
            String numStr = proposal.replaceAll("[^0-9]", "");
            if (!numStr.isEmpty()) {
                return Long.parseLong(numStr.substring(0, Math.min(6, numStr.length())));
            }
        } catch (NumberFormatException e) {
            // 忽略
        }
        return 0;
    }

    private void printHistory() {
        System.out.println("\n[谈判历史]");
        for (String entry : context.history) {
            System.out.println("  " + entry);
        }
    }

    public NegotiationContext getContext() {
        return context;
    }
}
