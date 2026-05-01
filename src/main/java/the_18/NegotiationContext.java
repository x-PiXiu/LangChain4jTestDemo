package the_18;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 谈判上下文 - 对应 LangGraph 的 State
 * 贯穿整个谈判流程的共享数据
 */
public class NegotiationContext implements Serializable {

    private static final long serialVersionUID = 1L;

    String negotiationId;
    NegotiationState state;
    Map<String, Object> sharedData;  // 对应 LangGraph 的 State
    int round;
    List<String> history;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public NegotiationContext(String negotiationId) {
        this.negotiationId = negotiationId;
        this.state = NegotiationState.INITIAL;
        this.sharedData = new HashMap<>();
        this.round = 0;
        this.history = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查点保存 - 对应 LangGraph 的 Checkpointing
     * 实际项目中使用 Redis 持久化
     */
    public void checkpoint() {
        this.updatedAt = LocalDateTime.now();
        String checkpointKey = "negotiation:checkpoint:" + negotiationId;

        // 模拟 Redis 保存
        System.out.println("    [Checkpoint] 保存状态到: " + checkpointKey);
        System.out.println("    [Checkpoint] 状态: " + state + ", 轮次: " + round);

        // 实际实现会序列化并存储到 Redis
        // redisTemplate.opsForValue().set(checkpointKey, serialize(this), Duration.ofDays(7));
    }

    /**
     * 从检查点恢复 - 对应 LangGraph 的 resume from checkpoint
     */
    public static NegotiationContext restore(String negotiationId) {
        String checkpointKey = "negotiation:checkpoint:" + negotiationId;
        System.out.println("    [Restore] 从检查点恢复: " + checkpointKey);

        // 实际实现从 Redis 读取并反序列化
        // byte[] data = redisTemplate.opsForValue().get(checkpointKey);
        // return deserialize(data);

        NegotiationContext context = new NegotiationContext(negotiationId);
        context.state = NegotiationState.UNDER_REVIEW; // 模拟恢复的状态
        return context;
    }

    public void addHistory(String entry) {
        this.history.add("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + entry);
    }

    public void incrementRound() {
        this.round++;
    }

    public String getBudget() {
        Object budget = sharedData.get("budget");
        return budget != null ? budget.toString() : "未知";
    }

    public void setBudget(long budget) {
        sharedData.put("budget", budget);
    }

    public String getProposal() {
        return (String) sharedData.get("proposal");
    }

    public void setProposal(String proposal) {
        sharedData.put("proposal", proposal);
    }

    @Override
    public String toString() {
        return "NegotiationContext{" +
                "negotiationId='" + negotiationId + '\'' +
                ", state=" + state +
                ", round=" + round +
                ", budget=" + getBudget() +
                ", proposal=" + getProposal() +
                '}';
    }
}
