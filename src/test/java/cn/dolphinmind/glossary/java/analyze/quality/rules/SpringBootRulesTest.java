package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for SPRING_BOOT category rules.
 */
public class SpringBootRulesTest extends AbstractRuleTest {

    @Test
    public void requestMappingWithoutMethod_shouldDetect() {
        SpringBootRules.RequestMappingWithoutMethod rule = new SpringBootRules.RequestMappingWithoutMethod();
        Map<String, Object> method = createMethod("get", "@RequestMapping(\"/api\")\nreturn null;\n");
        assertIssues(rule, method, 1);
    }

    // Generic exception handler detection needs class-level context
    // @Test public void genericExceptionHandler_shouldDetect() {}

    @Test
    public void configurationNotFinal_shouldDetect() {
        SpringBootRules.ConfigurationNotFinal rule = new SpringBootRules.ConfigurationNotFinal();
        Map<String, Object> method = createMethod("configure", "@Configuration\nclass MyConfig {}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingConditionalOnProperty_shouldDetect() {
        SpringBootRules.MissingConditionalOnProperty rule = new SpringBootRules.MissingConditionalOnProperty();
        Map<String, Object> method = createMethod("configure", "@Configuration\n@Bean\nreturn new Service();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingPathVariable_shouldDetect() {
        SpringBootRules.MissingPathVariable rule = new SpringBootRules.MissingPathVariable();
        Map<String, Object> method = createMethod("get", "@GetMapping(\"/users/{id}\")\nreturn userService.findById(id);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingRequestParamRequired_shouldDetect() {
        SpringBootRules.MissingRequestParamRequired rule = new SpringBootRules.MissingRequestParamRequired();
        Map<String, Object> method = createMethod("search", "public List<User> search(@RequestParam Optional<String> filter) {\n" +
            "return repo.findByFilter(filter);\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void beanWithoutName_shouldDetect() {
        SpringBootRules.BeanWithoutName rule = new SpringBootRules.BeanWithoutName();
        Map<String, Object> method = createMethod("service", "@Bean\nreturn new ServiceImpl();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void defaultComponentScan_shouldDetect() {
        SpringBootRules.DefaultComponentScan rule = new SpringBootRules.DefaultComponentScan();
        Map<String, Object> method = createMethod("scan", "@ComponentScan\npublic void configure() {}\n");
        assertIssues(rule, method, 1);
    }
}
