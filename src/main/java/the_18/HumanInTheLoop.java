package the_18;

import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * Human-in-the-Loop 实现 - 对应 LangGraph 的 interrupt()/resume
 * 用于在关键节点暂停等待人类审批
 */
public class HumanInTheLoop {

    private final NegotiationContext context;
    private final BlockingQueue<ApprovalResult> approvalQueue;
    private final Scanner scanner;
    private ExecutorService executor;

    public HumanInTheLoop(NegotiationContext context) {
        this.context = context;
        this.approvalQueue = new LinkedBlockingQueue<>();
        this.scanner = new Scanner(System.in);
    }

    /**
     * 暂停等待审批 - 对应 LangGraph 的 interrupt()
     * 这会让状态机暂停，直到人类做出决策
     */
    public void pauseForApproval(String proposalSummary) {
        System.out.println("\n========================================");
        System.out.println("    [HiTL Interrupt] 谈判暂停，等待审批");
        System.out.println("========================================");
        System.out.println("提案摘要: " + proposalSummary);
        System.out.println("预算: " + context.getBudget() + " 元");
        System.out.println("当前轮次: " + context.round);
        System.out.println("----------------------------------------");

        // 1. 保存检查点
        context.checkpoint();

        // 2. 启动异步监听器监听审批结果
        startApprovalListener();

        // 3. 模拟发送通知（实际项目中会发短信/邮件/推送）
        sendApprovalNotification(proposalSummary);

        // 4. 同步等待人类响应
        System.out.println("[HiTL] 请输入审批结果 (通过/拒绝)，或 '修改' 重新协商:");
        System.out.print("> ");

        String input = scanner.nextLine().trim();
        ApprovalResult result = processHumanInput(input);

        // 5. 将结果放入队列，唤醒等待的线程
        approvalQueue.offer(result);

        // 6. 根据审批结果更新状态
        updateStateFromApproval(result);

        System.out.println("========================================\n");
    }

    /**
     * 处理人类输入
     */
    private ApprovalResult processHumanInput(String input) {
        if (input.isEmpty()) {
            return new ApprovalResult(false, "超时未响应");
        }

        return switch (input) {
            case "通过", "同意", "是", "ok", "OK" -> new ApprovalResult(true, "用户批准");
            case "拒绝", "不要", "否", "no", "NO" -> new ApprovalResult(false, "用户拒绝");
            case "修改", "重新协商", "降价" -> new ApprovalResult(false, "需要修改", true);
            default -> new ApprovalResult(false, "未知输入，默认拒绝");
        };
    }

    /**
     * 启动异步审批监听器
     */
    private void startApprovalListener() {
        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            // 这个监听器可以接收异步审批结果（如移动App审批）
            // 实际项目中会从消息队列或WebSocket接收
        });
    }

    /**
     * 发送审批通知
     */
    private void sendApprovalNotification(String proposalSummary) {
        System.out.println("[通知] 审批请求已发送:");
        System.out.println("  - 谈判ID: " + context.negotiationId);
        System.out.println("  - 提案摘要: " + proposalSummary);
        System.out.println("  - 预计响应时间: 24小时");
    }

    /**
     * 根据审批结果更新状态
     */
    private void updateStateFromApproval(ApprovalResult result) {
        NegotiationState previousState = context.state;

        if (result.isApproved()) {
            context.state = NegotiationState.ACCEPTED;
            context.addHistory("【审批通过】人类批准了提案");
        } else if (result.isNeedsModification()) {
            context.state = NegotiationState.MODIFIED;
            context.addHistory("【需要修改】人类要求重新协商");
        } else {
            context.state = NegotiationState.REJECTED;
            context.addHistory("【审批拒绝】人类拒绝了提案");
        }

        context.addHistory("审批决策: " + result.getReason());
        context.checkpoint();
    }

    /**
     * 获取当前等待的审批（用于异步场景）
     */
    public BlockingQueue<ApprovalResult> getApprovalQueue() {
        return approvalQueue;
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
        scanner.close();
    }

    /**
     * 审批结果内部类
     */
    public static class ApprovalResult {
        private final boolean approved;
        private final boolean needsModification;
        private final String reason;

        public ApprovalResult(boolean approved) {
            this(approved, null, false);
        }

        public ApprovalResult(boolean approved, String reason) {
            this(approved, reason, false);
        }

        public ApprovalResult(boolean approved, String reason, boolean needsModification) {
            this.approved = approved;
            this.reason = reason;
            this.needsModification = needsModification;
        }

        public boolean isApproved() {
            return approved;
        }

        public boolean isNeedsModification() {
            return needsModification;
        }

        public String getReason() {
            return reason != null ? reason : (approved ? "批准" : "拒绝");
        }
    }
}
