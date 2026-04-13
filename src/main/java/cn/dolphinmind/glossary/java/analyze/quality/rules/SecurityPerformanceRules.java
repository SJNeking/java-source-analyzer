package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * P0/P1 High-Impact Security and Performance Rules.
 *
 * Covers:
 * - IDOR (Insecure Direct Object Reference) - OWASP A01
 * - Mass Assignment / Over-Posting
 * - SimpleDateFormat Thread Safety
 * - Optional.orElse() vs orElseGet() performance
 * - javax-to-jakarta migration
 */
public final class SecurityPerformanceRules {
    private SecurityPerformanceRules() {}

    // =====================================================================
    // SECURITY: IDOR Detection
    // =====================================================================

    /**
     * RSPEC-20011: Insecure Direct Object Reference (IDOR)
     * Detects REST endpoints using path variables directly in database queries
     * without ownership/authorization checks.
     */
    public static class InsecureDirectObjectReference extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20011"; }
        public String getName() { return "Insecure Direct Object Reference detected"; }
        public String getCategory() { return "SECURITY"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Check for REST endpoint annotations
            boolean hasEndpointMapping = body.contains("@GetMapping") || body.contains("@PostMapping") ||
                body.contains("@RequestMapping") || body.contains("@DeleteMapping") ||
                body.contains("@PutMapping") || body.contains("@PatchMapping");

            if (!hasEndpointMapping) return issues;

            // Check for path variable usage in data access
            boolean hasPathVariable = body.contains("@PathVariable") || body.contains("PathVariable");
            boolean hasDataAccess = body.contains(".findById(") || body.contains(".findOne(") ||
                body.contains(".getById(") || body.contains(".getOne(") ||
                body.contains(".deleteById(") || body.contains(".delete(") ||
                body.contains(".findBy") || body.contains(".getBy");

            // Check for authorization/ownership checks
            boolean hasAuthCheck = body.contains("@PreAuthorize") || body.contains("@Secured") ||
                body.contains("hasRole") || body.contains("hasAuthority") ||
                body.contains("isOwner") || body.contains("checkPermission") ||
                body.contains("canAccess") || body.contains("authorize");

            if (hasPathVariable && hasDataAccess && !hasAuthCheck) {
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-20011")
                    .ruleName("Insecure Direct Object Reference")
                    .severity(Severity.CRITICAL)
                    .category("SECURITY")
                    .filePath(fp)
                    .className(cn)
                    .methodName(name)
                    .line(line)
                    .message("Endpoint may allow IDOR - no authorization check before data access")
                    .evidence("@PathVariable + findById without @PreAuthorize")
                    .build());
            }

            return issues;
        }
    }

    // =====================================================================
    // SECURITY: Mass Assignment / Over-Posting
    // =====================================================================

    /**
     * RSPEC-20014: Mass Assignment / Over-Posting
     * Detects @RequestBody bound directly to entity classes with sensitive fields
     * without DTO separation or @JsonIgnoreProperties.
     */
    public static class MassAssignment extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20014"; }
        public String getName() { return "Mass Assignment vulnerability - entity bound directly"; }
        public String getCategory() { return "SECURITY"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Check for @RequestBody with entity-like parameter
            boolean hasRequestBodyEntity = Pattern.compile("@RequestBody\\s+\\w+\\s+\\w+").matcher(body).find();

            if (!hasRequestBodyEntity) return issues;

            // Check for DTO separation (method parameter type contains DTO/Request/Command)
            boolean hasDtoPattern = Pattern.compile("@RequestBody\\s+\\w*(DTO|Request|Command|Form|Input)\\w*\\s+").matcher(body).find();

            // Check for @JsonIgnoreProperties protection
            boolean hasJsonIgnore = body.contains("@JsonIgnoreProperties") ||
                body.contains("JsonIgnoreProperties") ||
                body.contains("@JsonUnwrapped");

            // Check for sensitive field types in the entity
            boolean hasSensitiveFields = body.contains("role") || body.contains("admin") ||
                body.contains("permission") || body.contains("authority") ||
                body.contains("isActive") || body.contains("enabled") ||
                body.contains("password") || body.contains("secret");

            if (hasRequestBodyEntity && !hasDtoPattern && !hasJsonIgnore && hasSensitiveFields) {
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-20014")
                    .ruleName("Mass Assignment vulnerability")
                    .severity(Severity.CRITICAL)
                    .category("SECURITY")
                    .filePath(fp)
                    .className(cn)
                    .methodName(name)
                    .line(line)
                    .message("Entity bound directly to request - use DTO with explicit fields")
                    .evidence("@RequestBody EntityClass")
                    .build());
            }

            return issues;
        }
    }

    // =====================================================================
    // CONCURRENCY: SimpleDateFormat Thread Safety
    // =====================================================================

    /**
     * RSPEC-20015: SimpleDateFormat should not be used as static field
     * SimpleDateFormat is NOT thread-safe. Using it as static field causes
     * silent data corruption in multi-threaded environments.
     */
    public static class SimpleDateFormatThreadSafety extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20015"; }
        public String getName() { return "SimpleDateFormat is not thread-safe"; }
        public String getCategory() { return "CONCURRENCY"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect static SimpleDateFormat usage
            boolean hasStaticSimpleDateFormat = body.contains("static") &&
                (body.contains("SimpleDateFormat") || body.contains("DateFormat"));

            // Detect usage without ThreadLocal
            boolean hasThreadLocal = body.contains("ThreadLocal") ||
                body.contains("DateTimeFormatter") || // Java 8+ alternative
                body.contains("withThreadLocal");

            if (hasStaticSimpleDateFormat && !hasThreadLocal) {
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-20015")
                    .ruleName("SimpleDateFormat is not thread-safe")
                    .severity(Severity.CRITICAL)
                    .category("CONCURRENCY")
                    .filePath(fp)
                    .className(cn)
                    .methodName(name)
                    .line(line)
                    .message("SimpleDateFormat is not thread-safe - use DateTimeFormatter or ThreadLocal")
                    .evidence("static SimpleDateFormat")
                    .build());
            }

            return issues;
        }
    }

    // =====================================================================
    // PERFORMANCE: Optional.orElse() vs orElseGet()
    // =====================================================================

    /**
     * RSPEC-20018: Optional.orElse() should not be used with expensive computation
     * Detects .orElse(expensiveCall()) which eagerly evaluates even when Optional is present.
     * Should use .orElseGet(() -> expensiveCall()) instead.
     */
    public static class OptionalOrElsePerformance extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20018"; }
        public String getName() { return "Optional.orElse() with expensive computation"; }
        public String getCategory() { return "PERFORMANCE"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect .orElse( with method call or new expression (not simple constant/null)
            Pattern orElseExpensive = Pattern.compile("\\.orElse\\(\\s*(new\\s+\\w+|\\w+\\.\\w+\\(|\\w+\\.create\\w*\\(|\\w+\\.build\\(|\\w+\\.get\\w*\\(|loadFrom\\w*\\()");
            Matcher matcher = orElseExpensive.matcher(body);

            while (matcher.find()) {
                // Make sure it's not .orElseGet
                int orElseStart = matcher.start();
                String before = body.substring(0, orElseStart);
                if (!before.endsWith("Get") && !before.endsWith("Get)")) {
                    issues.add(new QualityIssue.Builder()
                        .ruleKey("RSPEC-20018")
                        .ruleName("Optional.orElse() with expensive computation")
                        .severity(Severity.MAJOR)
                        .category("PERFORMANCE")
                        .filePath(fp)
                        .className(cn)
                        .methodName(name)
                        .line(line)
                        .message("orElse() eagerly evaluates argument - use orElseGet() with lambda")
                        .evidence(matcher.group(0))
                        .build());
                    break; // Only report once per method
                }
            }

            return issues;
        }
    }

    // =====================================================================
    // MODERNIZATION: javax-to-jakarta Migration
    // =====================================================================

    /**
     * RSPEC-20021: javax.* packages should be migrated to jakarta.*
     * Detects imports from javax.persistence, javax.servlet, javax.validation
     * which break in Spring Boot 3+ / Jakarta EE.
     */
    public static class JakartaMigration extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20021"; }
        public String getName() { return "javax package should be migrated to jakarta"; }
        public String getCategory() { return "MODERNIZATION"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            String[] deprecatedPackages = {
                "javax.persistence",
                "javax.servlet",
                "javax.validation",
                "javax.annotation",
                "javax.inject",
                "javax.transaction",
                "javax.ws.rs",
                "javax.mail",
                "javax.xml.bind",
                "javax.activation"
            };

            List<String> foundPackages = new ArrayList<>();
            for (String pkg : deprecatedPackages) {
                if (body.contains("import " + pkg + ".") || body.contains("import " + pkg + ";")) {
                    foundPackages.add(pkg);
                }
            }

            if (!foundPackages.isEmpty()) {
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-20021")
                    .ruleName("javax package should be migrated to jakarta")
                    .severity(Severity.MAJOR)
                    .category("MODERNIZATION")
                    .filePath(fp)
                    .className(cn)
                    .methodName(name)
                    .line(line)
                    .message("Deprecated javax package: " + String.join(", ", foundPackages) +
                        " - migrate to jakarta.* for Spring Boot 3+")
                    .evidence("import " + foundPackages.get(0) + ".")
                    .build());
            }

            return issues;
        }
    }

    // =====================================================================
    // PERFORMANCE: Parallel Stream Misuse
    // =====================================================================

    /**
     * RSPEC-20016: Parallel stream should not be used on small collections
     * Detects .parallelStream() on small datasets or with IO-bound operations,
     * which actually degrades performance.
     */
    public static class ParallelStreamMisuse extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20016"; }
        public String getName() { return "Parallel stream misuse on small collection or IO-bound"; }
        public String getCategory() { return "PERFORMANCE"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean hasParallelStream = body.contains(".parallelStream()") ||
                body.contains(".parallel()");

            if (!hasParallelStream) return issues;

            // Detect IO-bound or blocking operations in parallel stream
            boolean hasIOBound = body.contains("Thread.sleep(") || body.contains(".read(") ||
                body.contains(".write(") || body.contains("HttpURLConnection") ||
                body.contains("InputStream") || body.contains("OutputStream") ||
                body.contains("socket") || body.contains("connect(") ||
                body.contains("execute(") || body.contains("query(");

            // Detect small collection patterns
            boolean hasSmallCollection = body.contains("Arrays.asList(") ||
                body.contains("List.of(") || body.contains("Collections.singletonList(") ||
                body.contains("Arrays.stream(");

            if (hasIOBound) {
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-20016")
                    .ruleName("Parallel stream with IO-bound operation")
                    .severity(Severity.MAJOR)
                    .category("PERFORMANCE")
                    .filePath(fp)
                    .className(cn)
                    .methodName(name)
                    .line(line)
                    .message("Parallel stream with IO-bound operation degrades performance")
                    .evidence(".parallelStream() + IO operation")
                    .build());
            } else if (hasSmallCollection) {
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-20016")
                    .ruleName("Parallel stream on small collection")
                    .severity(Severity.MINOR)
                    .category("PERFORMANCE")
                    .filePath(fp)
                    .className(cn)
                    .methodName(name)
                    .line(line)
                    .message("Parallel stream overhead exceeds benefit for small collection")
                    .evidence(".parallelStream() on small collection")
                    .build());
            }

            return issues;
        }
    }

    // =====================================================================
    // BUG: Stream Reuse Detection
    // =====================================================================

    /**
     * RSPEC-20017: Stream should not be reused
     * Detects attempts to consume a Stream more than once, which throws
     * IllegalStateException.
     */
    public static class StreamReuse extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20017"; }
        public String getName() { return "Stream should not be reused"; }
        public String getCategory() { return "BUG"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect: Stream variable assigned, then multiple terminal operations
            Pattern streamAssign = Pattern.compile("Stream<[^>]+>\\s+(\\w+)\\s*=");
            Matcher assignMatcher = streamAssign.matcher(body);

            while (assignMatcher.find()) {
                String streamVar = assignMatcher.group(1);
                // Count terminal operations on same variable
                int terminalOps = 0;
                String afterAssign = body.substring(assignMatcher.end());

                String[] terminalPatterns = {
                    streamVar + "\\.collect\\(",
                    streamVar + "\\.forEach\\(",
                    streamVar + "\\.count\\(",
                    streamVar + "\\.findFirst\\(",
                    streamVar + "\\.findAny\\(",
                    streamVar + "\\.reduce\\(",
                    streamVar + "\\.toArray\\("
                };

                for (String pattern : terminalPatterns) {
                    if (Pattern.compile(pattern).matcher(afterAssign).find()) {
                        terminalOps++;
                    }
                }

                if (terminalOps > 1) {
                    issues.add(new QualityIssue.Builder()
                        .ruleKey("RSPEC-20017")
                        .ruleName("Stream should not be reused")
                        .severity(Severity.MAJOR)
                        .category("BUG")
                        .filePath(fp)
                        .className(cn)
                        .methodName(name)
                        .line(line)
                        .message("Stream consumed multiple times - will throw IllegalStateException")
                        .evidence("Stream<" + streamVar + "> with " + terminalOps + " terminal ops")
                        .build());
                    break;
                }
            }

            return issues;
        }
    }

    // =====================================================================
    // BUG: List.of()/Set.of() Immutability Violation
    // =====================================================================

    /**
     * RSPEC-20020: List.of()/Set.of() result should not be modified
     * Detects factory method collections followed by modification attempts.
     */
    public static class ImmutableCollectionModification extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20020"; }
        public String getName() { return "Immutable collection from factory method should not be modified"; }
        public String getCategory() { return "BUG"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect List.of/Set.of/Map.of assignment
            Pattern immutableAssign = Pattern.compile("(List|Set|Map)\\.of\\([^)]*\\)\\s*(;|->|,\\s*\\w+\\s*=)");
            Matcher matcher = immutableAssign.matcher(body);

            while (matcher.find()) {
                // Check if the assigned variable is later modified
                String afterAssign = body.substring(matcher.end());
                if (afterAssign.contains(".add(") || afterAssign.contains(".remove(") ||
                    afterAssign.contains(".set(") || afterAssign.contains(".clear(") ||
                    afterAssign.contains(".put(")) {

                    issues.add(new QualityIssue.Builder()
                        .ruleKey("RSPEC-20020")
                        .ruleName("Immutable collection modification")
                        .severity(Severity.MAJOR)
                        .category("BUG")
                        .filePath(fp)
                        .className(cn)
                        .methodName(name)
                        .line(line)
                        .message("List.of()/Set.of() creates immutable collection - cannot modify")
                        .evidence(matcher.group(0))
                        .build());
                    break;
                }
            }

            return issues;
        }
    }

    // =====================================================================
    // SECURITY: NoSQL Injection
    // =====================================================================

    /**
     * RSPEC-20012: NoSQL Injection
     * Detects user input concatenated into MongoDB/NoSQL queries.
     */
    public static class NoSqlInjection extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20012"; }
        public String getName() { return "NoSQL Injection vulnerability"; }
        public String getCategory() { return "SECURITY"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect NoSQL query construction with user input
            boolean hasNoSqlDriver = body.contains("MongoCollection") || body.contains("MongoDatabase") ||
                body.contains("BasicDBObject") || body.contains("Document(") ||
                body.contains("Filters.") || body.contains("Query.") ||
                body.contains("Cassandra") || body.contains("Couchbase") ||
                body.contains("Redis") || body.contains("DynamoDB");

            if (!hasNoSqlDriver) return issues;

            // Detect string concatenation in query construction
            boolean hasStringConcatInQuery = Pattern.compile("(find|query|aggregate)\\s*\\(.*\\+.*\\)").matcher(body).find() ||
                Pattern.compile("new\\s+(BasicDBObject|Document)\\s*\\(.*\\+.*\\)").matcher(body).find();

            // Detect request parameter usage in NoSQL context
            boolean hasRequestInput = body.contains("getParameter(") || body.contains("getRequest().") ||
                body.contains("@RequestParam") || body.contains("@RequestBody");

            if (hasNoSqlDriver && (hasStringConcatInQuery || hasRequestInput)) {
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-20012")
                    .ruleName("NoSQL Injection vulnerability")
                    .severity(Severity.CRITICAL)
                    .category("SECURITY")
                    .filePath(fp)
                    .className(cn)
                    .methodName(name)
                    .line(line)
                    .message("User input may be used in NoSQL query without sanitization")
                    .evidence("NoSQL query with external input")
                    .build());
            }

            return issues;
        }
    }

    // =====================================================================
    // CONCURRENCY: Virtual Thread Blocking
    // =====================================================================

    /**
     * RSPEC-20022: Blocking operations should not be used in virtual threads
     * Detects synchronized blocks or Thread.sleep() in virtual thread context,
     * which causes platform thread pinning.
     */
    public static class VirtualThreadBlocking extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20022"; }
        public String getName() { return "Blocking operation in virtual thread"; }
        public String getCategory() { return "CONCURRENCY"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect virtual thread context
            boolean hasVirtualThread = body.contains("Thread.ofVirtual()") ||
                body.contains("newVirtualThreadPerTaskExecutor()") ||
                body.contains("VirtualThread") || body.contains("@VirtualThread");

            if (!hasVirtualThread) return issues;

            // Detect blocking operations that pin virtual threads
            boolean hasBlocking = body.contains("synchronized (") || body.contains("synchronized{") ||
                body.contains("Thread.sleep(") || body.contains(".wait(") ||
                body.contains("Object.wait(") || body.contains("LockSupport.park(");

            if (hasBlocking) {
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-20022")
                    .ruleName("Blocking operation in virtual thread")
                    .severity(Severity.MAJOR)
                    .category("CONCURRENCY")
                    .filePath(fp)
                    .className(cn)
                    .methodName(name)
                    .line(line)
                    .message("Blocking operation pins virtual thread to platform thread - use ReentrantLock")
                    .evidence("synchronized/sleep in virtual thread")
                    .build());
            }

            return issues;
        }
    }

    // =====================================================================
    // MICROSERVICE: Missing Graceful Shutdown
    // =====================================================================

    /**
     * RSPEC-20027: Graceful shutdown should be implemented
     * Detects classes with external connections but no @PreDestroy or cleanup.
     */
    public static class MissingGracefulShutdown extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20027"; }
        public String getName() { return "Graceful shutdown not implemented"; }
        public String getCategory() { return "MICROSERVICE"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect resource that need cleanup
            boolean hasExternalResource = body.contains("Connection") || body.contains("DataSource") ||
                body.contains("KafkaTemplate") || body.contains("RabbitTemplate") ||
                body.contains("JmsTemplate") || body.contains("RestTemplate") ||
                body.contains("WebClient") || body.contains("ExecutorService") ||
                body.contains("ScheduledExecutorService");

            if (!hasExternalResource) return issues;

            // Check for cleanup annotations
            boolean hasCleanup = body.contains("@PreDestroy") || body.contains("DisposableBean") ||
                body.contains("destroy()") || body.contains("close()") ||
                body.contains("shutdown()") || body.contains("@EventListener.*Shutdown");

            if (hasExternalResource && !hasCleanup) {
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-20027")
                    .ruleName("Graceful shutdown not implemented")
                    .severity(Severity.MAJOR)
                    .category("MICROSERVICE")
                    .filePath(fp)
                    .className(cn)
                    .methodName(name)
                    .line(line)
                    .message("External resource without @PreDestroy or cleanup - may leak connections")
                    .evidence("Connection/ExecutorService without @PreDestroy")
                    .build());
            }

            return issues;
        }
    }

    // =====================================================================
    // SECURITY: Server-Side Template Injection (SSTI)
    // =====================================================================

    /**
     * RSPEC-20013: Server-Side Template Injection
     * Detects user input passed directly to template engines without sanitization.
     */
    public static class ServerSideTemplateInjection extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20013"; }
        public String getName() { return "Server-Side Template Injection vulnerability"; }
        public String getCategory() { return "SECURITY"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect template engine usage
            boolean hasTemplateEngine = body.contains("TemplateEngine") || body.contains("templateEngine") ||
                body.contains("FreeMarker") || body.contains("Thymeleaf") ||
                body.contains("Velocity") || body.contains("Mustache") || body.contains("Pebble") ||
                body.contains("processTemplate(") || body.contains("mergeTemplate(");

            if (!hasTemplateEngine) return issues;

            // Detect unsanitized user input in template context
            boolean hasUserInput = body.contains("getParameter(") || body.contains("getRequest().") ||
                body.contains("getAttribute(") || body.contains("@RequestParam");

            boolean hasSanitization = body.contains("HtmlUtils.htmlEscape(") || body.contains("StringEscapeUtils.") ||
                body.contains("sanitize(") || body.contains("encodeHTML(") ||
                body.contains("XSSFilter");

            if (hasTemplateEngine && hasUserInput && !hasSanitization) {
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-20013")
                    .ruleName("Server-Side Template Injection")
                    .severity(Severity.CRITICAL)
                    .category("SECURITY")
                    .filePath(fp)
                    .className(cn)
                    .methodName(name)
                    .line(line)
                    .message("User input passed to template engine without sanitization - RCE risk")
                    .evidence("TemplateEngine + request input")
                    .build());
            }

            return issues;
        }
    }

    // =====================================================================
    // MICROSERVICE: Missing Idempotency
    // =====================================================================

    /**
     * RSPEC-20025: State-changing endpoints should have idempotency
     * Detects POST/PUT endpoints for payments/orders without idempotency key.
     */
    public static class MissingIdempotency extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20025"; }
        public String getName() { return "State-changing endpoint missing idempotency"; }
        public String getCategory() { return "MICROSERVICE"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect state-changing endpoints for critical operations
            boolean isCriticalEndpoint = body.contains("@PostMapping") || body.contains("@PutMapping");
            boolean isCriticalOperation = body.contains("payment") || body.contains("order") ||
                body.contains("charge") || body.contains("transfer") ||
                body.contains("transaction") || body.contains("purchase") ||
                body.contains("refund") || body.contains("withdraw");

            if (!isCriticalEndpoint || !isCriticalOperation) return issues;

            // Check for idempotency mechanisms
            boolean hasIdempotency = body.contains("Idempotency-Key") || body.contains("idempotency") ||
                body.contains("X-Request-ID") || body.contains("X-Idempotency") ||
                body.contains("duplicate") || body.contains("unique") ||
                body.contains("idempotent") || body.contains("X-Correlation");

            if (!hasIdempotency) {
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-20025")
                    .ruleName("State-changing endpoint missing idempotency")
                    .severity(Severity.MAJOR)
                    .category("MICROSERVICE")
                    .filePath(fp)
                    .className(cn)
                    .methodName(name)
                    .line(line)
                    .message("Critical operation without idempotency key - duplicate requests may cause data corruption")
                    .evidence("@PostMapping payment/order without idempotency")
                    .build());
            }

            return issues;
        }
    }

    // =====================================================================
    // CONCURRENCY: ConcurrentHashMap computeIfAbsent Blocking
    // =====================================================================

    /**
     * RSPEC-20029: ConcurrentHashMap computeIfAbsent should not block
     * Detects blocking operations inside computeIfAbsent which can cause deadlocks.
     */
    public static class ConcurrentHashMapBlocking extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20029"; }
        public String getName() { return "ConcurrentHashMap computeIfAbsent should not block"; }
        public String getCategory() { return "CONCURRENCY"; }

        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect computeIfAbsent usage
            if (!body.contains(".computeIfAbsent(")) return issues;

            // Detect blocking operations inside the lambda
            boolean hasBlockingInLambda = body.contains("computeIfAbsent") &&
                (body.contains("Thread.sleep(") || body.contains(".wait(") ||
                 body.contains("synchronized") || body.contains(".lock(") ||
                 body.contains("CountDownLatch") || body.contains("Future.get()"));

            if (hasBlockingInLambda) {
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-20029")
                    .ruleName("ConcurrentHashMap computeIfAbsent should not block")
                    .severity(Severity.CRITICAL)
                    .category("CONCURRENCY")
                    .filePath(fp)
                    .className(cn)
                    .methodName(name)
                    .line(line)
                    .message("Blocking inside computeIfAbsent can cause deadlock")
                    .evidence("computeIfAbsent + blocking operation")
                    .build());
            }

            return issues;
        }
    }
}
