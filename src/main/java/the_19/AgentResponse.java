package the_19;

/**
 * Agent 响应封装
 * 提供统一的成功/失败响应格式
 */
public class AgentResponse {

    private final boolean success;
    private final ServiceResult data;
    private final String errorMessage;

    private AgentResponse(boolean success, ServiceResult data, String errorMessage) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static AgentResponse success(ServiceResult result) {
        return new AgentResponse(true, result, null);
    }

    public static AgentResponse error(String errorMessage) {
        return new AgentResponse(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public ServiceResult getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        if (success) {
            return "AgentResponse{success=true, data=" + data + "}";
        }
        return "AgentResponse{success=false, error='" + errorMessage + "'}";
    }
}
