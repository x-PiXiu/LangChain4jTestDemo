package the_14;

// ==================== 工具定义 ====================

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

/** 天气查询工具 */
class WeatherTool {

    @Tool("查询指定城市的当前天气，返回格式：城市,温度,天气状况,空气质量")
    public WeatherResult getWeather(
            @P("城市名称，必须是中文，例如：北京") String city) {
        // 真实场景：调用天气 API
        // 这里用模拟数据演示
        if (city.contains("北京")) {
            return new WeatherResult(city, 26.5, "晴", "优");
        }
        return new WeatherResult(city, 24.0, "多云", "良");
    }

    public record WeatherResult(
            String city, double temperature, String condition, String aqi
    ) {}
}

/** 通知提醒工具 */
class NotificationTool {

    @Tool("发送提醒通知，返回是否发送成功")
    public String sendAlert(
            @P("提醒内容") String message) {
        System.out.println("📢 发送通知：" + message);
        return "通知已发送：" + message;
    }
}

// ==================== Agent 定义 ====================

@SystemMessage("""
    你是一个贴心的智能助手。
    当用户问到天气时，先调用天气工具查询真实天气。
    如果天气显示有雨（小雨/中雨/大雨/暴雨），自动调用通知工具发送提醒。
    如果天气显示晴天或多云，不需要发送通知。
    完成任务后，直接输出最终回答，不需要再调用其他工具。
    """)
interface WeatherAssistant {
    String chat(String message);
}

// ==================== 启动入口 ====================

public class WeatherAgentDemo {

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();


        WeatherAssistant assistant = AiServices.builder(WeatherAssistant.class)
                .chatModel(model)
                .tools(new WeatherTool(), new NotificationTool())
                .build();

        // 一句调用，内部可能触发多轮 ReAct 循环
        String result = assistant.chat(
                "北京明天天气怎么样？有雨的话提醒我带伞出门"
        );
        System.out.println("AI 最终回答：" + result);
    }
}
