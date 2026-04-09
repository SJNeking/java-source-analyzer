package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Logging Rules
 *
 * Detects logging anti-patterns:
 * - Wrong log level
 * - Missing log parameters
 * - Logging sensitive data
 * - String concatenation in logs
 * - Logging in loops
 * - Missing MDC context
 * - Debug in production
 * - Error without context
 * - Using System.out/err
 * - Logging exceptions improperly
 */
public final class LoggingRules {
    private LoggingRules() {}

    /**
     * RSPEC-15001: Using System.out.println
     */
    public static class UsingSystemOut extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-15001"; }
        public String getName() { return "Use logger instead of System.out"; }
        public String getCategory() { return "LOGGING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("System.out.println") || body.contains("System.err.println") ||
                body.contains("System.out.print") || body.contains("System.err.print")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "System.out/err used", "use proper logging"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-15002: String concatenation in log
     */
    public static class StringConcatInLog extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-15002"; }
        public String getName() { return "Use parameterized logging"; }
        public String getCategory() { return "LOGGING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("log.debug(") || body.contains("log.info(") || body.contains("log.warn(") || body.contains("log.error(")) &&
                body.contains(" + ")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "String concatenation in log", "use {} placeholders"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-15003: Logging sensitive data
     */
    public static class LoggingSensitiveData extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-15003"; }
        public String getName() { return "Do not log sensitive data"; }
        public String getCategory() { return "LOGGING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("log.info(") || body.contains("log.debug(") || body.contains("log.trace(")) &&
                (body.contains("password") || body.contains("secret") || body.contains("token") || body.contains("creditCard"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Sensitive data in logs", "mask or remove"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-15004: Exception not logged with stack trace
     */
    public static class ExceptionWithoutStackTrace extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-15004"; }
        public String getName() { return "Exception should be logged with stack trace"; }
        public String getCategory() { return "LOGGING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("catch") && (body.contains("log.error") || body.contains("logger.error") || body.contains("LOG.error"))) {
                if (!body.contains("log.error") || !body.contains("e)")) {
                    if (!body.contains(", e)") && !body.contains(", ex)")) {
                        issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                            "Exception logged without stack trace", "pass exception as parameter"));
                    }
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-15005: Wrong log level for exception
     */
    public static class WrongLogLevel extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-15005"; }
        public String getName() { return "Use appropriate log level"; }
        public String getCategory() { return "LOGGING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("catch") && (body.contains("log.info(") || body.contains("log.debug("))) {
                if (body.contains("Exception") || body.contains("Error") || body.contains("RuntimeException")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Exception logged at wrong level", "use WARN or ERROR"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-15006: Debug in production code
     */
    public static class DebugInProduction extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-15006"; }
        public String getName() { return "Debug logging in production"; }
        public String getCategory() { return "LOGGING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("log.debug(") && body.contains("while") && body.split("log.debug").length > 10) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Excessive debug logging", "consider production impact"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-15007: Logging inside loop
     */
    public static class LoggingInLoop extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-15007"; }
        public String getName() { return "Avoid logging inside loops"; }
        public String getCategory() { return "LOGGING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("for (") || body.contains("while (")) && body.contains("log.")) {
                if (body.split("log\\.").length > 3) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Logging inside loop", "potential performance issue"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-15008: Missing log parameter
     */
    public static class MissingLogParameter extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-15008"; }
        public String getName() { return "Use log parameters instead of concatenation"; }
        public String getCategory() { return "LOGGING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("log.info(\"") && body.contains("\" + ")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "String concatenation in log", "use {} parameter"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-15009: Logger not static final
     */
    public static class LoggerNotStaticFinal extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-15009"; }
        public String getName() { return "Logger should be static final"; }
        public String getCategory() { return "LOGGING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("Logger") && body.contains("LoggerFactory") && !body.contains("static final Logger")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Logger not static final", "performance"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-15010: Log message not internationalized
     */
    public static class LogNotInternationalized extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-15010"; }
        public String getName() { return "Log messages should be clear"; }
        public String getCategory() { return "LOGGING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("log.info(\"") && !body.contains("Logger") && body.split("\"").length > 20) {
                if (body.contains("成功") || body.contains("失败") || body.contains("错误")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Non-English log message", "use English for logs"));
                }
            }
            return issues;
        }
    }
}
