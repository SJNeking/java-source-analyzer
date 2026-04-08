package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Java Modernization Rules - P5 Priority
 *
 * Detects usage of legacy APIs and non-modern Java patterns:
 * - java.util.Date/Calendar vs java.time
 * - Legacy collections (Vector, Hashtable)
 * - Manual resource management vs try-with-resources
 * - Anonymous classes vs Lambdas
 * - String concatenation vs String.join
 */
public final class JavaModernizationRules {
    private JavaModernizationRules() {}

    // =====================================================================
    // Date/Time API
    // =====================================================================

    /**
     * RSPEC-3356: java.util.Date and Calendar should not be used
     * Detects usage of legacy Date/Calendar classes.
     */
    public static class UseLocalDate extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3356"; }
        public String getName() { return "java.time API should be used instead of java.util.Date/Calendar"; }
        public String getCategory() { return "MODERNIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("java.util.Date") || body.contains("new Date(") ||
                body.contains("java.util.Calendar") || body.contains("Calendar.getInstance()") ||
                body.contains("SimpleDateFormat")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Legacy Date/Calendar API used", "use java.time"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Legacy Collections
    // =====================================================================

    /**
     * RSPEC-1319: Legacy collections (Vector, Hashtable) should not be used
     * Detects usage of synchronized legacy collections.
     */
    public static class LegacyCollections extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1319"; }
        public String getName() { return "Legacy synchronized collections should not be used"; }
        public String getCategory() { return "MODERNIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("new Vector") || body.contains("new Hashtable") ||
                body.contains("new Stack(")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Legacy collection used (Vector/Hashtable/Stack)", "use concurrent collections"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-1449: StringBuffer should not be used
     * Detects usage of StringBuffer instead of StringBuilder.
     */
    public static class StringBufferInsteadOfBuilder extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1449"; }
        public String getName() { return "StringBuilder should be used instead of StringBuffer"; }
        public String getCategory() { return "MODERNIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("new StringBuffer") || body.contains("StringBuffer.append")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "StringBuffer used instead of StringBuilder", "use StringBuilder"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Resource Management
    // =====================================================================

    /**
     * RSPEC-2095: Resources should be closed (try-with-resources)
     * Detects resources not using try-with-resources.
     */
    public static class UseTryWithResources extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2095"; }
        public String getName() { return "Try-with-resources should be used"; }
        public String getCategory() { return "MODERNIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean opensResource = body.contains("new FileInputStream") || body.contains("new FileOutputStream") ||
                body.contains("new BufferedReader") || body.contains("new FileReader") ||
                body.contains("Files.newInputStream") || body.contains("Files.newOutputStream") ||
                body.contains("connection.prepareStatement") || body.contains("connection.createStatement");
            boolean usesTryWithResources = body.contains("try (");

            if (opensResource && !usesTryWithResources) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Resource opened without try-with-resources", "use try-with-resources"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Lambdas vs Anonymous Classes
    // =====================================================================

    /**
     * RSPEC-1612: Anonymous classes should be replaced with lambdas
     * Detects anonymous inner classes for functional interfaces.
     */
    public static class AnonymousClassToLambda extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1612"; }
        public String getName() { return "Anonymous classes should be replaced with lambdas"; }
        public String getCategory() { return "MODERNIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("new Runnable") || body.contains("new Callable") ||
                body.contains("new Consumer") || body.contains("new Function") ||
                body.contains("new Predicate") || body.contains("new Comparator") ||
                body.contains("new ActionListener")) {
                // Check if it's a lambda (already modern)
                if (!body.contains("->")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Anonymous class used instead of lambda", "use lambda"));
                }
            }
            return issues;
        }
    }

    // =====================================================================
    // String Operations
    // =====================================================================

    /**
     * RSPEC-1210: String.isEmpty() should be used
     * Detects length() == 0 checks.
     */
    public static class StringLengthCheck extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1210"; }
        public String getName() { return "String.isEmpty() should be used"; }
        public String getCategory() { return "MODERNIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains(".length() == 0") || body.contains(".length() == 0")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "length() == 0 used instead of isEmpty()", "use isEmpty()"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-4423: String.join() should be used
     * Detects manual concatenation in loops or join operations.
     */
    public static class ManualStringJoin extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4423"; }
        public String getName() { return "String.join() should be used"; }
        public String getCategory() { return "MODERNIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect manual join pattern: sb.append(str).append(",")
            if (body.contains("append") && body.contains("\", \"") && body.contains("toString()")) {
                if (!body.contains("String.join")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Manual string join used instead of String.join()", "use String.join"));
                }
            }
            return issues;
        }
    }

    // =====================================================================
    // Wrapper Classes
    // =====================================================================

    /**
     * RSPEC-2201: Constructor of wrapper classes should not be used
     * Detects new Integer(), new Double(), etc.
     */
    public static class WrapperClassConstructor extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2201"; }
        public String getName() { return "Wrapper class constructors should not be used"; }
        public String getCategory() { return "MODERNIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            String[] wrappers = {"Integer", "Long", "Double", "Float", "Boolean", "Byte", "Short", "Character"};
            for (String w : wrappers) {
                if (body.contains("new " + w + "(")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Wrapper constructor used: new " + w + "()", "use valueOf()"));
                    break;
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-4973: Boxed values should not be used for primitives
     * Detects unboxing issues.
     */
    public static class UnnecessaryBoxing extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4973"; }
        public String getName() { return "Boxed values should not be used for primitives"; }
        public String getCategory() { return "MODERNIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("Integer.valueOf") && body.contains(".intValue()") ||
                body.contains("Long.valueOf") && body.contains(".longValue()") ||
                body.contains("Double.valueOf") && body.contains(".doubleValue()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Unnecessary boxing/unboxing", "redundant boxing"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-1905: Cast of boxed type to primitive
     * Detects redundant unboxing.
     */
    public static class RedundantUnboxing extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1905"; }
        public String getName() { return "Redundant unboxing should be removed"; }
        public String getCategory() { return "MODERNIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("intValue()") && body.contains("Integer") ||
                body.contains("longValue()") && body.contains("Long")) {
                // Simplified check: if we see explicit conversion
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Redundant unboxing", "explicit unboxing"));
            }
            return issues;
        }
    }
}
