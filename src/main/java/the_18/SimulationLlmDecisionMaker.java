package the_18;

import java.util.Random;

/**
 * 模拟 LLM 决策实现 - 用于演示
 * 实际项目中替换为真实的 LLM API 调用
 */
public class SimulationLlmDecisionMaker implements LlmDecisionMaker {

    private final Random random = new Random();
    private final boolean simulateLlmCall;

    public SimulationLlmDecisionMaker(boolean simulateLlmCall) {
        this.simulateLlmCall = simulateLlmCall;
    }

    @Override
    public String decideNext(NegotiationContext context, String userMessage) {
        if (simulateLlmCall) {
            // 模拟 LLM API 调用延迟
            simulateLlmLatency();
        }

        return analyzeAndDecide(context, userMessage);
    }

    private void simulateLlmLatency() {
        try {
            // 模拟 LLM 调用延迟 100-500ms
            Thread.sleep(100 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 分析用户消息和上下文，决定下一步
     * 这模拟了 LLM 的推理能力
     */
    private String analyzeAndDecide(NegotiationContext context, String userMessage) {
        String msg = userMessage.toLowerCase();

        // 用户明确接受
        if (msg.contains("行") || msg.contains("好的") || msg.contains("同意") ||
            msg.contains("就这个") || msg.contains("成交") || msg.contains("可以")) {
            return "ACCEPT";
        }

        // 用户明确拒绝
        if (msg.contains("算了") || msg.contains("不要") || msg.contains("拒绝") ||
            msg.contains("不行") || msg.contains("算了")) {
            return "REJECT";
        }

        // 用户要求修改或讨价还价
        if (msg.contains("便宜") || msg.contains("降价") || msg.contains("降低") ||
            msg.contains("修改") || msg.contains("能不能") || msg.contains("再想想")) {
            return "MODIFY";
        }

        // 预算检查
        String proposal = context.getProposal();
        if (proposal != null) {
            long budget = Long.parseLong(context.getBudget().toString());
            // 从提案中提取金额（简化处理）
            long proposalAmount = extractAmount(proposal);

            if (proposalAmount > budget) {
                return "REVIEW"; // 超预算，需要审批
            }
        }

        // 默认继续谈判
        return "CONTINUE";
    }

    private long extractAmount(String proposal) {
        // 简化：从提案文本中提取数字
        // 实际项目中应该让 LLM 解析结构化数据
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
}
