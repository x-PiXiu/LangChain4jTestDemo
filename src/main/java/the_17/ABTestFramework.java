package the_17;

public class ABTestFramework {

    record ABTestResult(
            String testCase,
            String versionA_result,
            String versionB_result,
            String verdict
    ) {}

    public ABTestResult runABTest(
            String input,
            SuperAssistant versionA,
            SuperAssistant versionB,
            QualityJudge judge) {

        String resultA = versionA.chat("ab_user", input);
        String resultB = versionB.chat("ab_user", input);

        String judgment = judge.evaluate("""
            用户问题：%s
            版本A回答：%s
            版本B回答：%s
            哪个回答更好？从准确性、完整性和相关性角度评估。
            输出：A更好 / B更好 / 持平，并说明原因。
            """.formatted(input, resultA, resultB));

        return new ABTestResult(input, resultA, resultB, judgment);
    }
}
