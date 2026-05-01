package the_18;

/**
 * 谈判状态枚举
 * 对应 LangGraph 中的 State 概念
 */
public enum NegotiationState {
    INITIAL,            // 初始状态
    PROPOSAL_GENERATED, // 提案已生成
    UNDER_REVIEW,       // 等待审批（中断点）
    ACCEPTED,           // 谈判成功
    REJECTED,           // 谈判失败
    MODIFIED            // 提案已修改（回到 PROPOSAL_GENERATED）
}
