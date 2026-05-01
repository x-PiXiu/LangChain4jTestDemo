package the_17;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;

import java.util.List;

public class DebugListener implements ChatModelListener {

    @Override
    public void onRequest(ChatModelRequestContext context) {
        System.out.println("━━━ LLM 请求 ━━━");
        System.out.println("  消息数：" + context.chatRequest().messages().size());

        List<ChatMessage> msgs = context.chatRequest().messages();
        if (!msgs.isEmpty()) {
            ChatMessage last = msgs.get(msgs.size() - 1);
            String text = extractText(last);
            System.out.println("  最新消息：" + text);
        }

        List<?> tools = context.chatRequest().toolSpecifications();
        System.out.println("  可用工具：" + (tools != null ? tools.size() : 0));
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        AiMessage ai = context.chatResponse().aiMessage();

        System.out.println("━━━ LLM 响应 ━━━");

        if (ai.hasToolExecutionRequests()) {
            for (var req : ai.toolExecutionRequests()) {
                System.out.println("  🔧 调用工具：" + req.name());
                System.out.println("  📋 参数：" + req.arguments());
            }
        } else {
            System.out.println("  💬 回复：" + ai.text());
        }

        TokenUsage usage = context.chatResponse().tokenUsage();
        if (usage != null) {
            System.out.println("  📊 Token：" + usage.inputTokenCount() + " in / "
                    + usage.outputTokenCount() + " out");
        }
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        System.err.println("━━━ LLM 错误 ━━━");
        System.err.println("  " + context.error().getMessage());
    }

    private String extractText(ChatMessage msg) {
        if (msg instanceof UserMessage userMsg) {
            return userMsg.singleText();
        } else if (msg instanceof AiMessage aiMsg) {
            return aiMsg.text();
        }
        return msg.toString();
    }
}
