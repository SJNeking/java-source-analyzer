package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for JAVA8_PLUS category rules.
 */
public class Java8PlusRulesTest extends AbstractRuleTest {

    @Test
    public void useStreamApi_shouldDetect() {
        Java8PlusRules.UseStreamApi rule = new Java8PlusRules.UseStreamApi();
        Map<String, Object> method = createMethod("filterList", "List<String> result = new ArrayList<>();\n" +
            "for (String s : list) {\n" +
            "    if (s.length() > 5) result.add(s);\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void useOptional_shouldDetect() {
        Java8PlusRules.UseOptional rule = new Java8PlusRules.UseOptional();
        Map<String, Object> method = createMethod("findUser", "if (user == null) return null;\n" +
            "return user.getName();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void useMethodReference_shouldDetect() {
        Java8PlusRules.UseMethodReference rule = new Java8PlusRules.UseMethodReference();
        Map<String, Object> method = createMethod("process", "list.stream().map(x -> x.toString()).collect(Collectors.toList());\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void useTryWithResources_shouldDetect() {
        Java8PlusRules.UseTryWithResources rule = new Java8PlusRules.UseTryWithResources();
        Map<String, Object> method = createMethod("readFile", "FileInputStream fis = new FileInputStream(\"file.txt\");\n" +
            "try {\n" +
            "    fis.read();\n" +
            "} catch (IOException e) {\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void useArraysAsList_shouldDetect() {
        Java8PlusRules.UseArraysAsList rule = new Java8PlusRules.UseArraysAsList();
        Map<String, Object> method = createMethod("createList", "List<String> list = new ArrayList<>();\n" +
            "list.add(\"a\");\n" +
            "list.add(\"b\");\n" +
            "return list;\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void useCompletableFuture_shouldDetect() {
        Java8PlusRules.UseCompletableFuture rule = new Java8PlusRules.UseCompletableFuture();
        Map<String, Object> method = createMethod("asyncTask", "new Thread(() -> doWork()).start();\n");
        assertIssues(rule, method, 1);
    }
}
