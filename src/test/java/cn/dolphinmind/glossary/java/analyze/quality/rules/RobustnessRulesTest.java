package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for ROBUSTNESS category rules.
 */
public class RobustnessRulesTest extends AbstractRuleTest {

    @Test
    public void switchMissingDefault_shouldDetect() {
        RobustnessRules.SwitchMissingDefault rule = new RobustnessRules.SwitchMissingDefault();
        Map<String, Object> method = createMethod("process", "switch (code) {\n" +
            "    case 1: break;\n" +
            "    case 2: break;\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void emptyFinallyBlock_shouldDetect() {
        RobustnessRules.EmptyFinallyBlock rule = new RobustnessRules.EmptyFinallyBlock();
        Map<String, Object> method = createMethod("process", "try {\n" +
            "    doWork();\n" +
            "} finally {\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void sunApiUsage_shouldDetect() {
        RobustnessRules.SunApiUsage rule = new RobustnessRules.SunApiUsage();
        Map<String, Object> method = createMethod("process", "sun.misc.Unsafe unsafe = getUnsafe();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void doubleBraceInitialization_shouldDetect() {
        RobustnessRules.DoubleBraceInitialization rule = new RobustnessRules.DoubleBraceInitialization();
        Map<String, Object> method = createMethod("createList", "List<String> list = new ArrayList<>() {{\n" +
            "    add(\"a\");\n" +
            "    add(\"b\");\n" +
            "}};\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void booleanInversion_shouldDetect() {
        RobustnessRules.BooleanInversion rule = new RobustnessRules.BooleanInversion();
        Map<String, Object> method = createMethod("check", "if (flag == true) return true;\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void cloneMisuse_shouldDetect() {
        RobustnessRules.CloneMisuse rule = new RobustnessRules.CloneMisuse();
        Map<String, Object> method = createMethod("clone", "return new MyObject();\n");
        assertIssues(rule, method, 1);
    }

    // EnumOrdinalUsed is in ReflectionRules, not RobustnessRules
    // @Test public void enumOrdinalUsed_shouldDetect() {}
}
