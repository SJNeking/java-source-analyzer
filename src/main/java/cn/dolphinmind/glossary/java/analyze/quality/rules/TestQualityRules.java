package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Test Quality Rules - P5 Priority (Part 3 of Rule Expansion)
 *
 * Detects test quality issues:
 * - Missing assertions
 * - Test without @Test annotation
 * - Test method naming
 * - Mocking anti-patterns
 * - Test code duplication
 * - Flaky test patterns
 * - Missing test coverage for public methods
 */
public final class TestQualityRules {
    private TestQualityRules() {}

    /**
     * RSPEC-7001: Test without assertion
     */
    public static class TestWithoutAssertion extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-7001"; }
        public String getName() { return "Test should have assertion"; }
        public String getCategory() { return "TEST"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name.startsWith("test") || name.contains("Test")) {
                boolean hasAssertion = body.contains("assert") || body.contains("verify") || body.contains("expect") ||
                    body.contains("shouldThrow") || body.contains("ExpectedException");
                if (!hasAssertion) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Test without assertion", "useless test"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-7002: Test method naming convention
     */
    public static class TestMethodNameConvention extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-7002"; }
        public String getName() { return "Test method should follow naming convention"; }
        public String getCategory() { return "TEST"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name.startsWith("test") && name.length() > 4 && Character.isLowerCase(name.charAt(4))) {
                // testSomething is OK, but test_something or testSomethingWithMeaningfulName is better
                if (name.length() < 10 || !name.contains("Should") && !name.contains("When") && !name.contains("Given")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Test method naming not descriptive", "use should_when or given_when_then"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-7003: Excessive mocking
     */
    public static class ExcessiveMocking extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-7003"; }
        public String getName() { return "Test should not mock too many dependencies"; }
        public String getCategory() { return "TEST"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name.startsWith("test") || name.contains("Test")) {
                int mockCount = countOccurrences(body, "@Mock") + countOccurrences(body, "Mockito.mock") + countOccurrences(body, "when(");
                if (mockCount > 3) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Test mocks " + mockCount + " dependencies", "consider integration test"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-7004: Test code duplication
     */
    public static class TestCodeDuplication extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-7004"; }
        public String getName() { return "Test code should be refactored"; }
        public String getCategory() { return "TEST"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            // Simplified: check for repeated patterns in test body
            if (body.contains("given") && body.contains("when") && body.contains("then")) {
                // Check if setup code is duplicated
                String[] lines = body.split("\n");
                int givenCount = 0;
                for (String l : lines) {
                    if (l.trim().startsWith("given(") || l.trim().startsWith("when(")) givenCount++;
                }
                if (givenCount > 2) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Test has duplicated setup", "use @BeforeEach"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-7005: Flaky test pattern
     */
    public static class FlakyTest extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-7005"; }
        public String getName() { return "Test should not rely on timing"; }
        public String getCategory() { return "TEST"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("Thread.sleep") || body.contains("TimeUnit.") || body.contains("await()")) &&
                !body.contains("Awaitility") && !body.contains("CountDownLatch")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Test uses timing/sleep", "flaky test risk"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-7006: Test without cleanup
     */
    public static class TestWithoutCleanup extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-7006"; }
        public String getName() { return "Test should clean up resources"; }
        public String getCategory() { return "TEST"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name.startsWith("test") && (body.contains("createTempFile") || body.contains("new File") || body.contains("openConnection")) &&
                !body.contains("delete") && !body.contains("close") && !body.contains("@After") && !body.contains("try-with-resources")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Test creates resources without cleanup", "resource leak in tests"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-7007: Test catches generic Exception
     */
    public static class TestCatchGenericException extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-7007"; }
        public String getName() { return "Test should not catch generic Exception"; }
        public String getCategory() { return "TEST"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name.startsWith("test") && (body.contains("catch (Exception") || body.contains("catch (Throwable"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Test catches generic Exception", "catch specific exception"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-7008: Test method too long
     */
    public static class TestMethodTooLong extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-7008"; }
        public String getName() { return "Test method should be concise"; }
        public String getCategory() { return "TEST"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name.startsWith("test") && body.split("\n").length > 30) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Test method too long (>30 lines)", "split into smaller tests"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-7009: Test depends on order
     */
    public static class TestOrderDependency extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-7009"; }
        public String getName() { return "Tests should not depend on order"; }
        public String getCategory() { return "TEST"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("@TestInstance(PER_CLASS)") || body.contains("@FixMethodOrder")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Test depends on execution order", "tests should be independent"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-7010: Missing @ParameterizedTest
     */
    public static class MissingParameterizedTest extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-7010"; }
        public String getName() { return "Similar tests should use parameterization"; }
        public String getCategory() { return "TEST"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            // Detect repeated patterns like testCase1, testCase2, etc.
            if (name.matches("test\\w+\\d+") || name.matches("test\\w+_\\d+")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Test with numeric suffix", "use @ParameterizedTest"));
            }
            return issues;
        }
    }

    private static int countOccurrences(String text, String pattern) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) { count++; idx += pattern.length(); }
        return count;
    }
}
