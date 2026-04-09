package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for MODERNIZATION category rules.
 */
public class ModernizationRulesTest extends AbstractRuleTest {

    @Test
    public void useLocalDate_shouldDetect() {
        JavaModernizationRules.UseLocalDate rule = new JavaModernizationRules.UseLocalDate();
        Map<String, Object> method = createMethod("getDate", "Date now = new Date();\n" +
            "SimpleDateFormat sdf = new SimpleDateFormat(\"yyyy-MM-dd\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void legacyCollections_shouldDetect() {
        JavaModernizationRules.LegacyCollections rule = new JavaModernizationRules.LegacyCollections();
        Map<String, Object> method = createMethod("getList", "Vector<String> v = new Vector<>();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void stringBufferInsteadOfBuilder_shouldDetect() {
        JavaModernizationRules.StringBufferInsteadOfBuilder rule = new JavaModernizationRules.StringBufferInsteadOfBuilder();
        Map<String, Object> method = createMethod("build", "StringBuffer sb = new StringBuffer();\n" +
            "sb.append(\"hello\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void wrapperClassConstructor_shouldDetect() {
        JavaModernizationRules.WrapperClassConstructor rule = new JavaModernizationRules.WrapperClassConstructor();
        Map<String, Object> method = createMethod("convert", "Integer x = new Integer(5);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void unnecessaryBoxing_shouldDetect() {
        JavaModernizationRules.UnnecessaryBoxing rule = new JavaModernizationRules.UnnecessaryBoxing();
        Map<String, Object> method = createMethod("convert", "int val = Integer.valueOf(5).intValue();\n");
        assertIssues(rule, method, 1);
    }
}
