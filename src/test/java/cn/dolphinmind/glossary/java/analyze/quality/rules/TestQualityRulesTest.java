package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for TEST category rules.
 */
public class TestQualityRulesTest extends AbstractRuleTest {

    @Test
    public void testWithoutAssertion_shouldDetect() {
        TestQualityRules.TestWithoutAssertion rule = new TestQualityRules.TestWithoutAssertion();
        Map<String, Object> method = createMethod("testProcess", "service.doSomething();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void testWithoutAssertion_shouldNotDetectWithAssert() {
        TestQualityRules.TestWithoutAssertion rule = new TestQualityRules.TestWithoutAssertion();
        Map<String, Object> method = createMethod("testProcess", "service.doSomething();\nassertEquals(1, result);\n");
        assertIssues(rule, method, 0);
    }

    @Test
    public void excessiveMocking_shouldDetect() {
        TestQualityRules.ExcessiveMocking rule = new TestQualityRules.ExcessiveMocking();
        Map<String, Object> method = createMethod("testProcess", "@Mock A a;\n@Mock B b;\n@Mock C c;\n@Mock D d;\n" +
            "when(a.get()).thenReturn(1);\nwhen(b.get()).thenReturn(2);\nwhen(c.get()).thenReturn(3);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void flakyTests_shouldDetect() {
        TestQualityRules.FlakyTest rule = new TestQualityRules.FlakyTest();
        Map<String, Object> method = createMethod("testProcess", "Thread.sleep(1000);\n" +
            "assertNotNull(result);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void testMethodTooLong_shouldDetect() {
        TestQualityRules.TestMethodTooLong rule = new TestQualityRules.TestMethodTooLong();
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 35; i++) {
            body.append("// step ").append(i).append("\n");
        }
        Map<String, Object> method = createMethod("testLongProcess", body.toString());
        assertIssues(rule, method, 1);
    }

    @Test
    public void testOrderDependency_shouldDetect() {
        TestQualityRules.TestOrderDependency rule = new TestQualityRules.TestOrderDependency();
        Map<String, Object> method = createMethod("testProcess", "@TestInstance(PER_CLASS)\n" +
            "doSomething();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingParameterizedTest_shouldDetect() {
        TestQualityRules.MissingParameterizedTest rule = new TestQualityRules.MissingParameterizedTest();
        Map<String, Object> method = createMethod("testCase1", "assertNotNull(result);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void testCatchGenericException_shouldDetect() {
        TestQualityRules.TestCatchGenericException rule = new TestQualityRules.TestCatchGenericException();
        Map<String, Object> method = createMethod("testProcess", "try {\n" +
            "    doWork();\n" +
            "} catch (Exception e) {}\n");
        assertIssues(rule, method, 1);
    }
}
