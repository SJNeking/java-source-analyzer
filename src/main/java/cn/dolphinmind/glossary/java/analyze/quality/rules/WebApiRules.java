package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;
import java.util.*;
import java.util.regex.*;

/**
 * Web API Rules - P5 Priority (Part 1 of Rule Expansion)
 *
 * Detects Web/API anti-patterns:
 * - Missing HTTP method specification
 * - No response status code
 * - Missing CORS headers
 * - Path variable injection
 * - No pagination
 * - No input validation
 * - Response body leaks sensitive data
 * - Missing Content-Type
 * - No rate limiting
 * - Error details exposed
 * - Missing API documentation
 * - No request size limit
 */
public final class WebApiRules {
    private WebApiRules() {}

    /**
     * RSPEC-5001: Missing HTTP method specification
     */
    public static class MissingHttpMethod extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5001"; }
        public String getName() { return "HTTP method should be specified"; }
        public String getCategory() { return "WEB_API"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("@RequestMapping") && !body.contains("method =") &&
                !body.contains("@GetMapping") && !body.contains("@PostMapping") &&
                !body.contains("@PutMapping") && !body.contains("@DeleteMapping")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "RequestMapping without HTTP method", "ambiguous mapping"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-5002: Missing response status code
     */
    public static class MissingResponseStatusCode extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5002"; }
        public String getName() { return "Response status code should be explicit"; }
        public String getCategory() { return "WEB_API"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("@PostMapping") || body.contains("@PutMapping") || body.contains("@DeleteMapping")) &&
                !body.contains("@ResponseStatus") && !body.contains("ResponseEntity")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Missing response status", "default 200 OK"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-5003: No CORS headers
     */
    public static class MissingCorsHeaders extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5003"; }
        public String getName() { return "Cross-origin requests should be handled"; }
        public String getCategory() { return "WEB_API"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("@CrossOrigin(\"*\")") || body.contains("Access-Control-Allow-Origin: *")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "CORS allows all origins", "wildcard CORS"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-5004: Path variable injection
     */
    public static class PathVariableInjection extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5004"; }
        public String getName() { return "Path variables should be validated"; }
        public String getCategory() { return "WEB_API"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("@PathVariable") && (body.contains("SELECT") || body.contains("executeQuery") || body.contains("findBy"))) {
                if (!body.contains("validate") && !body.contains("Pattern") && !body.contains("@Size")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "PathVariable used in query without validation", "SQL/NoSQL injection risk"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-5005: No pagination
     */
    public static class MissingPagination extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5005"; }
        public String getName() { return "List endpoints should support pagination"; }
        public String getCategory() { return "WEB_API"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((name.toLowerCase().contains("list") || name.toLowerCase().contains("all") ||
                 name.toLowerCase().contains("findall")) &&
                !body.contains("Pageable") && !body.contains("Page") && !body.contains("limit") &&
                !body.contains("offset") && !body.contains("pageSize")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "List endpoint without pagination", "potential performance issue"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-5006: No input validation
     */
    public static class MissingInputValidation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5006"; }
        public String getName() { return "Request body should be validated"; }
        public String getCategory() { return "WEB_API"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("@RequestBody") && !body.contains("@Valid") && !body.contains("@Validated") &&
                !body.contains("BindingResult") && !body.contains("Errors")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "RequestBody without validation", "missing @Valid"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-5007: Response body leaks sensitive data
     */
    public static class SensitiveDataExposure extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5007"; }
        public String getName() { return "Response should not expose sensitive data"; }
        public String getCategory() { return "WEB_API"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("password") || body.contains("secret") || body.contains("token") || body.contains("credential")) &&
                (body.contains("return") || body.contains("ResponseEntity")) &&
                !body.contains("JsonIgnore") && !body.contains("@Transient") && !body.contains("setPassword(null)")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Potential sensitive data exposure", "return sensitive field"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-5008: Missing Content-Type
     */
    public static class MissingContentType extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5008"; }
        public String getName() { return "Content-Type should be specified"; }
        public String getCategory() { return "WEB_API"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("produces = ") && !body.contains("application/json")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Non-JSON response type", "produces != json"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-5009: No rate limiting
     */
    public static class MissingRateLimiting extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5009"; }
        public String getName() { return "Endpoints should have rate limiting"; }
        public String getCategory() { return "WEB_API"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("@PostMapping") || body.contains("@PutMapping")) &&
                !body.contains("RateLimit") && !body.contains("Bucket4j") && !body.contains("Resilience4j")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Write endpoint without rate limiting", "potential abuse"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-5010: Error details exposed
     */
    public static class ErrorDetailsExposed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5010"; }
        public String getName() { return "Error responses should not expose stack traces"; }
        public String getCategory() { return "WEB_API"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("e.printStackTrace()") || body.contains("e.getMessage()") && body.contains("ResponseEntity")) &&
                !body.contains("Internal Server Error") && !body.contains("error response")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Error details exposed to client", "stack trace exposure"));
            }
            return issues;
        }
    }
}
