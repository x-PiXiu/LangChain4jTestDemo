package the_4;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis持久化存储类
 * 实现了ChatMemoryStore接口，可与MessageWindowChatMemory配合使用
 */
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final JedisPool jedisPool;
    private static final String KEY_PREFIX = "chat_memory:";

    public RedisChatMemoryStore(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public RedisChatMemoryStore(String host, int port) {
        this.jedisPool = new JedisPool(host, port);
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = KEY_PREFIX + memoryId.toString();
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(key);
            if (json == null || json.isEmpty()) {
                return new ArrayList<>();
            }
            return fromJson(json);
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = KEY_PREFIX + memoryId.toString();
        try (Jedis jedis = jedisPool.getResource()) {
            if (messages == null || messages.isEmpty()) {
                jedis.del(key);
            } else {
                String json = toJson(messages);
                jedis.set(key, json);
            }
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String key = KEY_PREFIX + memoryId.toString();
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        }
    }

    // JSON序列化
    private String toJson(List<ChatMessage> messages) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> jsonList = new ArrayList<>();
            for (ChatMessage msg : messages) {
                if (msg instanceof UserMessage) {
                    jsonList.add("USER:" + ((UserMessage) msg).singleText());
                } else if (msg instanceof AiMessage) {
                    jsonList.add("AI:" + ((AiMessage) msg).text());
                } else if (msg instanceof SystemMessage) {
                    jsonList.add("SYSTEM:" + ((SystemMessage) msg).text());
                }
            }
            return mapper.writeValueAsString(jsonList);
        } catch (Exception e) {
            throw new RuntimeException("序列化失败", e);
        }
    }

    // JSON反序列化
    private List<ChatMessage> fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JavaType listType = mapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class);
            List<String> jsonList = mapper.readValue(json, listType);

            List<ChatMessage> messages = new ArrayList<>();
            for (String item : jsonList) {
                if (item.startsWith("USER:")) {
                    messages.add(UserMessage.from(item.substring(5)));
                } else if (item.startsWith("AI:")) {
                    messages.add(AiMessage.from(item.substring(3)));
                } else if (item.startsWith("SYSTEM:")) {
                    messages.add(SystemMessage.from(item.substring(7)));
                }
            }
            return messages;
        } catch (Exception e) {
            throw new RuntimeException("反序列化失败", e);
        }
    }

    public void close() {
        jedisPool.close();
    }
}