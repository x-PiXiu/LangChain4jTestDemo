package the_17;

import java.util.List;

public class AgentTestSuite {

    record TestCase(
            String input,
            List<String> expectedTools,
            String expectedContent,
            int maxToolCalls
    ) {}

    private final List<TestCase> testCases = List.of(
            new TestCase(
                    "查一下明天下午有没有会议",
                    List.of("queryEvents"),
                    "日程",
                    1
            ),
            new TestCase(
                    "帮我查明天会议，有的话给张三发确认邮件",
                    List.of("queryEvents", "lookupEmail", "sendEmail"),
                    "邮件",
                    4
            ),
            new TestCase(
                    "上季度销售数据怎么样",
                    List.of("searchKnowledge"),
                    "销售额",
                    2
            )
    );

    public void runTests(SuperAssistant agent) {
        int passed = 0;
        int total = testCases.size();

        for (TestCase tc : testCases) {
            String result = agent.chat("test_user", tc.input);
            boolean contentMatch = result.contains(tc.expectedContent);

            if (contentMatch) {
                passed++;
                System.out.printf("✅ PASS: %s%n", tc.input);
            } else {
                System.out.printf("❌ FAIL: %s → 期望包含'%s'，实际：'%s'%n",
                        tc.input, tc.expectedContent,
                        result.length() > 100 ? result.substring(0, 100) + "..." : result);
            }
        }

        System.out.printf("%n完成率：%d/%d（%.0f%%）%n",
                passed, total, (double) passed / total * 100);
    }
}
