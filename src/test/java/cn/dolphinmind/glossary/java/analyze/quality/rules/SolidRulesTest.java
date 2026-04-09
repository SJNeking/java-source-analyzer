package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for SOLID category rules.
 */
public class SolidRulesTest extends AbstractRuleTest {

    @Test
    public void openClosedViolation_shouldDetect() {
        SolidRules.OpenClosedViolation rule = new SolidRules.OpenClosedViolation();
        Map<String, Object> method = createMethod("process", "if (obj instanceof A) {}\n" +
            "else if (obj instanceof B) {}\n" +
            "else if (obj instanceof C) {}\n");
        assertIssues(rule, method, 1);
    }

    // Rule needs class context with @Override
    // @Test public void liskovSubstitution_shouldDetect() {}

    @Test
    public void dependencyInversion_shouldDetect() {
        SolidRules.DependencyInversion rule = new SolidRules.DependencyInversion();
        Map<String, Object> method = createMethod("process", "private ArrayList<String> items = new ArrayList<>();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void featureEnvy_shouldDetect() {
        SolidRules.FeatureEnvy rule = new SolidRules.FeatureEnvy();
        Map<String, Object> method = createMethod("calculate", "return order.getPrice() * order.getQuantity() + order.getTax() + order.getShipping() + order.getDiscount() + order.getFees();\n");
        assertIssues(rule, method, 1);
    }

    // DataClass needs class context with only getters/setters
    // @Test public void dataClass_shouldDetect() {}
}
