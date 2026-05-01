package the_19;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Guardrail 审计日志记录器（Section 6.3）
 * 记录所有拦截事件，用于统计分析与合规审计
 */
public class GuardrailAuditLog {

    private static final Logger log = Logger.getLogger(GuardrailAuditLog.class.getName());

    // 内存存储（生产环境应持久化到数据库）
    private final List<GuardrailInterceptEvent> events = Collections.synchronizedList(new ArrayList<>());

    public GuardrailAuditLog() {
    }

    /** 记录一次拦截事件 */
    public void log(GuardrailInterceptEvent event) {
        events.add(event);
        log.info("Guardrail 拦截: " + event.toLogString());
    }

    /** 获取所有拦截事件（不可变视图） */
    public List<GuardrailInterceptEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /** 获取拦截事件总数 */
    public int getEventCount() {
        return events.size();
    }

    /** 按规则类型统计拦截次数 */
    public String getStatistics() {
        long piiCount = events.stream().filter(e -> "PII".equals(e.getRuleType())).count();
        long contentCount = events.stream().filter(e -> "CONTENT".equals(e.getRuleType())).count();
        long lengthCount = events.stream().filter(e -> "LENGTH".equals(e.getRuleType())).count();
        long enumCount = events.stream().filter(e -> "ENUM".equals(e.getRuleType())).count();

        return String.format("""
            === Guardrail 拦截统计 ===
            PII 脱敏拦截: %d 次
            内容审核拦截: %d 次
            长度超限拦截: %d 次
            枚举值异常拦截: %d 次
            总计: %d 次
            =========================""",
            piiCount, contentCount, lengthCount, enumCount, events.size());
    }
}
