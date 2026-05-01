package the_3;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class IntentClassifier {

    private final ChatModel model;

    public IntentClassifier(ChatModel model) {
        this.model = model;
    }

    public String classify(String userMessage) {
        // Few-Shot Prompt：给出示例
        SystemMessage systemMessage = SystemMessage.from("""
            你是一个客服对话意图分类器。
            
            根据用户消息，判断属于哪个意图类别。只输出意图名称。
            
            示例：
            用户：我想退款
            意图：退款申请
            
            示例：
            用户：这东西什么时候发货
            意图：物流查询
            
            示例：
            用户：换个收货地址可以吗
            意图：地址修改
            
            示例：
            用户：质量有问题要投诉
            意图：投诉建议
            
            现在判断：
            用户：%s
            意图：
            """.formatted(userMessage));

        UserMessage um = UserMessage.from(userMessage);

        ChatRequest request = ChatRequest.builder()
                .messages(systemMessage, um)
                .build();

        ChatResponse response = model.chat(request);
        return response.aiMessage().text().trim();
    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("MINIMAX_API_KEY"))
                .baseUrl("https://api.minimax.chat/v1")
                .modelName("MiniMax-M2.5")
                .build();

        IntentClassifier classifier = new IntentClassifier(model);

        String[] testMessages = {
                "我的订单怎么还没到",
                "申请退货退款",
                "地址写错了改一下"
        };

        for (String msg : testMessages) {
            String intent = classifier.classify(msg);
            System.out.println("[" + msg + "] → " + intent);
        }
    }
}