package the_17;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@SystemMessage("""
    你是一个AI回答质量评估专家。

    你会收到：
    1. 用户的原始问题
    2. 检索到的参考文档
    3. AI 的回答

    请评估：
    - faithfulness（忠实度，0-10分）：回答是否完全基于参考文档？
    - relevancy（相关性，0-10分）：回答是否切中用户问题？
    - completeness（完整性，0-10分）：回答是否完整覆盖了关键信息？

    输出JSON格式：
    {"faithfulness": 8, "relevancy": 9, "completeness": 7, "comment": "简要说明"}
    """)
public interface QualityJudge {
    String evaluate(@UserMessage String evaluationRequest);
}
