package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for API_DESIGN category rules.
 */
public class ApiDesignRulesTest extends AbstractRuleTest {

    @Test
    public void returnNullInsteadOfEmpty_shouldDetect() {
        ApiDesignRules.ReturnNullInsteadOfEmpty rule = new ApiDesignRules.ReturnNullInsteadOfEmpty();
        Map<String, Object> method = createMethod("getList", "return null;\n", "java.util.List", null);
        assertIssues(rule, method, 1);
    }

    @Test
    public void returnNullInsteadOfEmpty_shouldNotDetectWithEmptyList() {
        ApiDesignRules.ReturnNullInsteadOfEmpty rule = new ApiDesignRules.ReturnNullInsteadOfEmpty();
        Map<String, Object> method = createMethod("getList", "return Collections.emptyList();\n", "java.util.List", null);
        assertIssues(rule, method, 0);
    }

    @Test
    public void mutableReturnType_shouldDetect() {
        ApiDesignRules.MutableReturnType rule = new ApiDesignRules.MutableReturnType();
        Map<String, Object> method = createMethod("getData", "return new ArrayList<>();\n", "ArrayList", null);
        assertIssues(rule, method, 1);
    }

    @Test
    public void apiReturnsImplementation_shouldDetect() {
        ApiDesignRules.ApiReturnsImplementation rule = new ApiDesignRules.ApiReturnsImplementation();
        Map<String, Object> method = createMethod("getMap", "return new HashMap<>();\n", "HashMap", null);
        assertIssues(rule, method, 1);
    }

    @Test
    public void synchronizedMethod_shouldDetect() {
        ApiDesignRules.SynchronizedMethod rule = new ApiDesignRules.SynchronizedMethod();
        Map<String, Object> method = createMethod("process", "doWork();\n", "void", Arrays.asList("public", "synchronized"));
        assertIssues(rule, method, 1);
    }

    @Test
    public void throwsCheckedException_shouldDetect() {
        ApiDesignRules.ThrowsCheckedException rule = new ApiDesignRules.ThrowsCheckedException();
        Map<String, Object> method = createMethod("readFile", "throws IOException\nFileInputStream fis = new FileInputStream(\"file.txt\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void builderNotUsed_shouldDetect() {
        ApiDesignRules.BuilderNotUsed rule = new ApiDesignRules.BuilderNotUsed();
        Map<String, Object> method = createMethod("create", "", "void", null);
        method.put("signature", "void create(String a, String b, String c, String d, String e, String f)");
        assertIssues(rule, method, 1);
    }

    @Test
    public void tooManyReturnPaths_shouldDetect() {
        ApiDesignRules.TooManyReturnPaths rule = new ApiDesignRules.TooManyReturnPaths();
        Map<String, Object> method = createMethod("getStatus", "if (a) return 1;\nif (b) return 2;\nif (c) return 3;\n" +
            "if (d) return 4;\nif (e) return 5;\nif (f) return 6;\nreturn 0;\n");
        assertIssues(rule, method, 1);
    }
}
