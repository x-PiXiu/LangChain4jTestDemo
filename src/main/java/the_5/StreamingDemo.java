package the_5;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.concurrent.CountDownLatch;

public class StreamingDemo {

    public static void main(String[] args) throws InterruptedException {
        // 创建流式模型
        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .build();

        // 使用 CountDownLatch 等待流式完成
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullResponse = new StringBuilder();

        // 流式输出
        model.chat(
                "写一首关于春天的诗",
                new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        // 每个Token到就打印（打字机效果）
                        System.out.print(partialResponse);
                        fullResponse.append(partialResponse);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        System.out.println("\n\n--- 回复完成 ---");
                        System.out.println("完整内容：" + fullResponse.toString());
                        latch.countDown();  // 通知完成
                    }

                    @Override
                    public void onError(Throwable error) {
                        System.out.println("\n--- 发生错误 ---");
                        error.printStackTrace();
                        latch.countDown();  // 发生错误也要解除等待
                    }
                }
        );

        // 等待流式输出完成（最多等待30秒）
        boolean completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
        if (!completed) {
            System.out.println("\n--- 流式输出超时 ---");
        }
    }
}
