package the_19;

import java.util.List;
import java.util.regex.Pattern;

/**
 * PII（个人身份信息）脱敏器（Section 4.2）
 * 使用正则表达式快速扫描并替换文本中的敏感信息
 *
 * 修复说明（相对文档原文）：
 * - 移除了 ChatModel nerModel 依赖（NER 部分属于高级功能，影响无参构造）
 * - 仅保留正则匹配策略，处理 90%+ 的常见 PII 格式
 */
public class PIIRedactor {

    // 正则匹配规则：速度快，覆盖常见格式
    private static final List<PIIPattern> REGEX_PATTERNS = List.of(
        new PIIPattern(
            Pattern.compile("\\b\\d{15,18}\\b"),      // 身份证号
            "[身份证号]"
        ),
        new PIIPattern(
            Pattern.compile("\\b1[3-9]\\d{9}\\b"),     // 手机号
            "[手机号]"
        ),
        new PIIPattern(
            Pattern.compile("\\b\\d{16,19}\\b"),       // 银行卡号
            "[银行卡号]"
        ),
        new PIIPattern(
            Pattern.compile("\\b\\d{17}[\\dXx]\\b"),   // 护照号
            "[护照号]"
        ),
        new PIIPattern(
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), // 邮箱
            "[邮箱]"
        )
    );

    public PIIRedactor() {
        // 无参构造，开箱即用
    }

    /**
     * 对文本进行 PII 脱敏
     * 依次应用所有正则模式，将匹配到的敏感信息替换为标签
     */
    public String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        for (PIIPattern pattern : REGEX_PATTERNS) {
            result = pattern.getPattern().matcher(result)
                .replaceAll(pattern.getReplacement());
        }
        return result;
    }
}
