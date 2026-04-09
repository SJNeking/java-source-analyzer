package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for REFLECTION category rules.
 */
public class ReflectionRulesTest extends AbstractRuleTest {

    @Test
    public void setAccessible_shouldDetect() {
        ReflectionRules.SetAccessible rule = new ReflectionRules.SetAccessible();
        Map<String, Object> method = createMethod("access", "field.setAccessible(true);\n" +
            "field.get(obj);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void classForNameWithoutLoader_shouldDetect() {
        ReflectionRules.ClassForNameWithoutLoader rule = new ReflectionRules.ClassForNameWithoutLoader();
        Map<String, Object> method = createMethod("load", "Class<?> cls = Class.forName(className);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void proxyMisuse_shouldDetect() {
        ReflectionRules.ProxyMisuse rule = new ReflectionRules.ProxyMisuse();
        Map<String, Object> method = createMethod("createProxy", "Proxy.newProxyInstance(loader, interfaces, handler);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void dynamicClassLoading_shouldDetect() {
        ReflectionRules.DynamicClassLoading rule = new ReflectionRules.DynamicClassLoading();
        Map<String, Object> method = createMethod("load", "String cls = request.getParameter(\"class\");\n" +
            "Class.forName(cls);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void enumOrdinalUsed_shouldDetect() {
        ReflectionRules.EnumOrdinalUsed rule = new ReflectionRules.EnumOrdinalUsed();
        Map<String, Object> method = createMethod("getIndex", "return status.ordinal();\n");
        assertIssues(rule, method, 1);
    }
}
