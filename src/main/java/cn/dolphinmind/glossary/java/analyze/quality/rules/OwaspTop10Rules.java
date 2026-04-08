package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * OWASP Top 10 Security Rules - P0 Priority
 *
 * Covers all 10 OWASP Top 10 categories with real detection logic.
 * Each rule uses AST-based pattern matching on method body code.
 */
public final class OwaspTop10Rules {
    private OwaspTop10Rules() {}

    // =====================================================================
    // A01: Broken Access Control
    // =====================================================================

    /**
     * RSPEC-4434: Access controls should be implemented
     * Detects methods that perform privileged operations without authorization checks.
     */
    public static class AccessControlNotImplemented extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4434"; }
        public String getName() { return "Access control should be implemented"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect privileged operations without auth checks
            boolean hasPrivilegedOp = body.contains("delete(") || body.contains("update(") ||
                body.contains("execute(") || body.contains("remove(") || body.contains("drop(") ||
                body.contains("admin") || body.contains("sudo");
            boolean hasAuthCheck = body.contains("isAuthorized") || body.contains("hasPermission") ||
                body.contains("checkAccess") || body.contains("isAuthenticated") ||
                body.contains("@PreAuthorize") || body.contains("@Secured") ||
                body.contains("SecurityContext") || body.contains("Principal");

            if (hasPrivilegedOp && !hasAuthCheck) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Privileged operation without access control", "no auth check"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-5659: Insecure JWT configuration
     * Detects JWT tokens without proper validation.
     */
    public static class InsecureJWTValidation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5659"; }
        public String getName() { return "JWT tokens should be validated"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean usesJWT = body.contains("Jwts.") || body.contains("Jwt.") || body.contains("JWT.") ||
                body.contains("parseClaimsJws") || body.contains("decode(") || body.contains("token");
            boolean validates = body.contains("verify") || body.contains("validate") ||
                body.contains("setSigningKey") || body.contains("require");

            if (usesJWT && !validates) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "JWT used without validation", "no JWT verify"));
            }
            return issues;
        }
    }

    // =====================================================================
    // A02: Cryptographic Failures
    // =====================================================================

    /**
     * RSPEC-4426: TLS certificates should be verified
     * Detects disabled certificate verification.
     */
    public static class TLSVerificationDisabled extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4426"; }
        public String getName() { return "TLS certificates should be verified"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("TrustManager") || body.contains("X509TrustManager") ||
                body.contains("checkClientTrusted") || body.contains("checkServerTrusted") ||
                body.contains("setHostnameVerifier") || body.contains("ALLOW_ALL_HOSTNAME_VERIFIER") ||
                body.contains("NoopHostnameVerifier") || body.contains("SSLContext") && body.contains("init(null")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "TLS certificate verification disabled", "trust all certs"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-4790: Weak cryptographic algorithm
     * Detects use of weak encryption algorithms.
     */
    public static class WeakCryptoAlgorithm extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4790"; }
        public String getName() { return "Weak cryptographic algorithm should not be used"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            String[] weakAlgorithms = {
                "DES", "DESede", "Blowfish", "RC2", "RC4",
                "MD5", "SHA-1", "SHA1", "HmacMD5", "HmacSHA1"
            };

            for (String algo : weakAlgorithms) {
                if (body.contains("\"" + algo) || body.contains("Cipher.getInstance(\"" + algo) ||
                    body.contains("MessageDigest.getInstance(\"" + algo)) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Weak crypto: " + algo, algo));
                    break;
                }
            }
            return issues;
        }
    }

    // =====================================================================
    // A03: Injection
    // =====================================================================

    /**
     * RSPEC-3649: XML external entity injection (XXE)
     * Detects XML parsers with XXE vulnerability.
     */
    public static class XXEInjection extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3649"; }
        public String getName() { return "XML parsers should not be vulnerable to XXE"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean usesXMLParser = body.contains("DocumentBuilder") || body.contains("SAXParser") ||
                body.contains("XMLReader") || body.contains("SAXReader") || body.contains("XMLInputFactory");
            boolean disablesXXE = body.contains("FEATURE_SECURE_PROCESSING") ||
                body.contains("disallow-doctype-decl") || body.contains("ACCESS_EXTERNAL_DTD") ||
                body.contains("ACCESS_EXTERNAL_SCHEMA") || body.contains("setExpandEntityReferences(false)");

            if (usesXMLParser && !disablesXXE) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "XML parser vulnerable to XXE", "no XXE protection"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-5131: Open redirect
     * Detects redirects using user-controlled input.
     */
    public static class OpenRedirect extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5131"; }
        public String getName() { return "Open redirects should not be used"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean hasRedirect = body.contains("sendRedirect(") || body.contains("redirect:") ||
                body.contains("HttpServletResponse.sendRedirect") || body.contains("Response.redirect");
            boolean usesUserInput = body.contains("getParameter(") || body.contains("request.") ||
                body.contains("@RequestParam") || body.contains("header(") || body.contains("getHeader(");

            if (hasRedirect && usesUserInput) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Open redirect using user input", "redirect + user input"));
            }
            return issues;
        }
    }

    // =====================================================================
    // A04: Insecure Design
    // =====================================================================

    /**
     * RSPEC-6419: Unrestricted file upload
     * Detects file uploads without type/size validation.
     */
    public static class UnrestrictedFileUpload extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6419"; }
        public String getName() { return "File uploads should be restricted"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean hasUpload = body.contains("MultipartFile") || body.contains("Part") ||
                body.contains("getInputStream()") && body.contains("upload") ||
                body.contains("transferTo(") || body.contains("write(");
            boolean hasValidation = body.contains("getContentType()") || body.contains("getOriginalFilename()") ||
                body.contains("getSize()") || body.contains("allowedTypes") || body.contains("maxSize") ||
                body.contains("validateFile") || body.contains("checkFile");

            if (hasUpload && !hasValidation) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "File upload without validation", "no file restrictions"));
            }
            return issues;
        }
    }

    // =====================================================================
    // A05: Security Misconfiguration
    // =====================================================================

    /**
     * RSPEC-4507: Security annotations should be configured
     * Detects Spring Security misconfigurations.
     */
    public static class SecurityMisconfiguration extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4507"; }
        public String getName() { return "Security configuration should not disable protections"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            String[] insecureConfigs = {
                ".disable()", ".permitAll()", ".anonymous()",
                "ALLOW_ALL_HOSTNAME_VERIFIER", "TrustAll",
                "setUseInsecureTransport(true)", "setAllowUnsafeRenegotiation(true)"
            };

            for (String config : insecureConfigs) {
                if (body.contains(config)) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Insecure security configuration: " + config, config));
                    break;
                }
            }
            return issues;
        }
    }

    // =====================================================================
    // A06: Vulnerable and Outdated Components
    // =====================================================================

    /**
     * RSPEC-4423: Use of deprecated/insecure methods
     * Detects use of known vulnerable APIs.
     */
    public static class DeprecatedInsecureAPI extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4423"; }
        public String getName() { return "Deprecated insecure APIs should not be used"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            String[] insecureAPIs = {
                "MessageDigest.getInstance(\"MD5\")", "MessageDigest.getInstance(\"SHA-1\")",
                "Cipher.getInstance(\"DES\")", "Cipher.getInstance(\"DESede\")",
                "new java.util.Random()", "Math.random()",
                "new javax.crypto.spec.SecretKeySpec(", "new sun.misc.BASE64Decoder()"
            };

            for (String api : insecureAPIs) {
                if (body.contains(api)) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Insecure API: " + api, api));
                    break;
                }
            }
            return issues;
        }
    }

    // =====================================================================
    // A07: Identification and Authentication Failures
    // =====================================================================

    /**
     * RSPEC-4425: Passwords should not be hardcoded
     * Detects hardcoded credentials.
     */
    public static class PasswordStoredInCode extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4425"; }
        public String getName() { return "Passwords should not be stored in code"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            Pattern passwordPattern = Pattern.compile(
                "(?:password|passwd|pwd|secret|token)\\s*=\\s*\"[^\"]{3,}\"",
                Pattern.CASE_INSENSITIVE);
            if (passwordPattern.matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Hardcoded password", "password = \"...\""));
            }
            return issues;
        }
    }

    /**
     * RSPEC-4424: Passwords should be hashed
     * Detects plaintext password storage.
     */
    public static class PlaintextPassword extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4424"; }
        public String getName() { return "Passwords should be hashed"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean storesPassword = body.contains("setPassword(") || body.contains("password =") ||
                body.contains("savePassword") || body.contains("storePassword") ||
                body.contains("createUser") && body.contains("password");
            boolean hashesPassword = body.contains("BCrypt") || body.contains("MessageDigest") ||
                body.contains("PBKDF2") || body.contains("scrypt") || body.contains("argon2") ||
                body.contains("PasswordEncoder") || body.contains("digest(");

            if (storesPassword && !hashesPassword) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Password stored in plaintext", "no password hashing"));
            }
            return issues;
        }
    }

    // =====================================================================
    // A08: Software and Data Integrity Failures
    // =====================================================================

    /**
     * RSPEC-5122: Insecure deserialization
     * Detects unsafe object deserialization.
     */
    public static class InsecureDeserialization extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5122"; }
        public String getName() { return "Insecure deserialization should not be used"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean deserializes = body.contains("ObjectInputStream") || body.contains("readObject(") ||
                body.contains("XMLDecoder") || body.contains("readUnshared(") ||
                body.contains("Yaml.load(") || body.contains("ObjectMapper.readValue(");
            boolean validates = body.contains("ObjectInputFilter") || body.contains("resolveClass") ||
                body.contains("validateObject") || body.contains("whitelist") ||
                body.contains("allowedClasses");

            if (deserializes && !validates) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Unsafe deserialization", "no deserialization filter"));
            }
            return issues;
        }
    }

    // =====================================================================
    // A09: Security Logging and Monitoring Failures
    // =====================================================================

    /**
     * RSPEC-4423: Logging should not be verbose
     * Detects logging of sensitive information.
     */
    public static class SensitiveInfoLogged extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5146"; }
        public String getName() { return "Sensitive information should not be logged"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            String[] sensitivePatterns = {
                "log.*password", "log.*secret", "log.*token", "log.*credential",
                "info.*password", "debug.*password", "print.*password",
                "password\\)\\.toString\\(\\)", "password\\)\\.getPassword\\("
            };

            for (String pattern : sensitivePatterns) {
                if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(body).find()) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Sensitive information in logs", "logging sensitive data"));
                    break;
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-4502: Exception messages should not reveal stack traces
     * Detects exception message exposure in responses.
     */
    public static class ExceptionMessageExposed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1166"; }
        public String getName() { return "Exception messages should not be exposed to users"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean exposesException = body.contains("e.getMessage()") && (body.contains("response") || body.contains("return") || body.contains("send(")) ||
                body.contains("printStackTrace()") || body.contains("response.getWriter().write(e");
            if (exposesException) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Exception message exposed to user", "e.getMessage() in response"));
            }
            return issues;
        }
    }

    // =====================================================================
    // A10: Server-Side Request Forgery (SSRF)
    // =====================================================================

    /**
     * RSPEC-5144: SSRF should be prevented
     * Detects HTTP requests with user-controlled URLs.
     */
    public static class SSRF extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5144"; }
        public String getName() { return "Server-side request forgery should be prevented"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean makesRequest = body.contains("openConnection(") || body.contains("openStream(") ||
                body.contains("HttpClient") || body.contains("RestTemplate") ||
                body.contains("HttpURLConnection") || body.contains("URLConnection") ||
                body.contains("new URL(") || body.contains("WebClient.");
            boolean usesUserInput = body.contains("getParameter(") || body.contains("request.") ||
                body.contains("url =") && body.contains("request") ||
                body.contains("@RequestParam") || body.contains("queryParam");
            boolean validates = body.contains("allowedHosts") || body.contains("whitelist") ||
                body.contains("URLValidator") || body.contains("isValidUrl") ||
                body.contains("startsWith(\"http") && body.contains("allowed");

            if (makesRequest && usesUserInput && !validates) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "SSRF vulnerability: user-controlled URL", "no URL validation"));
            }
            return issues;
        }
    }
}
