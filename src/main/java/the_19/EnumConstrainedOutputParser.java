package the_19;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 枚举约束输出解析器（Section 5.1）
 * 将 AI 输出（包括中文）映射到枚举值
 *
 * 示例：AI 输出"成功" → OrderStatus.SUCCESS
 */
public class EnumConstrainedOutputParser<T extends Enum<T>> implements OutputParser<T> {

    private final Class<T> enumClass;

    public EnumConstrainedOutputParser(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public T parse(String result) {
        String trimmed = result.trim();

        // 提取纯枚举值（去除 JSON 包裹、引号等）
        String enumValue = extractEnumValue(trimmed);

        // 尝试直接解析
        try {
            return Enum.valueOf(enumClass, enumValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 枚举值不匹配 → 尝试模糊匹配
            String normalized = normalize(enumValue);
            for (T constant : enumClass.getEnumConstants()) {
                if (normalize(constant.name()).equals(normalized)) {
                    return constant;
                }
            }
            throw new RuntimeException(
                "Cannot parse '" + trimmed + "' to enum " + enumClass.getName(), e);
        }
    }

    /** 从 JSON 或纯文本中提取枚举值 */
    private String extractEnumValue(String input) {
        // 去除 JSON 引号包裹
        String cleaned = input;
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        // 去除可能的 JSON 对象包裹
        if (cleaned.startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                var node = mapper.readTree(cleaned);
                if (node.isObject() && !node.isEmpty()) {
                    cleaned = node.fields().next().getValue().asText();
                }
            } catch (Exception ignored) {
                // JSON 解析失败，使用原始值
            }
        }
        return cleaned.trim();
    }

    /** "success" / "成功" / "SUCCESS" → 都解析为同一格式 */
    private String normalize(String input) {
        return input
            .replaceAll("[^a-zA-Z0-9]", "")  // 去符号（含中文）
            .toUpperCase();
    }

    @Override
    public String formatInstructions() {
        StringBuilder names = new StringBuilder();
        T[] constants = enumClass.getEnumConstants();
        for (int i = 0; i < constants.length; i++) {
            if (i > 0) names.append(", ");
            names.append(constants[i].name());
        }
        return "请只输出以下枚举值之一：" + names;
    }
}
