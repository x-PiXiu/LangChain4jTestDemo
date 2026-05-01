package the_19;

/**
 * 自定义 OutputParser 接口
 *
 * 说明：LangChain4j 1.0.0 的 dev.langchain4j.service.output.OutputParser 是 package-private 的，
 * 无法在外部包中实现。这里定义一个同语义的公开接口，保持文档中的设计模式。
 */
public interface OutputParser<T> {

    /**
     * 将 AI 输出字符串解析为目标类型
     */
    T parse(String result);

    /**
     * 返回格式化指令，告诉 AI 应该输出什么格式
     */
    String formatInstructions();
}
