package the_17;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

public class MetricsCollectingListener implements ChatModelListener {

    private final Counter toolCallCounter;
    private final Counter errorCounter;
    private final Timer responseTimer;
    private final PrometheusMeterRegistry registry;

    public MetricsCollectingListener() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        toolCallCounter = Counter.builder("agent.tool.calls")
                .description("Agent tool call count")
                .register(registry);

        errorCounter = Counter.builder("agent.errors")
                .description("Agent error count")
                .register(registry);

        responseTimer = Timer.builder("agent.response.time")
                .description("Agent response time")
                .register(registry);
    }

    @Override
    public void onRequest(ChatModelRequestContext context) {
        // 请求阶段不需要处理
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        responseTimer.record(() -> {
            if (context.chatResponse().aiMessage().hasToolExecutionRequests()) {
                toolCallCounter.increment(
                        context.chatResponse().aiMessage().toolExecutionRequests().size()
                );
            }
        });
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        errorCounter.increment();
    }

    public PrometheusMeterRegistry getRegistry() {
        return registry;
    }
}
