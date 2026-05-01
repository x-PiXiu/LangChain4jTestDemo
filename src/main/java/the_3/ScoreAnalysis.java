package the_3;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class ScoreAnalysis {

    private final ChatModel model;

    public ScoreAnalysis(ChatModel model) {
        this.model = model;
    }

    public String analyzeScore(int score, int totalStudents) {
        // 构建带JSON格式要求的SystemMessage
        SystemMessage systemMessage = SystemMessage.from("""
            你是一个学生成绩分析助手。
            
            分析学生的考试成绩，严格按照以下JSON格式输出：
            {
                "score": number,           // 分数（0-100）
                "grade": string,            // 等级：优秀/良好/及格/不及格
                "percentile": number,      // 百分位排名（0-100）
                "suggestion": string       // 学习建议
            }
            
            判定规则：
            - >=90分：优秀
            - >=75分：良好  
            - >=60分：及格
            - <60分：不及格
            
            注意：只输出JSON，不要其他任何内容！
            """);

        UserMessage userMessage = UserMessage.from(
                String.format("学生考了%d分，班级共%d人", score, totalStudents)
        );

        ChatRequest request = ChatRequest.builder()
                .messages(systemMessage, userMessage)
                .build();

        ChatResponse response = model.chat(request);
        return response.aiMessage().text();
    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        ScoreAnalysis analyzer = new ScoreAnalysis(model);
        String jsonResult = analyzer.analyzeScore(78, 50);
        System.out.println("AI返回：" + jsonResult);

        // 用Jackson解析JSON
        // ObjectMapper mapper = new ObjectMapper();
        // ScoreResult result = mapper.readValue(jsonResult, ScoreResult.class);
    }
}
