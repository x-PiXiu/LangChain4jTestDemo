package the_15;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.Map;

public class EmailTool {

    @Tool("发送一封邮件给指定收件人")
    public String sendEmail(
            @P("收件人姓名") String recipient,
            @P("邮件主题") String subject,
            @P("邮件正文内容") String body
    ) {
        // 实际项目中：调用邮件服务 API（JavaMail / SendGrid / 企业微信）
        // 这里用控制台输出模拟
        System.out.println("📧 发送邮件 ====");
        System.out.println("  收件人：" + recipient);
        System.out.println("  主题：" + subject);
        System.out.println("  正文：" + body);
        System.out.println("================");

        return "邮件已发送给 " + recipient + "，主题：" + subject;
    }

    @Tool("查询某个联系人的邮箱地址")
    public String lookupEmail(
            @P("联系人姓名") String name
    ) {
        // 模拟通讯录
        Map<String, String> contacts = Map.of(
                "张三", "zhangsan@company.com",
                "李四", "lisi@company.com",
                "王五", "wangwu@company.com"
        );
        String email = contacts.get(name);
        return email != null ? email : "未找到 " + name + " 的邮箱";
    }
}
