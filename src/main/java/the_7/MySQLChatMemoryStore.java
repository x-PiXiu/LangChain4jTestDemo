package the_7;

import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * 基于MySQL的ChatMemoryStore实现
 *
 * 表结构：
 * CREATE TABLE chat_memory (
 *     id VARCHAR(64) PRIMARY KEY,      -- 用户ID
 *     messages JSON NOT NULL,         -- 消息历史（JSON数组）
 *     updated_at BIGINT NOT NULL      -- 更新时间戳
 * );
 */
public class MySQLChatMemoryStore implements ChatMemoryStore {

    private final Connection connection;

    public MySQLChatMemoryStore(String jdbcUrl, String username, String password) throws SQLException {
        this.connection = DriverManager.getConnection(jdbcUrl, username, password);
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String sql = "SELECT messages FROM chat_memory WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, (String) memoryId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String json = rs.getString("messages");
                return parseMessages(json);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String sql = """
            INSERT INTO chat_memory (id, messages, updated_at)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE messages = VALUES(messages), updated_at = VALUES(updated_at)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, (String) memoryId);
            ps.setString(2, toJson(messages));
            ps.setLong(3, Instant.now().toEpochMilli());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update memory", e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String sql = "DELETE FROM chat_memory WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, (String) memoryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete memory", e);
        }
    }

    // ========== 序列化工具 ==========

    private String toJson(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            String text = extractText(msg);
            if (i > 0) sb.append(",");
            sb.append("{\"type\":\"").append(msg.type()).append("\",");
            sb.append("\"text\":\"").append(escape(text)).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String extractText(ChatMessage msg) {
        if (msg instanceof UserMessage userMsg) return userMsg.singleText();
        if (msg instanceof AiMessage aiMsg) return aiMsg.text();
        if (msg instanceof SystemMessage sysMsg) return sysMsg.text();
        return "";
    }

    private List<ChatMessage> parseMessages(String json) {
        List<ChatMessage> messages = new ArrayList<>();
        if (json == null || json.isEmpty() || json.equals("[]")) return messages;

        // 简单解析：[{"type":"USER","text":"xxx"},{"type":"AI","text":"yyy"}]
        // 去掉首尾[]
        json = json.substring(1, json.length() - 1);
        int i = 0;
        while (i < json.length()) {
            int typeStart = json.indexOf("\"type\":\"", i);
            if (typeStart < 0) break;
            typeStart += 7;
            int typeEnd = json.indexOf("\"", typeStart);
            String type = json.substring(typeStart, typeEnd);

            int textStart = json.indexOf("\"text\":\"", typeEnd);
            if (textStart < 0) break;
            textStart += 7;
            int textEnd = json.indexOf("\"", textStart);
            String text = json.substring(textStart, textEnd);

            switch (type) {
                case "USER" -> messages.add(UserMessage.from(text));
                case "AI" -> messages.add(AiMessage.from(text));
                case "SYSTEM" -> messages.add(SystemMessage.from(text));
            }

            i = textEnd + 1;
        }
        return messages;
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

