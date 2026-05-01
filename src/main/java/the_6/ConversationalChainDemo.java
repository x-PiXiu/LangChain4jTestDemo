package the_6;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

public class ConversationalChainDemo {

    public static void main(String[] args) {
        // 1. 初始化模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        // 2. 创建Memory（窗口大小20条消息）
        var memory = MessageWindowChatMemory.builder()
                .id("session_001")
                .maxMessages(20)
                .build();
        // 手动添加系统消息
        memory.add(SystemMessage.from("你是一个专业的Java技术顾问。回答问题时优先给出代码示例。"));

        // 3. 创建ConversationalChain
        // 内部自动把 memory 渲染到 Prompt 中，不需要手动拼接 history
        ConversationalChain chain = ConversationalChain.builder()
                .chatModel(model)
                .chatMemory(memory)
                .build();

        // 第1轮：问一个问题
        System.out.println("=== 第1轮 ===");
        String r1 = chain.execute("什么是Spring Boot？");
        System.out.println("AI：" + r1);

        // 第2轮：追问（AI自动记得上下文）
        System.out.println("\n=== 第2轮 ===");
        System.out.println("用户：它和Spring MVC什么关系？");
        String r2 = chain.execute("它和Spring MVC什么关系？");
        System.out.println("AI：" + r2);

        // 第3轮：让AI给示例
        System.out.println("\n=== 第3轮 ===");
        System.out.println("用户：给我一个Hello World示例");
        String r3 = chain.execute("给我一个Hello World示例");
        System.out.println("AI：" + r3);

        // 第4轮：验证AI记住了
        System.out.println("\n=== 第4轮（验证记忆）===");
        System.out.println("用户：刚才说的Hello World用的是什么框架？");
        String r4 = chain.execute("刚才说的Hello World用的是什么框架？");
        System.out.println("AI：" + r4);
    }
}
