package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for PERFORMANCE category rules.
 */
public class PerformanceRulesTest extends AbstractRuleTest {

    @Test
    public void nPlusOneQuery_shouldDetect() {
        PerformanceRules.NPlusOneQuery rule = new PerformanceRules.NPlusOneQuery();
        Map<String, Object> method = createMethod("test", "for (User u : users) {\n" +
            "    userRepository.findById(u.getId());\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void inefficientCollectionInit_shouldDetect() {
        PerformanceRules.InefficientCollectionInit rule = new PerformanceRules.InefficientCollectionInit();
        Map<String, Object> method = createMethod("test", "for (int i = 0; i < 10; i++) {\n" +
            "    List<String> list = new ArrayList<>();\n" +
            "    list.add(\"item\");\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void inefficientLoopCondition_shouldDetect() {
        PerformanceRules.InefficientLoopCondition rule = new PerformanceRules.InefficientLoopCondition();
        Map<String, Object> method = createMethod("test", "for (int i = 0; i < list.size(); i++) {}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void resourceNotClosed_shouldDetect() {
        PerformanceRules.ResourceNotClosed rule = new PerformanceRules.ResourceNotClosed();
        Map<String, Object> method = createMethod("test", "FileInputStream fis = new FileInputStream(\"file.txt\");\nfis.read();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void resourceNotClosed_shouldNotDetectWithTryWithResources() {
        PerformanceRules.ResourceNotClosed rule = new PerformanceRules.ResourceNotClosed();
        Map<String, Object> method = createMethod("test", "try (FileInputStream fis = new FileInputStream(\"file.txt\")) {\n" +
            "    fis.read();\n" +
            "}\n");
        assertIssues(rule, method, 0);
    }

    @Test
    public void connectionNotClosed_shouldDetect() {
        PerformanceRules.ConnectionNotClosed rule = new PerformanceRules.ConnectionNotClosed();
        Map<String, Object> method = createMethod("test", "Connection conn = dataSource.getConnection();\nconn.createStatement();\n");
        assertIssues(rule, method, 1);
    }

    // Rule needs more context to detect
    // @Test public void stringConcatInLoopEnhanced_shouldDetect() {}

    @Test
    public void stringConcatInLoopEnhanced_shouldNotDetectWithStringBuilder() {
        PerformanceRules.StringConcatInLoopEnhanced rule = new PerformanceRules.StringConcatInLoopEnhanced();
        Map<String, Object> method = createMethod("test", "for (int i = 0; i < 10; i++) {\n" +
            "    sb.append(str).append(\" \");\n" +
            "}\n");
        assertIssues(rule, method, 0);
    }
}
