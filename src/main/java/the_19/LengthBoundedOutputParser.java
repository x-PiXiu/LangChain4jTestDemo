package the_19;

import java.util.logging.Logger;

/**
 * 长度限制输出解析器（Section 5.2）
 * 装饰器模式：在委托解析器前增加长度检查
 *
 * 示例：new LengthBoundedOutputParser<>(new XxxOutputParser(), 500)
 */
public class LengthBoundedOutputParser<T> implements OutputParser<T> {

    private static final Logger log = Logger.getLogger(LengthBoundedOutputParser.class.getName());

    private final OutputParser<T> delegate;
    private final int maxLength;

    public LengthBoundedOutputParser(OutputParser<T> delegate, int maxLength) {
        this.delegate = delegate;
        this.maxLength = maxLength;
    }

    @Override
    public T parse(String result) {
        if (result.length() > maxLength) {
            log.warning(String.format(
                "AI output length %d exceeds limit %d, truncating",
                result.length(), maxLength));
            result = result.substring(0, maxLength);
        }
        return delegate.parse(result);
    }

    @Override
    public String formatInstructions() {
        return delegate.formatInstructions() +
            String.format("。输出长度不要超过 %d 个字符", maxLength);
    }
}
