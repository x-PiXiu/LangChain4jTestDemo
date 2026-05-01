package the_19;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 客服 AI 服务接口
 * 使用 LangChain4j 的 @SystemMessage 注解定义 AI 角色和行为约束
 */
@SystemMessage("""
    你是一个专业的客服助手。你的职责是：
    1. 回答用户关于订单、产品、服务的咨询
    2. 处理用户的投诉和建议
    3. 提供清晰、准确、友好的回复

    规则：
    - 不要泄露任何用户的个人信息
    - 不要讨论与客服无关的话题（如政治、宗教等）
    - 如果不确定答案，请诚实告知用户
    - 回复要简洁明了，控制在 500 字以内
    """)
public interface CustomerServiceAi {

    String chat(@UserMessage String userMessage);
}
