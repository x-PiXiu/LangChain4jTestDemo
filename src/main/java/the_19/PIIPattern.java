package the_19;

import java.util.regex.Pattern;

/**
 * PII 正则匹配模式
 * 封装正则表达式与替换文本的对应关系
 */
public class PIIPattern {

    private final Pattern pattern;
    private final String replacement;

    public PIIPattern(Pattern pattern, String replacement) {
        this.pattern = pattern;
        this.replacement = replacement;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getReplacement() {
        return replacement;
    }
}
