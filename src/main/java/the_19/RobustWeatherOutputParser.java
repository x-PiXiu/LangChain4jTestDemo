package the_19;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 健壮的天气输出解析器（Section 2.3）
 * 三层防御：直接解析 → 修复常见格式问题 → 抛出明确异常
 */
public class RobustWeatherOutputParser implements OutputParser<WeatherInfo> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WeatherInfo parse(String result) {
        // 第 1 步：尝试直接解析
        try {
            return objectMapper.readValue(result, WeatherInfo.class);
        } catch (JsonProcessingException e) {
            // 第 2 步：解析失败 → 尝试修复常见格式问题
            String fixed = fixCommonIssues(result);
            try {
                return objectMapper.readValue(fixed, WeatherInfo.class);
            } catch (JsonProcessingException e2) {
                // 第 3 步：修复后仍然失败 → 抛出明确的业务异常
                throw new RuntimeException(
                    "Failed to parse WeatherInfo after fix attempts. " +
                    "Last error: " + e2.getMessage() + ". " +
                    "Raw output: " + result,
                    e2
                );
            }
        }
    }

    /** 修复 AI 输出中常见的格式问题 */
    private String fixCommonIssues(String raw) {
        return raw
            // 修复 temperature 格式不统一（如 "25度" → "25"）
            .replaceAll("(\\d+)度", "$1")
            // 去除多余空格
            .trim();
    }

    @Override
    public String formatInstructions() {
        return "请以 JSON 格式输出天气信息，字段包括：city, temperature, condition, humidity, wind";
    }
}
