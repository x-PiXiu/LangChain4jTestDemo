package the_7;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

public class SimpleCustomerService {

    public static void main(String[] args) {
        // 1. 初始化模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        // 2. 创建带记忆的客服Chain
        // 每个用户一个独立的Memory，用 userId 区分
        var customerMemory = MessageWindowChatMemory.builder()
                .id("customer_001")  // 模拟用户ID
                .maxMessages(20)     // 最多记忆20条消息
                .build();
        customerMemory.add(SystemMessage.from("""
                你是一个电商平台的智能客服。

                回答规则：
                1. 只回答与订单、物流、退换货相关的问题
                2. 如果用户问的问题与电商无关，请引导回到正题
                3. 回答要简洁、专业、有耐心
                4. 如果不知道答案，请说"这个问题我需要进一步确认，请稍等"
                """));
        ConversationalChain customerService = ConversationalChain.builder()
                .chatModel(model)
                .chatMemory(customerMemory)
                .build();

        // 3. 第一轮：用户问物流
        System.out.println("=== 第1轮：问物流 ===");
        String r1 = customerService.execute("我的订单什么时候发货？");
        System.out.println("用户：我的订单什么时候发货？");
        System.out.println("客服：" + r1);

        // 4. 第二轮：用户追问订单号（AI应该记得"订单"上下文）
        System.out.println("\n=== 第2轮：追问订单号 ===");
        String r2 = customerService.execute("我的订单号是TB123456，能查到吗？");
        System.out.println("用户：我的订单号是TB123456，能查到吗？");
        System.out.println("客服：" + r2);

        // 5. 第三轮：用户说想退换货
        System.out.println("\n=== 第3轮：退换货 ===");
        String r3 = customerService.execute("我想要退换货可以吗？");
        System.out.println("用户：我想要退换货可以吗？");
        System.out.println("客服：" + r3);
    }
}
