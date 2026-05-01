package the_19;

/**
 * 客服服务结果（可变 POJO）
 * 支持 setMessage / setDescription 用于 Guardrails 管道中的 PII 脱敏和内容替换
 */
public class ServiceResult {

    private String message;
    private String description;
    private String status;
    private String suggestion;

    public ServiceResult() {
    }

    public ServiceResult(String message, String description, String status, String suggestion) {
        this.message = message;
        this.description = description;
        this.status = status;
        this.suggestion = suggestion;
    }

    /** 降级工厂方法：当 JSON 解析失败时，包装纯文本返回 */
    public static ServiceResult fallback(String rawText) {
        ServiceResult result = new ServiceResult();
        result.message = rawText;
        result.description = "";
        result.status = "FALLBACK";
        result.suggestion = "";
        return result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    @Override
    public String toString() {
        return "ServiceResult{status='" + status +
                "', message='" + message + "'}";
    }
}
