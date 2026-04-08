package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Robustness Rules - P5 Priority
 *
 * Detects code robustness anti-patterns:
 * - Switch without default
 * - Empty finally block
 * - Sun API usage
 * - Public fields
 * - Double brace initialization
 * - Boolean inversion
 * - Utility class pattern violations
 * - Static initializer exceptions
 * - Serialization issues
 */
public final class RobustnessRules {
    private RobustnessRules() {}

    // =====================================================================
    // Control Flow
    // =====================================================================

    /**
     * RSPEC-4001: switch statement should have default
     * Detects switch statements without default case.
     */
    public static class SwitchMissingDefault extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4001"; }
        public String getName() { return "switch statements should have default clause"; }
        public String getCategory() { return "ROBUSTNESS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("switch (") || body.contains("switch(")) {
                if (!body.contains("default:")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "switch statement without default", "missing default"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-4002: finally block should not be empty
     * Detects empty finally blocks.
     */
    public static class EmptyFinallyBlock extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4002"; }
        public String getName() { return "finally block should not be empty"; }
        public String getCategory() { return "ROBUSTNESS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            Pattern emptyFinally = Pattern.compile("finally\\s*\\{\\s*\\}");
            if (emptyFinally.matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Empty finally block", "empty finally"));
            }
            return issues;
        }
    }

    // =====================================================================
    // API Usage
    // =====================================================================

    /**
     * RSPEC-4003: Sun API usage
     * Detects usage of sun.* packages.
     */
    public static class SunApiUsage extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4003"; }
        public String getName() { return "Sun internal API should not be used"; }
        public String getCategory() { return "ROBUSTNESS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("sun.misc.") || body.contains("sun.reflect.") || body.contains("com.sun.")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Internal Sun API used", "sun.* API"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Encapsulation
    // =====================================================================

    /**
     * RSPEC-4004: Public field
     * Detects public fields in classes.
     */
    public static class PublicField extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4004"; }
        public String getName() { return "Fields should not be public"; }
        public String getCategory() { return "ROBUSTNESS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect public fields (not methods, not classes)
            Pattern publicField = Pattern.compile("public\\s+(?!class|interface|enum|static\\s+class)[\\w<>\\[\\]]+\\s+\\w+\\s*[;=]");
            if (publicField.matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Public field used", "public field"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Initialization
    // =====================================================================

    /**
     * RSPEC-4005: Double brace initialization
     * Detects double brace initialization pattern.
     */
    public static class DoubleBraceInitialization extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4005"; }
        public String getName() { return "Double brace initialization should not be used"; }
        public String getCategory() { return "ROBUSTNESS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("new ArrayList") && body.contains("{{") ||
                body.contains("new HashMap") && body.contains("{{") ||
                body.contains("new HashSet") && body.contains("{{")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Double brace initialization used", "anonymous inner class overhead"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-4006: Static initializer throws exception
     * Detects static blocks that throw exceptions.
     */
    public static class StaticInitializerException extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4006"; }
        public String getName() { return "Static initializer should not throw exceptions"; }
        public String getCategory() { return "ROBUSTNESS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("static {") && body.contains("throw ")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Static initializer throws exception", "static init error"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Logic Errors
    // =====================================================================

    /**
     * RSPEC-4007: Boolean inversion
     * Detects `== false` or `== true`.
     */
    public static class BooleanInversion extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4007"; }
        public String getName() { return "Boolean literals should not be redundant"; }
        public String getCategory() { return "ROBUSTNESS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("== true") || body.contains("== false")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Redundant boolean comparison", "== true/false"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-4008: Utility class pattern
     * Detects classes with only static methods but no private constructor.
     */
    public static class UtilityClassPattern extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4008"; }
        public String getName() { return "Utility classes should have private constructor"; }
        public String getCategory() { return "ROBUSTNESS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Heuristic: if class has static methods but no constructor logic
            if (body.contains("public static ") && !body.contains("private ") && !body.contains("new ")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Utility class without private constructor", "instantiable utility"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Serialization/Clone
    // =====================================================================

    /**
     * RSPEC-4009: Cloneable misuse
     * Detects clone() throwing CloneNotSupportedException improperly.
     */
    public static class CloneMisuse extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4009"; }
        public String getName() { return "Cloneable should be used correctly"; }
        public String getCategory() { return "ROBUSTNESS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (name != null && name.equals("clone") && !body.contains("super.clone()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "clone() without super.clone()", "missing super.clone"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-4010: ReadResolve returns wrong type
     * Detects readResolve method returning non-matching type.
     */
    public static class ReadResolveWrongType extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4010"; }
        public String getName() { return "readResolve should return correct type"; }
        public String getCategory() { return "ROBUSTNESS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (name != null && name.equals("readResolve") && !body.contains("return this") && !body.contains("return " + cn)) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "readResolve may return wrong type", "serialization type"));
            }
            return issues;
        }
    }
}
