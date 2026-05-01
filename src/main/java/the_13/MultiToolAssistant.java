package the_13;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// ========== 工具类 1：天气工具 ==========
class WeatherTools {

    @Tool("查询指定城市的当前天气，返回温度、天气状况、风速、湿度")
    public String getCurrentWeather(
            @P("城市名称，如'北京'、'上海'、'深圳'") String city
    ) {
        // 模拟天气API调用
        Map<String, String> mockData = Map.of(
                "北京", "☀️ 晴天，25°C，湿度45%，东北风3级",
                "上海", "☁️ 多云，22°C，湿度68%，东南风2级",
                "深圳", "🌧️ 小雨，28°C，湿度85%，南风1级",
                "成都", "🌫️ 阴天，20°C，湿度72%，微风"
        );
        return mockData.getOrDefault(city, city + "的天气数据暂不可用");
    }

    @Tool("查询未来几天的天气预报")
    public String getWeatherForecast(
            @P("城市名称") String city,
            @P("预报天数，1-7天") int days
    ) {
        if (days < 1 || days > 7) {
            return "预报天数必须在1-7天之间";
        }
        // 模拟返回
        return String.format("%s未来%d天：第1天晴25°C，第2天多云22°C，第3天小雨20°C", city, days);
    }
}

// ========== 工具类 2：数据库查询工具（模拟） ==========
class DatabaseTools {

    @Tool("执行只读SQL查询，仅支持SELECT语句，用于查询用户数据、订单数据等")
    public String executeQuery(
            @P("要执行的SELECT SQL语句，必须是只读查询") String sql
    ) {
        // 安全检查：只允许 SELECT
        String trimmed = sql.trim().toUpperCase();
        if (!trimmed.startsWith("SELECT")) {
            return "安全限制：仅支持SELECT查询";
        }
        if (trimmed.contains("DROP") || trimmed.contains("DELETE") ||
                trimmed.contains("UPDATE") || trimmed.contains("INSERT")) {
            return "安全限制：检测到危险操作关键字";
        }

        // 模拟查询结果
        if (trimmed.contains("USER") || trimmed.contains("用户")) {
            return "查询到 3 条记录（显示前3条）：\n" +
                    "{id=1, name=张三, register_time=2024-01-15}\n" +
                    "{id=2, name=李四, register_time=2024-02-20}\n" +
                    "{id=3, name=王五, register_time=2024-03-10}";
        }
        if (trimmed.contains("ORDER") || trimmed.contains("订单")) {
            return "查询到 2 条记录（显示前2条）：\n" +
                    "{order_id=101, user=张三, amount=299.00, status=已发货}\n" +
                    "{order_id=102, user=李四, amount=158.50, status=待付款}";
        }
        return "查询结果为空（模拟数据库暂无匹配数据）";
    }
}

// ========== 工具类 3：日历管理工具 ==========
class CalendarTools {

    private final Map<String, List<String>> calendar = new ConcurrentHashMap<>();

    @Tool("添加一个日程事件到用户的日历")
    public String addEvent(
            @P("事件标题，如'团队周会'、'客户拜访'") String title,
            @P("事件日期，格式：yyyy-MM-dd") String date,
            @P("事件时间，格式：HH:mm") String time,
            @P(value = "事件备注说明", required = false) String note
    ) {
        String key = date;
        String event = String.format("%s %s - %s%s",
                date, time, title,
                note != null ? "（" + note + "）" : "");
        calendar.computeIfAbsent(key, k -> new ArrayList<>()).add(event);
        return "日程已添加：" + event;
    }

    @Tool("查询指定日期的所有日程事件")
    public String queryEvents(
            @P("要查询的日期，格式：yyyy-MM-dd") String date
    ) {
        List<String> events = calendar.get(date);
        if (events == null || events.isEmpty()) {
            return date + " 没有日程安排";
        }
        return date + " 的日程：\n" + String.join("\n", events);
    }

    @Tool("删除指定日期的所有日程事件")
    public String clearEvents(
            @P("要清空日程的日期，格式：yyyy-MM-dd") String date
    ) {
        List<String> removed = calendar.remove(date);
        if (removed == null) {
            return date + " 没有日程可删除";
        }
        return "已删除 " + date + " 的 " + removed.size() + " 个日程";
    }
}

// ========== 主程序：组装所有工具 ==========
public class MultiToolAssistant {

    interface Assistant {
        @SystemMessage("""
            你是一个全能AI助手，具备以下能力：
            1. 查询天气和天气预报
            2. 查询数据库中的用户和订单数据
            3. 管理用户的日程日历

            使用工具时注意：
            - 只在需要时调用工具，闲聊不需要调用
            - SQL查询只支持SELECT，注意安全
            - 日程操作前先确认日期格式正确
            """)
        String chat(@MemoryId String userId, @UserMessage String message);
    }

    public static void main(String[] args) {
        // 创建模型
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        // 创建工具实例
        WeatherTools weatherTools = new WeatherTools();
        DatabaseTools dbTools = new DatabaseTools();
        CalendarTools calendarTools = new CalendarTools();

        // 构建 AI Service
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(20))
                .tools(weatherTools, dbTools, calendarTools)
                .build();

        // 测试对话
        System.out.println("=== 测试1：查天气 ===");
        System.out.println(assistant.chat("user_001",
                "帮我看看北京和上海今天天气怎么样"));

        System.out.println("\n=== 测试2：查数据库 ===");
        System.out.println(assistant.chat("user_001",
                "查一下最近注册的5个用户"));

        System.out.println("\n=== 测试3：管理日程 ===");
        System.out.println(assistant.chat("user_001",
                "帮我明天下午3点安排一个团队周会，备注：准备季度汇报"));

        System.out.println("\n=== 测试4：组合任务 ===");
        System.out.println(assistant.chat("user_001",
                "明天北京天气怎么样？如果不下雨，帮我安排明天上午10点户外团建"));
    }
}
