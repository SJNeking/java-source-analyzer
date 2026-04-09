package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Input Validation Rules
 *
 * Detects input validation anti-patterns:
 * - Missing null checks
 * - Missing range validation
 * - Trusting external input
 * - Missing format validation
 * - Missing size limits
 * - Missing type validation
 * - Missing encoding validation
 * - Missing allowlist validation
 */
public final class InputValidationRules {
    private InputValidationRules() {}

    /**
     * RSPEC-8001: Missing null check on method parameter
     */
    public static class MissingNullCheck extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-8001"; }
        public String getName() { return "Method parameters should be checked for null"; }
        public String getCategory() { return "INPUT_VALIDATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            @SuppressWarnings("unchecked")
            List<String> mods = (List<String>) m.getOrDefault("modifiers", Collections.emptyList());
            boolean isPublic = mods.contains("public");
            boolean hasNullCheck = body.contains("Objects.requireNonNull") || body.contains("@NonNull") ||
                body.contains("@NotNull") || body.contains("if") && body.contains("== null") ||
                body.contains("Preconditions.checkNotNull") || body.contains("Objects.requireNonNull");
            if (isPublic && !hasNullCheck && body.contains(".")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Public method without null check", "potential NPE"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-8002: Missing range validation on numeric input
     */
    public static class MissingRangeValidation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-8002"; }
        public String getName() { return "Numeric input should have range validation"; }
        public String getCategory() { return "INPUT_VALIDATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            boolean hasNumericParam = body.contains("int ") || body.contains("long ") || body.contains("double ") || body.contains("float ");
            boolean hasRangeCheck = body.contains("> 0") || body.contains(">= 0") || body.contains("< 0") ||
                body.contains("@Min") || body.contains("@Max") || body.contains("@Range") || body.contains("Preconditions.checkArgument");
            if (hasNumericParam && !hasRangeCheck && (body.contains("@RequestParam") || body.contains("@PathVariable"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Numeric parameter without range validation", "potential overflow/underflow"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-8003: Trusting external input
     */
    public static class TrustingExternalInput extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-8003"; }
        public String getName() { return "External input should be validated"; }
        public String getCategory() { return "INPUT_VALIDATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("request.getParameter") || body.contains("@RequestParam") || body.contains("readLine")) &&
                (body.contains("Runtime.getRuntime") || body.contains("ProcessBuilder") || body.contains("exec("))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "External input used in command execution", "command injection risk"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-8004: Missing format validation
     */
    public static class MissingFormatValidation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-8004"; }
        public String getName() { return "Email/URL/phone should be validated"; }
        public String getCategory() { return "INPUT_VALIDATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            boolean hasEmailParam = body.contains("email") || body.contains("Email");
            boolean hasUrlParam = body.contains("url") || body.contains("Url") || body.contains("URL");
            boolean validatesFormat = body.contains("@Email") || body.contains("@Pattern") || body.contains("Pattern.compile") ||
                body.contains("matches(") || body.contains("@URL");
            if ((hasEmailParam || hasUrlParam) && !validatesFormat) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Format input without validation", "use @Email or @Pattern"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-8005: Missing size limit on string input
     */
    public static class MissingSizeValidation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-8005"; }
        public String getName() { return "String input should have size limit"; }
        public String getCategory() { return "INPUT_VALIDATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("@RequestParam String") || body.contains("String ") && body.contains("@RequestBody")) &&
                !body.contains("@Size") && !body.contains("@Length") && !body.contains(".length()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "String parameter without size limit", "potential DoS"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-8006: Missing type validation
     */
    public static class MissingTypeValidation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-8006"; }
        public String getName() { return "File type should be validated"; }
        public String getCategory() { return "INPUT_VALIDATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("MultipartFile") || body.contains("File upload")) {
                if (!body.contains("getContentType()") && !body.contains("getExtension") && !body.contains("allowedTypes")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "File upload without type validation", "upload malicious file"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-8007: Missing allowlist validation
     */
    public static class MissingAllowlist extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-8007"; }
        public String getName() { return "Input should use allowlist validation"; }
        public String getCategory() { return "INPUT_VALIDATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("redirect:") || body.contains("sendRedirect") || body.contains("forward:")) {
                if (!body.contains("allowedUrls") && !body.contains("validRedirect") && !body.contains("whitelist")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Redirect without allowlist", "open redirect risk"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-8008: Missing encoding validation
     */
    public static class MissingEncodingValidation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-8008"; }
        public String getName() { return "Input encoding should be specified"; }
        public String getCategory() { return "INPUT_VALIDATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("new FileReader") || body.contains("new FileWriter") || body.contains("new InputStreamReader")) &&
                !body.contains("StandardCharsets") && !body.contains("UTF-8") && !body.contains("charset")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "File I/O without encoding", "platform dependent"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-8009: Missing boundary check
     */
    public static class MissingBoundaryCheck extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-8009"; }
        public String getName() { return "Array access should have boundary check"; }
        public String getCategory() { return "INPUT_VALIDATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("[") && body.contains("]") && !body.contains(".length") && !body.contains(".size()")) {
                if (body.contains("args[") || body.contains("array[") || body.contains("list[")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Array/list access without boundary check", "IndexOutOfBoundsException"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-8010: Missing input sanitization
     */
    public static class MissingInputSanitization extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-8010"; }
        public String getName() { return "Input should be sanitized before processing"; }
        public String getCategory() { return "INPUT_VALIDATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            boolean hasUserInput = body.contains("getParameter") || body.contains("getHeader") || body.contains("readLine");
            boolean hasOutput = body.contains("println") || body.contains("write(") || body.contains("append(");
            boolean sanitizes = body.contains("escapeHtml") || body.contains("sanitize") || body.contains("clean") ||
                body.contains("replaceAll") || body.contains("encode");
            if (hasUserInput && hasOutput && !sanitizes) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Input used in output without sanitization", "XSS risk"));
            }
            return issues;
        }
    }
}
