package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for WEB_API category rules.
 */
public class WebApiRulesTest extends AbstractRuleTest {

    @Test
    public void missingHttpMethod_shouldDetect() {
        WebApiRules.MissingHttpMethod rule = new WebApiRules.MissingHttpMethod();
        Map<String, Object> method = createMethod("get", "@RequestMapping\nreturn null;\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingHttpMethod_shouldNotDetectWithGetMapping() {
        WebApiRules.MissingHttpMethod rule = new WebApiRules.MissingHttpMethod();
        Map<String, Object> method = createMethod("get", "@GetMapping\nreturn null;\n");
        assertIssues(rule, method, 0);
    }

    @Test
    public void missingResponseStatusCode_shouldDetect() {
        WebApiRules.MissingResponseStatusCode rule = new WebApiRules.MissingResponseStatusCode();
        Map<String, Object> method = createMethod("create", "@PostMapping\nreturn entity;\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingPagination_shouldDetect() {
        WebApiRules.MissingPagination rule = new WebApiRules.MissingPagination();
        Map<String, Object> method = createMethod("listAll", "return repository.findAll();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingInputValidation_shouldDetect() {
        WebApiRules.MissingInputValidation rule = new WebApiRules.MissingInputValidation();
        Map<String, Object> method = createMethod("create", "@RequestBody User user\nsave(user);\n");
        assertIssues(rule, method, 1);
    }
}
