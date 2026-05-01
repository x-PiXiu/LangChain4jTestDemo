package the_17;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StructuredLoggingListener implements ChatModelListener {

    private static final Logger log = LoggerFactory.getLogger("agent-trace");

    @Override
    public void onRequest(ChatModelRequestContext context) {
        // 开发阶段不需要记录请求，DebugListener 已覆盖
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        AiMessage ai = context.chatResponse().aiMessage();
        TokenUsage usage = context.chatResponse().tokenUsage();

        if (ai.hasToolExecutionRequests()) {
            for (var req : ai.toolExecutionRequests()) {
                log.info("tool_call tool={} args={} tokens_in={} tokens_out={}",
                        req.name(),
                        req.arguments(),
                        usage != null ? usage.inputTokenCount() : 0,
                        usage != null ? usage.outputTokenCount() : 0
                );
            }
        } else {
            log.info("final_response length={} tokens_in={} tokens_out={}",
                    ai.text() != null ? ai.text().length() : 0,
                    usage != null ? usage.inputTokenCount() : 0,
                    usage != null ? usage.outputTokenCount() : 0
            );
        }
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        log.error("llm_error message={}", context.error().getMessage(), context.error());
    }
}
