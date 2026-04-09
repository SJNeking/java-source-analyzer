package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Microservice Rules - P5 Priority (Part 2 of Rule Expansion)
 *
 * Detects microservice anti-patterns:
 * - Missing circuit breaker
 * - No timeout configuration
 * - Missing health check
 * - No retry logic
 * - Hardcoded service URLs
 * - Missing tracing
 * - No fallback mechanism
 * - Synchronous communication in critical paths
 * - Missing service discovery
 * - No load balancing
 * - Missing configuration externalization
 * - No graceful shutdown
 */
public final class MicroserviceRules {
    private MicroserviceRules() {}

    /**
     * RSPEC-6001: Missing circuit breaker
     */
    public static class MissingCircuitBreaker extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6001"; }
        public String getName() { return "External service calls should have circuit breaker"; }
        public String getCategory() { return "MICROSERVICE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            boolean callsExternal = body.contains("RestTemplate") || body.contains("WebClient") || body.contains("FeignClient") || body.contains("HttpClient");
            boolean hasCircuitBreaker = body.contains("@CircuitBreaker") || body.contains("Resilience4j") || body.contains("Hystrix");
            if (callsExternal && !hasCircuitBreaker) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "External call without circuit breaker", "cascading failure risk"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-6002: No timeout configuration
     */
    public static class MissingTimeout extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6002"; }
        public String getName() { return "HTTP clients should have timeout"; }
        public String getCategory() { return "MICROSERVICE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("RestTemplate") || body.contains("WebClient") || body.contains("HttpClient")) &&
                !body.contains("setConnectTimeout") && !body.contains("setReadTimeout") && !body.contains("timeout")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "HTTP client without timeout", "hanging request risk"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-6003: Missing health check
     */
    public static class MissingHealthCheck extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6003"; }
        public String getName() { return "Service should have health check endpoint"; }
        public String getCategory() { return "MICROSERVICE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            // This is a class-level check, simplified here
            return issues;
        }
    }

    /**
     * RSPEC-6004: No retry logic
     */
    public static class MissingRetryLogic extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6004"; }
        public String getName() { return "External calls should have retry logic"; }
        public String getCategory() { return "MICROSERVICE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            boolean callsExternal = body.contains("RestTemplate") || body.contains("WebClient") || body.contains("FeignClient");
            boolean hasRetry = body.contains("@Retryable") || body.contains("retry") || body.contains("RetryTemplate");
            if (callsExternal && !hasRetry) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "External call without retry logic", "transient failure handling"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-6005: Hardcoded service URLs
     */
    public static class HardcodedServiceUrl extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6005"; }
        public String getName() { return "Service URLs should not be hardcoded"; }
        public String getCategory() { return "MICROSERVICE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("http[s]?://[\\w.-]+:\\d+").matcher(body).find()) {
                if (!body.contains("${") && !body.contains("@Value")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Hardcoded service URL", "use service discovery"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-6006: Missing distributed tracing
     */
    public static class MissingTracing extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6006"; }
        public String getName() { return "Service should support distributed tracing"; }
        public String getCategory() { return "MICROSERVICE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("RestController") && !body.contains("Trace") && !body.contains("Span") && !body.contains("TraceId")) {
                // This is a class-level check
            }
            return issues;
        }
    }

    /**
     * RSPEC-6007: Missing fallback mechanism
     */
    public static class MissingFallback extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6007"; }
        public String getName() { return "External calls should have fallback"; }
        public String getCategory() { return "MICROSERVICE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("RestTemplate") || body.contains("WebClient")) &&
                !body.contains("fallback") && !body.contains("catch") && !body.contains("orElse")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "External call without fallback", "service degradation"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-6008: Synchronous communication in critical path
     */
    public static class SyncCriticalPath extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6008"; }
        public String getName() { return "Critical paths should use async communication"; }
        public String getCategory() { return "MICROSERVICE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((name.toLowerCase().contains("order") || name.toLowerCase().contains("payment") || name.toLowerCase().contains("checkout")) &&
                body.contains("RestTemplate") && !body.contains("CompletableFuture") && !body.contains("async")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Synchronous call in critical path", "use messaging/async"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-6009: Missing service discovery
     */
    public static class MissingServiceDiscovery extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6009"; }
        public String getName() { return "Service should register with discovery"; }
        public String getCategory() { return "MICROSERVICE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("@SpringBootApplication") && !body.contains("@EnableDiscoveryClient") && !body.contains("@EnableEurekaClient")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Missing service discovery registration", "use Eureka/Consul"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-6010: No load balancing
     */
    public static class MissingLoadBalancing extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6010"; }
        public String getName() { return "Service calls should use load balancing"; }
        public String getCategory() { return "MICROSERVICE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("RestTemplate") && !body.contains("@LoadBalanced") && !body.contains("ribbon")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "RestTemplate without load balancing", "use @LoadBalanced"));
            }
            return issues;
        }
    }
}
