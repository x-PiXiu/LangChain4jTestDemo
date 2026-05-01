package the_3;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能Java代码审查助手
 *
 * 功能：
 * 1. 根据代码质量问题评分（0-100）
 * 2. 识别性能、安全、规范三类问题
 * 3. 返回结构化JSON结果
 *
 * 使用方法：
 *   the_3.JavaCodeReviewer reviewer = new the_3.JavaCodeReviewer(model);
 *   String result = reviewer.review(code);
 */
public class JavaCodeReviewer {

    private final ChatModel model;

    public JavaCodeReviewer(ChatModel model) {
        this.model = model;
    }

    /**
     * 审查代码
     * @param code 要审查的Java代码
     * @return JSON格式的审查结果
     */
    public String review(String code) {
        // 1. 构建SystemMessage：角色+规则+输出格式
        SystemMessage systemMessage = SystemMessage.from("""
            你是一位资深的Java架构师，负责代码审查。
            
            审查规则：
            1. 只输出JSON格式，不要其他任何内容
            2. 评分范围0-100，低于60分必须有严重问题
            3. 问题分为三类：性能问题、安全隐患、代码规范
            4. 每类问题给出具体行号（用代码的实际行号）和修复建议
            
            JSON输出格式：
            {
                "score": number,           // 总分（0-100）
                "summary": string,          // 总体评价
                "issues": [                 // 问题列表（可为空）
                    {
                        "type": string,     // 性能问题/安全隐患/代码规范
                        "line": number,     // 问题所在行号
                        "description": string,  // 问题描述
                        "suggestion": string    // 修复建议
                    }
                ]
            }
            
            如果代码没有问题或只有轻微问题，score为90-100，issues可为空数组。
            """);

        // 2. 构建用户消息
        UserMessage userMessage = UserMessage.from("请审查以下Java代码：\n" + code);

        // 3. 构建请求
        ChatRequest request = ChatRequest.builder()
                .messages(systemMessage, userMessage)
                .build();

        // 4. 发送并获取响应
        ChatResponse response = model.chat(request);

        // 5. 返回AI回复文本
        return response.aiMessage().text();
    }

    /**
     * 多轮审查：支持追问
     */
    public String reviewWithFollowUp(String code, String followUpQuestion) {
        List<ChatMessage> messages = new ArrayList<>();

        // 系统消息
        messages.add(SystemMessage.from("""
            你是一位资深的Java架构师，负责代码审查。
            
            审查规则：
            1. 只输出JSON格式，不要其他任何内容
            2. 评分范围0-100，低于60分必须有严重问题
            3. JSON格式：{"score": number, "summary": string, "issues": [...]}
            
            如果代码没有问题或只有轻微问题，score为90-100，issues可为空数组。
            """));

        // 第一轮：代码审查
        messages.add(UserMessage.from("请审查以下Java代码：\n" + code));

        ChatRequest request1 = ChatRequest.builder()
                .messages(new ArrayList<>(messages))
                .build();
        ChatResponse response1 = model.chat(request1);
        String reviewResult = response1.aiMessage().text();

        System.out.println("=== 代码审查结果 ===");
        System.out.println(reviewResult);

        // 第二轮：追问
        messages.add(response1.aiMessage());
        messages.add(UserMessage.from(followUpQuestion));

        ChatRequest request2 = ChatRequest.builder()
                .messages(new ArrayList<>(messages))
                .build();
        ChatResponse response2 = model.chat(request2);

        return response2.aiMessage().text();
    }

    public static void main(String[] args) {
        // 1. 初始化模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        // 2. 创建审查器
        JavaCodeReviewer reviewer = new JavaCodeReviewer(model);

        // 3. 要审查的代码（有问题的版本）
        String badCode = """
            public class UserService {
                public String getUserById(String id) {
                    String sql = "SELECT * FROM users WHERE id = " + id;
                    return jdbcTemplate.queryForObject(sql);
                }
                
                public List<Object> getAllUsers() {
                    return jdbcTemplate.queryForList("SELECT * FROM users");
                }
            }
            """;

        // 4. 单轮审查
        System.out.println("=== 单轮审查 ===");
        String result = reviewer.review(badCode);
        System.out.println(result);

        // 5. 多轮审查（追问）
        System.out.println("\n=== 多轮审查（追问） ===");
        String followUp = reviewer.reviewWithFollowUp(
                badCode,
                "针对第三个问题，能给我一个修复后的代码示例吗？"
        );
        System.out.println(followUp);
    }
}
