package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Enhanced Security Rules
 *
 * Detects additional security anti-patterns:
 * - CSRF disabled
 * - Insecure cookie
 * - Missing Content-Security-Policy
 * - Information disclosure
 * - Insecure deserialization
 * - Weak hashing
 * - Missing rate limiting
 * - Missing audit logging
 * - Insecure file permissions
 * - Missing authentication
 * - Insecure random
 * - Hardcoded secrets
 * - Insecure CORS
 * - Missing HTTPS
 * - Insecure password storage
 */
public final class SecurityEnhancedRules {
    private SecurityEnhancedRules() {}

    /**
     * RSPEC-11001: CSRF protection disabled
     */
    public static class CsrfDisabled extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11001"; }
        public String getName() { return "CSRF protection should not be disabled"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("csrf().disable()") || (body.contains("csrfCustomizer") && body.contains("disable"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "CSRF protection disabled", "CSRF attack risk"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-11002: Cookie without Secure flag
     */
    public static class CookieWithoutSecure extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11002"; }
        public String getName() { return "Cookie should have Secure flag"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new Cookie(") || body.contains("ResponseCookie")) {
                if (!body.contains("setSecure(true)") && !body.contains("secure(true)")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Cookie without Secure flag", "interception risk"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-11003: Missing Content-Security-Policy
     */
    public static class MissingCsp extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11003"; }
        public String getName() { return "Content-Security-Policy header should be set"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("@Controller") || body.contains("@RestController")) {
                if (!body.contains("Content-Security-Policy") && !body.contains("addHeader") && !body.contains("setHeader")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Missing CSP header", "XSS risk"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-11004: Information disclosure via error messages
     */
    public static class InformationDisclosure extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11004"; }
        public String getName() { return "Error messages should not reveal internal details"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("e.getMessage()") || body.contains("e.printStackTrace()") || body.contains("e.toString()")) &&
                (body.contains("response") || body.contains("return ") || body.contains("sendError"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Internal error details exposed", "information disclosure"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-11005: Weak hashing algorithm
     */
    public static class WeakHashing extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11005"; }
        public String getName() { return "Weak hashing algorithm should not be used"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("MessageDigest.getInstance(\"MD5\")") || body.contains("MessageDigest.getInstance(\"SHA-1\")") ||
                body.contains("DigestUtils.md5")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Weak hash algorithm (MD5/SHA-1)", "use SHA-256 or better"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-11006: Insecure file permissions
     */
    public static class InsecureFilePermissions extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11006"; }
        public String getName() { return "File permissions should be restrictive"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("setReadable(true, false)") || body.contains("setWritable(true, false)") ||
                body.contains("setExecutable(true, false)") || body.contains("PosixFilePermission") && body.contains("OTHERS")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Overly permissive file permissions", "unauthorized access"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-11007: Missing authentication
     */
    public static class MissingAuthentication extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11007"; }
        public String getName() { return "Sensitive endpoints should require authentication"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((name.toLowerCase().contains("admin") || name.toLowerCase().contains("delete") || name.toLowerCase().contains("update")) &&
                (body.contains("@RequestMapping") || body.contains("@PostMapping") || body.contains("@GetMapping"))) {
                if (!body.contains("@PreAuthorize") && !body.contains("@Secured") && !body.contains("Authentication") &&
                    !body.contains("SecurityContextHolder")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Sensitive endpoint without authentication", "unauthorized access"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-11008: Insecure random number generation
     */
    public static class InsecureRandom extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11008"; }
        public String getName() { return "Secure random should be used for security"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("new Random()") || body.contains("Math.random()")) &&
                (body.contains("token") || body.contains("session") || body.contains("password") || body.contains("key") || body.contains("secret"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Insecure random for security purpose", "use SecureRandom"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-11009: Hardcoded secrets
     */
    public static class HardcodedSecrets extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11009"; }
        public String getName() { return "Secrets should not be hardcoded"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            Pattern secretPattern = Pattern.compile("(?:password|secret|api.key|token|auth)\\s*=\\s*\"[^\"]{5,}\"", Pattern.CASE_INSENSITIVE);
            if (secretPattern.matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Hardcoded secret", "use environment variables"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-11010: Insecure CORS configuration
     */
    public static class InsecureCors extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11010"; }
        public String getName() { return "CORS should not allow all origins"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("@CrossOrigin(\"*\")") || body.contains("Access-Control-Allow-Origin: *") ||
                body.contains("allowedOrigins(\"*\")")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "CORS allows all origins", "restrict to trusted domains"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-11011: Missing HTTPS
     */
    public static class MissingHttps extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11011"; }
        public String getName() { return "HTTPS should be enforced"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("http://") && !body.contains("https://") && !body.contains("localhost") && !body.contains("127.0.0.1")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "HTTP used instead of HTTPS", "data interception risk"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-11012: Insecure password storage
     */
    public static class InsecurePasswordStorage extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11012"; }
        public String getName() { return "Passwords should be hashed securely"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("password") && (body.contains("MD5") || body.contains("SHA-1") || body.contains("Base64") ||
                body.contains("plain") || body.contains("clear text"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Insecure password storage", "use BCrypt/Argon2"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-11013: Missing audit logging
     */
    public static class MissingAuditLogging extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11013"; }
        public String getName() { return "Security-sensitive operations should be logged"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((name.toLowerCase().contains("login") || name.toLowerCase().contains("logout") || name.toLowerCase().contains("password") ||
                 name.toLowerCase().contains("register") || name.toLowerCase().contains("admin")) &&
                !body.contains("log.") && !body.contains("logger.") && !body.contains("audit")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Security operation without audit log", "compliance risk"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-11014: XML external entity injection
     */
    public static class XxeInjection extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11014"; }
        public String getName() { return "XML parsers should be configured securely"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("DocumentBuilderFactory") || body.contains("SAXParser") || body.contains("XMLReader")) &&
                !body.contains("FEATURE_SECURE_PROCESSING") && !body.contains("disallow-doctype-decl")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "XML parser vulnerable to XXE", "enable secure processing"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-11015: LDAP injection
     */
    public static class LdapInjection extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-11015"; }
        public String getName() { return "LDAP queries should be parameterized"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("InitialDirContext") || body.contains("ldapTemplate") || body.contains("LdapContext")) {
                if (body.contains("+ \"") || body.contains("String.format") || body.contains("concat")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                        "LDAP injection risk", "use parameterized queries"));
                }
            }
            return issues;
        }
    }
}
