package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for MICROSERVICE category rules.
 */
public class MicroserviceRulesTest extends AbstractRuleTest {

    @Test
    public void missingTimeout_shouldDetect() {
        MicroserviceRules.MissingTimeout rule = new MicroserviceRules.MissingTimeout();
        Map<String, Object> method = createMethod("callService", "RestTemplate restTemplate = new RestTemplate();\n" +
            "restTemplate.getForObject(url, String.class);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void hardcodedServiceUrl_shouldDetect() {
        MicroserviceRules.HardcodedServiceUrl rule = new MicroserviceRules.HardcodedServiceUrl();
        Map<String, Object> method = createMethod("callService", "String url = \"http://service:8080/api\";\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingLoadBalancing_shouldDetect() {
        MicroserviceRules.MissingLoadBalancing rule = new MicroserviceRules.MissingLoadBalancing();
        Map<String, Object> method = createMethod("callService", "RestTemplate rest = new RestTemplate();\n" +
            "rest.getForObject(url, String.class);\n");
        assertIssues(rule, method, 1);
    }
}
