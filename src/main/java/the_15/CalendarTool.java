package the_15;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CalendarTool {

    // 模拟日程存储（实际项目用数据库）
    private final Map<String, List<CalendarEvent>> events = new ConcurrentHashMap<>();

    @Tool("查询指定日期的所有日程安排，返回时间、标题和参与者")
    public String queryEvents(
            @P("日期，格式 yyyy-MM-dd，例如：2026-04-25") String date
    ) {
        List<CalendarEvent> list = events.get(date);
        if (list == null || list.isEmpty()) {
            return date + " 没有日程安排";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(date).append(" 共 ").append(list.size()).append(" 个日程：\n");
        for (CalendarEvent e : list) {
            sb.append(String.format("  %s %s（参与者：%s）\n",
                    e.time, e.title,
                    e.participants.isEmpty() ? "无" : String.join("、", e.participants)));
        }
        return sb.toString();
    }

    @Tool("添加一条新的日程安排")
    public String addEvent(
            @P("日程标题，如'项目评审会'") String title,
            @P("日期，格式 yyyy-MM-dd") String date,
            @P("时间，格式 HH:mm") String time,
            @P(value = "参与者列表，用逗号分隔，如'张三,李四'，可为空", required = false) String participants
    ) {
        List<String> participantList = (participants == null || participants.isBlank())
                ? List.of()
                : Arrays.asList(participants.split("[,，]"));
        CalendarEvent event = new CalendarEvent(title, time, participantList);
        events.computeIfAbsent(date, k -> new ArrayList<>()).add(event);
        return "日程已添加：" + date + " " + time + " " + title
                + (participantList.isEmpty() ? "" : "（参与者：" + String.join("、", participantList) + "）");
    }

    @Tool("删除指定日期的某个日程")
    public String removeEvent(
            @P("日期，格式 yyyy-MM-dd") String date,
            @P("要删除的日程标题") String title
    ) {
        List<CalendarEvent> list = events.get(date);
        if (list == null) return date + " 没有日程";
        boolean removed = list.removeIf(e -> e.title.equals(title));
        return removed ? "已删除：" + title : "未找到日程：" + title;
    }

    // 初始化一些测试数据
    public CalendarTool() {
        events.put("2026-04-25", List.of(
                new CalendarEvent("项目评审会", "15:00", List.of("张三", "李四")),
                new CalendarEvent("1:1 周会", "10:00", List.of("王五"))
        ));
    }

    record CalendarEvent(String title, String time, List<String> participants) {}
}
