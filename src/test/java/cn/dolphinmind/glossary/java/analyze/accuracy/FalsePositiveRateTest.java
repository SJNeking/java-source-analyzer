package cn.dolphinmind.glossary.java.analyze.accuracy;

import cn.dolphinmind.glossary.java.analyze.quality.RuleEngine;
import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.rules.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * False positive rate tests to verify rule accuracy.
 *
 * These tests:
 * 1. Run rules against clean code (no issues expected)
 * 2. Verify rules don't flag valid code
 * 3. Measure false positive rate
 */
public class FalsePositiveRateTest {

    @Test
    public void emptyCatchBlockShouldNotTriggerOnValidCatch() {
        AllRules.EmptyCatchBlock rule = new AllRules.EmptyCatchBlock();
        Map<String, Object> method = createMethod("validMethod",
            "try {\n" +
            "    doSomething();\n" +
            "} catch (Exception e) {\n" +
            "    log.error(\"Error\", e);\n" +
            "}\n");
        List<QualityIssue> issues = rule.check(createClass("CleanClass", method));
        assertEquals("Should not flag valid catch block", 0, issues.size());
    }

    @Test
    public void stringLiteralEqualityShouldNotTriggerOnValidEquals() {
        AllRules.StringLiteralEquality rule = new AllRules.StringLiteralEquality();
        Map<String, Object> method = createMethod("validMethod",
            "if (\"hello\".equals(str)) {\n" +
            "    return true;\n" +
            "}\n");
        List<QualityIssue> issues = rule.check(createClass("CleanClass", method));
        assertEquals("Should not flag valid equals()", 0, issues.size());
    }

    @Test
    public void nullDereferenceShouldNotTriggerWithNullCheck() {
        AllRules.NullDereference rule = new AllRules.NullDereference();
        Map<String, Object> method = createMethod("validMethod",
            "String s = null;\n" +
            "if (s != null) {\n" +
            "    s.length();\n" +
            "}\n");
        List<QualityIssue> issues = rule.check(createClass("CleanClass", method));
        assertEquals("Should not flag null check before dereference", 0, issues.size());
    }

    @Test
    public void systemOutShouldNotTriggerOnValidLogging() {
        AllRules.SystemOutPrintln rule = new AllRules.SystemOutPrintln();
        Map<String, Object> method = createMethod("validMethod",
            "Logger log = LoggerFactory.getLogger(getClass());\n" +
            "log.info(\"Hello\");\n");
        List<QualityIssue> issues = rule.check(createClass("CleanClass", method));
        assertEquals("Should not flag valid logging", 0, issues.size());
    }

    @Test
    public void printStackTraceShouldNotTriggerOnValidLogging() {
        AllRules.PrintStackTrace rule = new AllRules.PrintStackTrace();
        Map<String, Object> method = createMethod("validMethod",
            "try {\n" +
            "    doSomething();\n" +
            "} catch (Exception e) {\n" +
            "    log.error(\"Error\", e);\n" +
            "}\n");
        List<QualityIssue> issues = rule.check(createClass("CleanClass", method));
        assertEquals("Should not flag valid error logging", 0, issues.size());
    }

    @Test
    public void tooLongMethodShouldNotTriggerOnReasonableMethod() {
        AllRules.TooLongMethod rule = new AllRules.TooLongMethod();
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            body.append("int var").append(i).append(" = ").append(i).append(";\n");
        }
        Map<String, Object> method = createMethod("validMethod", body.toString());
        List<QualityIssue> issues = rule.check(createClass("CleanClass", method));
        assertEquals("Should not flag 30-line method", 0, issues.size());
    }

    @Test
    public void booleanLiteralInConditionShouldNotTriggerOnValidCheck() {
        AllRules.BooleanLiteralInCondition rule = new AllRules.BooleanLiteralInCondition();
        Map<String, Object> method = createMethod("validMethod",
            "if (flag) {\n" +
            "    return true;\n" +
            "}\n");
        List<QualityIssue> issues = rule.check(createClass("CleanClass", method));
        assertEquals("Should not flag simple boolean check", 0, issues.size());
    }

    @Test
    public void equalsWithoutHashCodeShouldNotTriggerWhenBothPresent() {
        AllRules.EqualsWithoutHashCode rule = new AllRules.EqualsWithoutHashCode();
        Map<String, Object> clazz = createClass("CleanClass", Collections.emptyList());
        List<Map<String, Object>> methods = new ArrayList<>();
        methods.add(createMethod("equals", "return true;\n", "boolean", null));
        methods.add(createMethod("hashCode", "return 42;\n", "int", null));
        clazz.put("methods_full", methods);
        List<QualityIssue> issues = rule.check(clazz);
        assertEquals("Should not flag when both equals and hashCode present", 0, issues.size());
    }

    @Test
    public void magicNumberShouldNotTriggerOnZeroAndOne() {
        AllRules.MagicNumber rule = new AllRules.MagicNumber();
        Map<String, Object> method = createMethod("validMethod",
            "int x = 0;\n" +
            "int y = 1;\n" +
            "int z = -1;\n");
        List<QualityIssue> issues = rule.check(createClass("CleanClass", method));
        assertEquals("Should not flag 0, 1, -1", 0, issues.size());
    }

    @Test
    public void cleanCodeShouldHaveLowFalsePositiveRate() {
        // Run multiple rules against clean code
        RuleEngine engine = new RuleEngine();
        java.util.function.Consumer<cn.dolphinmind.glossary.java.analyze.quality.QualityRule> reg = rule -> {
            engine.registerRule(rule);
        };

        reg.accept(new AllRules.EmptyCatchBlock());
        reg.accept(new AllRules.StringLiteralEquality());
        reg.accept(new AllRules.NullDereference());
        reg.accept(new AllRules.SystemOutPrintln());
        reg.accept(new AllRules.PrintStackTrace());
        reg.accept(new AllRules.BooleanLiteralInCondition());

        // Clean class with valid patterns
        Map<String, Object> cleanClass = createClass("CleanClass", Arrays.asList(
            createMethod("validMethod1", "try {\n    doSomething();\n} catch (Exception e) {\n    log.error(\"Error\", e);\n}\n"),
            createMethod("validMethod2", "if (\"hello\".equals(str)) return true;\n"),
            createMethod("validMethod3", "if (flag) return true;\n"),
            createMethod("validMethod4", "Logger log = LoggerFactory.getLogger(getClass());\nlog.info(\"Hello\");\n")
        ));

        List<QualityIssue> issues = engine.run(Collections.singletonList(cleanClass));

        // Clean code should have zero false positives
        assertEquals("Clean code should produce zero issues", 0, issues.size());
    }

    private Map<String, Object> createMethod(String name, String body) {
        return createMethod(name, body, "void", null);
    }

    private Map<String, Object> createMethod(String name, String body, String returnType, List<String> modifiers) {
        Map<String, Object> method = new LinkedHashMap<>();
        method.put("name", name);
        method.put("body_code", body);
        method.put("return_type_path", returnType != null ? returnType : "void");
        method.put("modifiers", modifiers != null ? modifiers : Collections.singletonList("public"));
        method.put("line_start", 10);
        method.put("source_file", "CleanClass.java");
        return method;
    }

    private Map<String, Object> createClass(String className, List<Map<String, Object>> methods) {
        Map<String, Object> clazz = new LinkedHashMap<>();
        clazz.put("address", "com.example." + className);
        clazz.put("kind", "CLASS");
        clazz.put("source_file", className + ".java");
        clazz.put("methods_full", methods);
        clazz.put("fields_matrix", Collections.emptyList());
        clazz.put("hierarchy", new LinkedHashMap<>());
        return clazz;
    }

    private Map<String, Object> createClass(String className, Map<String, Object> method) {
        return createClass(className, Collections.singletonList(method));
    }
}
