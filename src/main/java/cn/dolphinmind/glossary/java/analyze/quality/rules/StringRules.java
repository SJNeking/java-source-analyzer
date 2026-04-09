package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * String Rules
 *
 * Detects string anti-patterns:
 * - String concatenation in loop
 * - equals() on char array
 * - toString() not overridden
 * - Empty string check
 * - String.split regex
 * - substring bounds
 * - String intern misuse
 * - Case-insensitive comparison
 * - StringBuilder capacity
 * - String.format vs concatenation
 */
public final class StringRules {
    private StringRules() {}

    /**
     * RSPEC-17001: String concatenation in loop
     */
    public static class StringConcatInLoop extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-17001"; }
        public String getName() { return "Use StringBuilder in loops"; }
        public String getCategory() { return "STRING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("for (") || body.contains("while (")) && body.contains(" + ")) {
                if (!body.contains("StringBuilder") && !body.contains("StringBuffer")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "String concatenation in loop", "use StringBuilder"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-17002: equals() on char array
     */
    public static class EqualsOnCharArray extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-17002"; }
        public String getName() { return "Use Arrays.equals() for char arrays"; }
        public String getCategory() { return "STRING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".equals(") && (body.contains("char[]") || body.contains("toCharArray"))) {
                if (!body.contains("Arrays.equals")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "equals() on char array", "use Arrays.equals()"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-17003: Empty string check
     */
    public static class EmptyStringCheck extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-17003"; }
        public String getName() { return "Use isEmpty() instead of length() == 0"; }
        public String getCategory() { return "STRING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".length() == 0") || body.contains(".length() == 0")) {
                if (body.contains("String") || body.contains("\"")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "length() == 0 check", "use isEmpty()"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-17004: String.split regex
     */
    public static class StringSplitRegex extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-17004"; }
        public String getName() { return "String.split() argument should be escaped"; }
        public String getCategory() { return "STRING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".split(\".\")") || body.contains(".split(\"|\")") || body.contains(".split(\"*\")") ||
                body.contains(".split(\"+\")") || body.contains(".split(\"?\")") || body.contains(".split(\"^\")") ||
                body.contains(".split(\"$\")") || body.contains(".split(\"[\")") || body.contains(".split(\"]\")") ||
                body.contains(".split(\"(\")") || body.contains(".split(\")\")") || body.contains(".split(\"{\")") ||
                body.contains(".split(\"}\")") || body.contains(".split(\"\\\\\")")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Unescaped regex in split()", "use Pattern.quote()"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-17005: substring bounds not checked
     */
    public static class SubstringBounds extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-17005"; }
        public String getName() { return "substring bounds should be validated"; }
        public String getCategory() { return "STRING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".substring(") && !body.contains(".length()") && !body.contains("Math.min") && !body.contains("Math.max")) {
                if (body.split("\\.substring\\(").length > 2) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Multiple substring calls", "validate bounds"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-17006: String.intern() misuse
     */
    public static class StringInternMisuse extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-17006"; }
        public String getName() { return "String.intern() should be used carefully"; }
        public String getCategory() { return "STRING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".intern()")) {
                if (!body.contains("switch") && !body.contains("enum") && !body.contains("constant")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "String.intern() used", "consider alternatives"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-17007: Case-insensitive comparison
     */
    public static class CaseInsensitiveComparison extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-17007"; }
        public String getName() { return "Use equalsIgnoreCase() for case-insensitive comparison"; }
        public String getCategory() { return "STRING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains(".toLowerCase().equals(") || body.contains(".toUpperCase().equals("))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Case conversion for comparison", "use equalsIgnoreCase()"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-17008: StringBuilder initial capacity
     */
    public static class StringBuilderCapacity extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-17008"; }
        public String getName() { return "Set initial capacity for StringBuilder"; }
        public String getCategory() { return "STRING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new StringBuilder()") && body.split("\\.append\\(").length > 10) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "StringBuilder without capacity", "set initial capacity"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-17009: String.format vs concatenation
     */
    public static class StringFormatVsConcatenation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-17009"; }
        public String getName() { return "Use String.format() for complex strings"; }
        public String getCategory() { return "STRING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(" + ") && body.split(" \\+ ").length > 5 && !body.contains("String.format")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Complex string concatenation", "use String.format()"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-17010: String.valueOf vs concatenation
     */
    public static class StringValueOfVsConcatenation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-17010"; }
        public String getName() { return "Use String.valueOf() for object conversion"; }
        public String getCategory() { return "STRING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("\"\" + ") && (body.contains("int") || body.contains("long") || body.contains("double") || body.contains("boolean"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Empty string concatenation", "use String.valueOf()"));
            }
            return issues;
        }
    }
}
