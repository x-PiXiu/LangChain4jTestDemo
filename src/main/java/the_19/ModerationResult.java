package the_19;

import java.util.List;

/**
 * 内容审核结果
 * 封装 PII 扫描、外部 API 审核、LLM 语义审核的综合判定
 */
public class ModerationResult {

    private final boolean clean;
    private final String redactedText;
    private final List<String> violations;
    private final String violatedContent;

    public ModerationResult(boolean clean, String redactedText, List<String> violations) {
        this(clean, redactedText, violations, null);
    }

    public ModerationResult(boolean clean, String redactedText, List<String> violations, String violatedContent) {
        this.clean = clean;
        this.redactedText = redactedText;
        this.violations = violations;
        this.violatedContent = violatedContent;
    }

    public boolean isClean() {
        return clean;
    }

    public String getRedactedText() {
        return redactedText;
    }

    public List<String> getViolations() {
        return violations;
    }

    public String getViolatedContent() {
        return violatedContent;
    }

    @Override
    public String toString() {
        return "ModerationResult{clean=" + clean +
                ", violations=" + violations +
                ", redactedText='" + redactedText + "'}";
    }
}
