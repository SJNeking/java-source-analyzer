package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.rules.SecurityPerformanceRules.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for new high-impact security and performance rules.
 */
public class SecurityPerformanceRulesTest extends AbstractRuleTest {

    // =====================================================================
    // IDOR Tests
    // =====================================================================

    @Test
    public void idor_shouldDetectMissingAuthorization() {
        InsecureDirectObjectReference rule = new InsecureDirectObjectReference();
        Map<String, Object> method = createMethod("getUser",
            "@GetMapping(\"/users/{id}\")\n" +
            "public User getUser(@PathVariable Long id) {\n" +
            "    return userRepository.findById(id).orElse(null);\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void idor_shouldNotDetectWithAuthCheck() {
        InsecureDirectObjectReference rule = new InsecureDirectObjectReference();
        Map<String, Object> method = createMethod("getUser",
            "@GetMapping(\"/users/{id}\")\n" +
            "@PreAuthorize(\"@securityService.isOwner(#id)\")\n" +
            "public User getUser(@PathVariable Long id) {\n" +
            "    return userRepository.findById(id).orElse(null);\n" +
            "}\n");
        assertIssues(rule, method, 0);
    }

    // =====================================================================
    // Mass Assignment Tests
    // =====================================================================

    @Test
    public void massAssignment_shouldDetectEntityBinding() {
        MassAssignment rule = new MassAssignment();
        Map<String, Object> method = createMethod("createUser",
            "@PostMapping(\"/users\")\n" +
            "public User createUser(@RequestBody User user) {\n" +
            "    user.setRole(\"admin\");\n" +
            "    return userRepository.save(user);\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void massAssignment_shouldNotDetectWithDTO() {
        MassAssignment rule = new MassAssignment();
        Map<String, Object> method = createMethod("createUser",
            "@PostMapping(\"/users\")\n" +
            "public User createUser(@RequestBody CreateUserRequest request) {\n" +
            "    return userRepository.save(request.toEntity());\n" +
            "}\n");
        assertIssues(rule, method, 0);
    }

    // =====================================================================
    // SimpleDateFormat Thread Safety Tests
    // =====================================================================

    @Test
    public void simpleDateFormat_shouldDetectStaticUsage() {
        SimpleDateFormatThreadSafety rule = new SimpleDateFormatThreadSafety();
        Map<String, Object> method = createMethod("formatDate",
            "private static final SimpleDateFormat sdf = new SimpleDateFormat(\"yyyy-MM-dd\");\n" +
            "public String formatDate(Date date) {\n" +
            "    return sdf.format(date);\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void simpleDateFormat_shouldNotDetectWithThreadLocal() {
        SimpleDateFormatThreadSafety rule = new SimpleDateFormatThreadSafety();
        Map<String, Object> method = createMethod("formatDate",
            "private static final ThreadLocal<SimpleDateFormat> sdf = ThreadLocal.withInitial(\n" +
            "    () -> new SimpleDateFormat(\"yyyy-MM-dd\"));\n");
        assertIssues(rule, method, 0);
    }

    // =====================================================================
    // Optional.orElse() Performance Tests
    // =====================================================================

    @Test
    public void optionalOrElse_shouldDetectExpensiveComputation() {
        OptionalOrElsePerformance rule = new OptionalOrElsePerformance();
        Map<String, Object> method = createMethod("getValue",
            "Optional<String> opt = getValue();\n" +
            "return opt.orElse(loadFromDatabase());\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void optionalOrElse_shouldNotDetectWithOrElseGet() {
        OptionalOrElsePerformance rule = new OptionalOrElsePerformance();
        Map<String, Object> method = createMethod("getValue",
            "Optional<String> opt = getValue();\n" +
            "return opt.orElseGet(() -> loadFromDatabase());\n");
        assertIssues(rule, method, 0);
    }

    // =====================================================================
    // Jakarta Migration Tests
    // =====================================================================

    @Test
    public void jakartaMigration_shouldDetectJavaxImports() {
        JakartaMigration rule = new JakartaMigration();
        Map<String, Object> method = createMethod("test",
            "import javax.persistence.Entity;\n" +
            "import javax.validation.constraints.NotNull;\n" +
            "import org.springframework.stereotype.Service;\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void jakartaMigration_shouldNotDetectJakartaImports() {
        JakartaMigration rule = new JakartaMigration();
        Map<String, Object> method = createMethod("test",
            "import jakarta.persistence.Entity;\n" +
            "import jakarta.validation.constraints.NotNull;\n");
        assertIssues(rule, method, 0);
    }

    // =====================================================================
    // Parallel Stream Misuse Tests
    // =====================================================================

    @Test
    public void parallelStream_shouldDetectIOBound() {
        ParallelStreamMisuse rule = new ParallelStreamMisuse();
        Map<String, Object> method = createMethod("processFiles",
            "files.parallelStream().forEach(f -> {\n" +
            "    InputStream is = new FileInputStream(f);\n" +
            "    is.read(buffer);\n" +
            "});\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void parallelStream_shouldNotDetectOnSequential() {
        ParallelStreamMisuse rule = new ParallelStreamMisuse();
        Map<String, Object> method = createMethod("process",
            "items.stream().map(x -> x * 2).collect(Collectors.toList());\n");
        assertIssues(rule, method, 0);
    }

    // =====================================================================
    // Stream Reuse Tests
    // =====================================================================

    @Test
    public void streamReuse_shouldDetectMultipleTerminalOps() {
        StreamReuse rule = new StreamReuse();
        Map<String, Object> method = createMethod("process",
            "Stream<String> stream = list.stream();\n" +
            "long count = stream.count();\n" +
            "List<String> collected = stream.collect(Collectors.toList());\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void streamReuse_shouldNotDetectSingleTerminalOp() {
        StreamReuse rule = new StreamReuse();
        Map<String, Object> method = createMethod("process",
            "Stream<String> stream = list.stream();\n" +
            "List<String> result = stream.collect(Collectors.toList());\n");
        assertIssues(rule, method, 0);
    }

    // =====================================================================
    // Immutable Collection Modification Tests
    // =====================================================================

    @Test
    public void immutableCollection_shouldDetectModification() {
        ImmutableCollectionModification rule = new ImmutableCollectionModification();
        Map<String, Object> method = createMethod("getList",
            "List<String> list = List.of(\"a\", \"b\", \"c\");\n" +
            "list.add(\"d\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void immutableCollection_shouldNotDetectReadOnly() {
        ImmutableCollectionModification rule = new ImmutableCollectionModification();
        Map<String, Object> method = createMethod("getList",
            "List<String> list = List.of(\"a\", \"b\", \"c\");\n" +
            "return list.stream().collect(Collectors.toList());\n");
        assertIssues(rule, method, 0);
    }

    // =====================================================================
    // NoSQL Injection Tests
    // =====================================================================

    @Test
    public void noSqlInjection_shouldDetectUnsanitizedInput() {
        NoSqlInjection rule = new NoSqlInjection();
        Map<String, Object> method = createMethod("findUser",
            "public User findUser(HttpServletRequest request) {\n" +
            "    String name = request.getParameter(\"name\");\n" +
            "    return collection.find(new Document(\"name\", name)).first();\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    // =====================================================================
    // Virtual Thread Blocking Tests
    // =====================================================================

    @Test
    public void virtualThread_shouldDetectBlocking() {
        VirtualThreadBlocking rule = new VirtualThreadBlocking();
        Map<String, Object> method = createMethod("process",
            "Thread.ofVirtual().start(() -> {\n" +
            "    synchronized (lock) {\n" +
            "        doWork();\n" +
            "    }\n" +
            "});\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void virtualThread_shouldNotDetectNonBlocking() {
        VirtualThreadBlocking rule = new VirtualThreadBlocking();
        Map<String, Object> method = createMethod("process",
            "Thread.ofVirtual().start(() -> {\n" +
            "    ReentrantLock lock = new ReentrantLock();\n" +
            "    lock.lock();\n" +
            "    try { doWork(); } finally { lock.unlock(); }\n" +
            "});\n");
        assertIssues(rule, method, 0);
    }

    // =====================================================================
    // Missing Graceful Shutdown Tests
    // =====================================================================

    @Test
    public void gracefulShutdown_shouldDetectMissingCleanup() {
        MissingGracefulShutdown rule = new MissingGracefulShutdown();
        Map<String, Object> method = createMethod("init",
            "private ExecutorService executor;\n" +
            "public void init() {\n" +
            "    executor = Executors.newFixedThreadPool(10);\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void gracefulShutdown_shouldNotDetectWithPreDestroy() {
        MissingGracefulShutdown rule = new MissingGracefulShutdown();
        Map<String, Object> method = createMethod("shutdown",
            "private ExecutorService executor;\n" +
            "@PreDestroy\n" +
            "public void shutdown() {\n" +
            "    executor.shutdown();\n" +
            "}\n");
        assertIssues(rule, method, 0);
    }

    // =====================================================================
    // SSTI Tests
    // =====================================================================

    @Test
    public void ssti_shouldDetectUnsanitizedInput() {
        ServerSideTemplateInjection rule = new ServerSideTemplateInjection();
        Map<String, Object> method = createMethod("render",
            "public String render(HttpServletRequest request) {\n" +
            "    String name = request.getParameter(\"name\");\n" +
            "    context.setVariable(\"name\", name);\n" +
            "    return templateEngine.process(\"template\", context);\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    // =====================================================================
    // Missing Idempotency Tests
    // =====================================================================

    @Test
    public void idempotency_shouldDetectMissingOnPayment() {
        MissingIdempotency rule = new MissingIdempotency();
        Map<String, Object> method = createMethod("processPayment",
            "@PostMapping(\"/payments\")\n" +
            "public PaymentResult processPayment(@RequestBody PaymentRequest req) {\n" +
            "    return paymentService.charge(req);\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    // =====================================================================
    // ConcurrentHashMap Blocking Tests
    // =====================================================================

    @Test
    public void chmBlocking_shouldDetectInComputeIfAbsent() {
        ConcurrentHashMapBlocking rule = new ConcurrentHashMapBlocking();
        Map<String, Object> method = createMethod("getOrCreate",
            "Map<String, Object> cache = new ConcurrentHashMap<>();\n" +
            "return cache.computeIfAbsent(key, k -> {\n" +
            "    Thread.sleep(1000);\n" +
            "    return expensiveOperation();\n" +
            "});\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void chmBlocking_shouldNotDetectNonBlocking() {
        ConcurrentHashMapBlocking rule = new ConcurrentHashMapBlocking();
        Map<String, Object> method = createMethod("getOrCreate",
            "Map<String, Object> cache = new ConcurrentHashMap<>();\n" +
            "return cache.computeIfAbsent(key, k -> new Object());\n");
        assertIssues(rule, method, 0);
    }
}
