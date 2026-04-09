package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for STRING category rules.
 */
public class StringRulesTest extends AbstractRuleTest {

    @Test
    public void stringSplitRegex_shouldDetect() {
        StringRules.StringSplitRegex rule = new StringRules.StringSplitRegex();
        Map<String, Object> method = createMethod("split", "String[] parts = text.split(\".\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void caseInsensitiveComparison_shouldDetect() {
        StringRules.CaseInsensitiveComparison rule = new StringRules.CaseInsensitiveComparison();
        Map<String, Object> method = createMethod("equals", "if (str.toLowerCase().equals(\"test\")) return true;\n");
        assertIssues(rule, method, 1);
    }
}
