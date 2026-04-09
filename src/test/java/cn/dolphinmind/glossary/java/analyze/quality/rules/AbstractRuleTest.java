package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;

import java.util.*;

/**
 * Base class for all rule unit tests.
 *
 * Provides helper methods to create test method assets and verify rule detection.
 */
public abstract class AbstractRuleTest {

    /**
     * Create a minimal method asset for testing.
     */
    protected Map<String, Object> createMethod(String name, String body, String returnType, List<String> modifiers) {
        Map<String, Object> method = new LinkedHashMap<>();
        method.put("name", name);
        method.put("body_code", body);
        method.put("return_type_path", returnType != null ? returnType : "void");
        method.put("modifiers", modifiers != null ? modifiers : Collections.singletonList("public"));
        method.put("line_start", 10);
        method.put("source_file", "com/example/Test.java");
        return method;
    }

    /**
     * Create a method with just a name and body.
     */
    protected Map<String, Object> createMethod(String name, String body) {
        return createMethod(name, body, "void", null);
    }

    /**
     * Create a class asset containing the given methods.
     */
    protected Map<String, Object> createClass(String className, List<Map<String, Object>> methods) {
        Map<String, Object> clazz = new LinkedHashMap<>();
        clazz.put("address", "com.example." + className);
        clazz.put("kind", "CLASS");
        clazz.put("source_file", "com/example/" + className + ".java");
        clazz.put("methods_full", methods);
        clazz.put("fields_matrix", Collections.emptyList());
        clazz.put("hierarchy", new LinkedHashMap<>());
        return clazz;
    }

    /**
     * Run a rule against a single method and return issues.
     */
    protected List<QualityIssue> runRuleOnMethod(QualityRule rule, Map<String, Object> method) {
        Map<String, Object> clazz = createClass("TestClass", Collections.singletonList(method));
        return rule.check(clazz);
    }

    /**
     * Run a rule against a class with methods and return issues.
     */
    protected List<QualityIssue> runRuleOnClass(QualityRule rule, Map<String, Object> clazz) {
        return rule.check(clazz);
    }

    /**
     * Assert that a rule detects the expected number of issues.
     */
    protected void assertIssues(QualityRule rule, Map<String, Object> method, int expected) {
        List<QualityIssue> issues = runRuleOnMethod(rule, method);
        if (issues.size() != expected) {
            throw new AssertionError(rule.getRuleKey() + ": expected " + expected + " issues but got " + issues.size() +
                (issues.isEmpty() ? "" : " - " + issues.get(0).getMessage()));
        }
    }

    /**
     * Assert that a rule detects issues on a class.
     */
    protected void assertIssuesOnClass(QualityRule rule, Map<String, Object> clazz, int expected) {
        List<QualityIssue> issues = runRuleOnClass(rule, clazz);
        if (issues.size() != expected) {
            throw new AssertionError(rule.getRuleKey() + ": expected " + expected + " issues but got " + issues.size() +
                (issues.isEmpty() ? "" : " - " + issues.get(0).getMessage()));
        }
    }
}
