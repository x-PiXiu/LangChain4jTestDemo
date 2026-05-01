package the_18;

import java.util.Scanner;

/**
 * 多轮谈判 Agent 演示入口
 * 对应文档中的【实战】用状态机实现"多轮谈判 Agent"（Java 版）
 *
 * 演示场景（文档7.5节）：
 * 用户：请帮我起草服务器采购合同，预算50万
 *   ↓
 * [1] 状态：INITIAL
 *     → 生成提案 → 状态变为 PROPOSAL_GENERATED
 *   ↓
 * [2] 状态：PROPOSAL_GENERATED
 *     → LLM 评估：预算内？ → [是] → ACCEPTED
 *     → LLM 评估：预算内？ → [否] → UNDER_REVIEW（中断，等审批）
 *   ↓
 * [3] 状态：UNDER_REVIEW（checkpoint 已保存）
 *     ← 人类收到通知，点击"通过"
 *     ← 状态恢复为 ACCEPTED
 *   ↓
 * [4] 状态：ACCEPTED
 *     → 生成最终合同 → 完成
 */
public class NegotiationDemo {

    public static void main(String[] args) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║     LangChain4j 天花板 - 多轮谈判 Agent (Java版)          ║");
        System.out.println("║     模拟 LangGraph 的 State + Node + Edge 架构           ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();

        // 选择演示场景
        if (args.length > 0 && args[0].equals("auto")) {
            runAutoDemo();
        } else {
            runInteractiveDemo();
        }
    }

    /**
     * 交互式演示 - 用户输入
     */
    private static void runInteractiveDemo() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("【演示场景】服务器采购谈判");
        System.out.println("甲方: 我想采购一批服务器，预算50万");
        System.out.println();

        // 创建 Agent（使用模拟 LLM）
        NegotiationAgent agent = new NegotiationAgent(new SimulationLlmDecisionMaker(true));

        // 模拟多轮对话
        String[] userMessages = {
            "我想采购一批服务器，预算50万",  // 第1轮
            "CPU能升一下吗？贵10万",        // 第2轮
            "能不能便宜点",                  // 第3轮
            "行，就这个"                     // 第4轮 - 接受
        };

        int round = 0;
        while (round < userMessages.length) {
            System.out.print("\n[系统] 按 Enter 开始第 " + (round + 1) + " 轮谈判...");
            scanner.nextLine();

            String userMessage = userMessages[round];
            System.out.println("\n>>> 用户: " + userMessage);

            if (round == 0) {
                agent.start(userMessage, 500000);
            }

            round++;
        }

        System.out.println("\n【演示完成】");
        System.out.println("\n这个演示展示了:");
        System.out.println("  1. State 贯穿全程 - 跨轮次保持状态");
        System.out.println("  2. ConditionalEdge - LLM 决定下一步");
        System.out.println("  3. Checkpoint - 中断恢复机制");
        System.out.println("  4. Human-in-the-Loop - 暂停等审批");

        scanner.close();
    }

    /**
     * 自动演示 - 直接执行完整流程
     */
    private static void runAutoDemo() {
        System.out.println("【自动演示模式】");
        System.out.println("========================================");
        System.out.println("演示场景：服务器采购多轮谈判");
        System.out.println("甲方预算：50万元");
        System.out.println("========================================\n");

        // 场景1：正常接受
        System.out.println("\n>>> 场景1：甲方直接接受提案\n");
        runScenario1();

        System.out.println("\n\n");

        // 场景2：超预算，需要审批
        System.out.println(">>> 场景2：提案超预算，触发HiTL审批\n");
        runScenario2();
    }

    /**
     * 场景1：正常谈判流程
     */
    private static void runScenario1() {
        NegotiationAgent agent = new NegotiationAgent(new SimulationLlmDecisionMaker(true));

        // 第1轮：INITIAL -> PROPOSAL_GENERATED
        System.out.println("[第1轮] 用户：请帮我起草服务器采购合同，预算50万");
        agent.start("请帮我起草服务器采购合同，预算50万", 500000);
    }

    /**
     * 场景2：超预算触发HiTL
     */
    private static void runScenario2() {
        // 使用特殊的 DecisionMaker 来模拟超预算场景
        NegotiationAgent agent = new NegotiationAgent((context, userMessage) -> {
            // 强制触发 UNDER_REVIEW 状态
            if (context.round == 1) {
                return "REVIEW";
            }
            return "ACCEPT";
        });

        System.out.println("[第1轮] 用户：请帮我起草服务器采购合同，预算50万");
        agent.start("请帮我起草服务器采购合同，预算50万", 500000);
    }
}
