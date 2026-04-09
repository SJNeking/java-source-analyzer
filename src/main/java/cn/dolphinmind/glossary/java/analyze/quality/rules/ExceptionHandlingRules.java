package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Exception Handling Rules
 *
 * Detects exception handling anti-patterns:
 * - Catching generic Exception
 * - Exception as control flow
 * - Swallowing exceptions
 * - Incomplete exception handling
 * - Logging and throwing
 * - Returning null on exception
 * - Catching Throwable
 * - Using exceptions for business logic
 */
public final class ExceptionHandlingRules {
    private ExceptionHandlingRules() {}

    /**
     * RSPEC-14001: Logging and throwing
     */
    public static class LoggingAndThrowing extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-14001"; }
        public String getName() { return "Exception should not be logged and thrown"; }
        public String getCategory() { return "EXCEPTION_HANDLING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("log.error") || body.contains("logger.error") || body.contains("LOG.error")) {
                if (body.contains("throw") && (body.contains("catch") && body.split("catch").length > 1)) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Exception logged and thrown", "log OR throw"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-14002: Returning null in catch block
     */
    public static class ReturnNullOnException extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-14002"; }
        public String getName() { return "Do not return null in catch block"; }
        public String getCategory() { return "EXCEPTION_HANDLING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("catch") && body.contains("return null")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Returning null on exception", "handle properly"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-14003: Exception used for control flow
     */
    public static class ExceptionForControlFlow extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-14003"; }
        public String getName() { return "Exception should not be used for control flow"; }
        public String getCategory() { return "EXCEPTION_HANDLING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("try {") && body.contains("} catch") && body.split("try \\{").length > 3) {
                if (body.contains("catch") && body.split("catch").length > 4) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Multiple try-catch blocks", "use exceptions for errors, not flow"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-14004: Incomplete catch block
     */
    public static class IncompleteCatch extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-14004"; }
        public String getName() { return "Catch block should handle exception properly"; }
        public String getCategory() { return "EXCEPTION_HANDLING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            Pattern emptyCatch = Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}");
            if (emptyCatch.matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Empty catch block", "handle exception"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-14005: Catching NullPointerException
     */
    public static class CatchNpe extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-14005"; }
        public String getName() { return "Do not catch NullPointerException"; }
        public String getCategory() { return "EXCEPTION_HANDLING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("catch (NullPointerException") || body.contains("catch (NPE")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Catching NullPointerException", "check for null instead"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-14006: Catching IndexOutOfBoundsException
     */
    public static class CatchIndexOutOfBounds extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-14006"; }
        public String getName() { return "Do not catch IndexOutOfBoundsException"; }
        public String getCategory() { return "EXCEPTION_HANDLING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("catch (IndexOutOfBoundsException") || body.contains("catch (ArrayIndexOutOfBoundsException")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Catching IndexOutOfBoundsException", "check bounds instead"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-14007: RuntimeException thrown from constructor
     */
    public static class RuntimeExceptionInConstructor extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-14007"; }
        public String getName() { return "Constructors should not throw RuntimeException"; }
        public String getCategory() { return "EXCEPTION_HANDLING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name != null && name.equals("<init>")) {
                if (body.contains("throw new RuntimeException") || body.contains("throw new IllegalStateException")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Constructor throws exception", "consider factory method"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-14008: Exception message not informative
     */
    public static class UninformativeExceptionMessage extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-14008"; }
        public String getName() { return "Exception message should be informative"; }
        public String getCategory() { return "EXCEPTION_HANDLING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            Pattern genericException = Pattern.compile("throw new \\w+Exception\\(\"(Error|error|Exception|exception|Failed|failed|Problem|problem)\"\\)");
            if (genericException.matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Uninformative exception message", "provide context"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-14009: Exception wrapped without cause
     */
    public static class ExceptionWrappedWithoutCause extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-14009"; }
        public String getName() { return "Exception should preserve original cause"; }
        public String getCategory() { return "EXCEPTION_HANDLING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("catch") && (body.contains("throw new") || body.contains("throw new"))) {
                if (!body.contains("e)") && !body.contains("cause") && !body.contains("initCause") && !body.contains("e)")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Exception wrapped without cause", "preserve stack trace"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-14010: Catching Throwable
     */
    public static class CatchingThrowable extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-14010"; }
        public String getName() { return "Do not catch Throwable"; }
        public String getCategory() { return "EXCEPTION_HANDLING"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("catch (Throwable") || body.contains("catch (Error")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Catching Throwable/Error", "catch specific exceptions"));
            }
            return issues;
        }
    }
}
