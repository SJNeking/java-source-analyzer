package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for DESIGN_PATTERN category rules.
 */
public class DesignPatternRulesTest extends AbstractRuleTest {

    // Singleton detection needs class-level context
    // @Test public void singletonWithPublicConstructor_shouldDetect() {}

    @Test
    public void complexFactoryMethod_shouldDetect() {
        DesignPatternRules.ComplexFactoryMethod rule = new DesignPatternRules.ComplexFactoryMethod();
        Map<String, Object> method = createMethod("createProduct", "if (type == 1) return new A();\n" +
            "else if (type == 2) return new B();\n" +
            "else if (type == 3) return new C();\n" +
            "else if (type == 4) return new D();\n" +
            "else if (type == 5) return new E();\n" +
            "else return new Default();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingStrategyPattern_shouldDetect() {
        DesignPatternRules.MissingStrategyPattern rule = new DesignPatternRules.MissingStrategyPattern();
        Map<String, Object> method = createMethod("calculate", "if (type == A) return price * 0.9;\n" +
            "else if (type == B) return price * 0.8;\n" +
            "else if (type == C) return price * 0.7;\n" +
            "else if (type == D) return price * 0.6;\n" +
            "return price;\n");
        assertIssues(rule, method, 1);
    }
}
