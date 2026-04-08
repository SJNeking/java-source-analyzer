package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Performance Rules - P1 Priority
 *
 * Detects common performance anti-patterns:
 * - N+1 queries
 * - Inefficient collections
 * - Memory leaks
 * - Thread pool misconfigurations
 * - Connection pool leaks
 */
public final class PerformanceRules {
    private PerformanceRules() {}

    // =====================================================================
    // N+1 Query Detection
    // =====================================================================

    /**
     * RSPEC-3843: N+1 query pattern detection
     * Detects loops that execute database queries.
     */
    public static class NPlusOneQuery extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3843"; }
        public String getName() { return "N+1 query pattern should be avoided"; }
        public String getCategory() { return "PERFORMANCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean hasLoop = body.contains("for (") || body.contains("while (") || body.contains(".forEach(") || body.contains(".stream(");
            boolean hasQueryInLoop = body.contains(".find(") || body.contains(".findOne(") ||
                body.contains(".findById(") || body.contains(".get(") ||
                body.contains("executeQuery(") || body.contains("select ") ||
                body.contains("entityManager.find(") || body.contains("repository.findById(");

            if (hasLoop && hasQueryInLoop) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "N+1 query pattern: DB query in loop", "query inside loop"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-3844: Missing batch operations
     * Detects multiple individual inserts/updates that should be batched.
     */
    public static class MissingBatchOperation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3844"; }
        public String getName() { return "Batch operations should be used"; }
        public String getCategory() { return "PERFORMANCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Count individual save/insert calls in loop
            int saveInLoopCount = 0;
            String[] loopPatterns = {"for\\s*\\(", "while\\s*\\(", "\\.forEach\\(", "\\.stream\\("};
            for (String loopPattern : loopPatterns) {
                Matcher loopMatcher = Pattern.compile(loopPattern).matcher(body);
                if (loopMatcher.find()) {
                    String afterLoop = body.substring(loopMatcher.start());
                    saveInLoopCount += countOccurrences(afterLoop, ".save(");
                    saveInLoopCount += countOccurrences(afterLoop, ".insert(");
                    saveInLoopCount += countOccurrences(afterLoop, ".update(");
                    saveInLoopCount += countOccurrences(afterLoop, ".executeUpdate(");
                }
            }

            if (saveInLoopCount > 3) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Missing batch operation: " + saveInLoopCount + " individual DB ops in loop", "batch not used"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Inefficient Collections
    // =====================================================================

    /**
     * RSPEC-4438: Inefficient collection initialization
     * Detects ArrayList/HashMap without initial capacity in loops.
     */
    public static class InefficientCollectionInit extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4438"; }
        public String getName() { return "Collections should be initialized with capacity"; }
        public String getCategory() { return "PERFORMANCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean hasLoop = body.contains("for (") || body.contains("while (");
            boolean inefficientInit = body.contains("new ArrayList<>()") || body.contains("new ArrayList(0)") ||
                body.contains("new HashMap<>()") || body.contains("new HashSet<>()") ||
                body.contains("new LinkedHashMap<>()");
            boolean hasAddInLoop = body.contains(".add(") || body.contains(".put(");

            if (hasLoop && inefficientInit && hasAddInLoop) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Collection initialized without capacity in loop", "no initial capacity"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-4439: Inefficient iteration with size() in loop condition
     * Detects calling size() method in loop condition.
     */
    public static class InefficientLoopCondition extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4439"; }
        public String getName() { return "Loop conditions should not call methods"; }
        public String getCategory() { return "PERFORMANCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect: for (int i = 0; i < list.size(); i++)
            Pattern inefficientFor = Pattern.compile("for\\s*\\([^;]*;[^;]*\\w+\\.size\\(\\)");
            if (inefficientFor.matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Inefficient loop: size() in loop condition", "size() in for"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Memory Leak Detection
    // =====================================================================

    /**
     * RSPEC-3845: Static collection not cleared
     * Detects static collections that grow without bounds.
     */
    public static class UnboundedStaticCollection extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3845"; }
        public String getName() { return "Static collections should be bounded"; }
        public String getCategory() { return "PERFORMANCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean addsToStatic = body.contains("static") && (body.contains(".add(") || body.contains(".put("));
            boolean clearsCollection = body.contains(".clear()") || body.contains(".remove(") ||
                body.contains("Collections.empty") || body.contains("new ArrayList") ||
                body.contains("evict(") || body.contains("expire(");

            if (addsToStatic && !clearsCollection) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Static collection grows without bound", "static collection leak"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-3846: Resource not closed in finally block
     * Detects streams, connections, readers not properly closed.
     */
    public static class ResourceNotClosed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3846"; }
        public String getName() { return "Resources should be closed"; }
        public String getCategory() { return "PERFORMANCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean createsResource = body.contains("new FileInputStream(") || body.contains("new FileOutputStream(") ||
                body.contains("new BufferedReader(") || body.contains("Connection") && body.contains("open") ||
                body.contains("Statement") && body.contains("createStatement") ||
                body.contains("new Scanner(") || body.contains("Files.newInputStream(");
            boolean closesResource = body.contains(".close()") || body.contains(".tryWithResources") ||
                body.contains("try (") || body.contains("IOUtils.closeQuietly(");

            if (createsResource && !closesResource) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Resource not closed", "resource leak"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Thread Pool Misconfiguration
    // =====================================================================

    /**
     * RSPEC-4437: Thread pool without proper configuration
     * Detects thread pools with unbounded queues or no rejection policy.
     */
    public static class ThreadPoolMisconfiguration extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4437"; }
        public String getName() { return "Thread pools should be properly configured"; }
        public String getCategory() { return "PERFORMANCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean createsPool = body.contains("new ThreadPoolExecutor(") || body.contains("Executors.newFixedThreadPool(") ||
                body.contains("Executors.newCachedThreadPool(") || body.contains("Executors.newSingleThreadExecutor(");
            boolean usesBoundedQueue = body.contains("new LinkedBlockingQueue(") || body.contains("new ArrayBlockingQueue(") ||
                body.contains("setRejectedExecutionHandler(") || body.contains(".submit(") && body.contains("Future");

            if (createsPool && !usesBoundedQueue) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Thread pool without bounded queue", "unbounded thread pool"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Connection Pool Leak
    // =====================================================================

    /**
     * RSPEC-4436: Connection not returned to pool
     * Detects connections not properly closed.
     */
    public static class ConnectionNotClosed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4436"; }
        public String getName() { return "Database connections should be closed"; }
        public String getCategory() { return "PERFORMANCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean createsConnection = body.contains("dataSource.getConnection(") || body.contains("DriverManager.getConnection(") ||
                body.contains("Connection conn") && body.contains("= ") || body.contains("conn = ") && body.contains("DataSource");
            boolean closesConnection = body.contains("conn.close()") || body.contains("connection.close()") ||
                body.contains("try (") || body.contains(".closeQuietly(");

            if (createsConnection && !closesConnection) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Connection not closed", "connection leak"));
            }
            return issues;
        }
    }

    // =====================================================================
    // String Concatenation in Loop
    // =====================================================================

    /**
     * RSPEC-1643: String concatenation in loop (already exists but enhanced)
     */
    public static class StringConcatInLoopEnhanced extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1643"; }
        public String getName() { return "String concatenation in loop is inefficient"; }
        public String getCategory() { return "PERFORMANCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean hasLoop = body.contains("for (") || body.contains("while (") || body.contains(".forEach(");
            int concatCount = countOccurrences(body, " + \"") + countOccurrences(body, "\" + ");
            boolean usesStringBuilder = body.contains("StringBuilder") || body.contains("StringBuffer");

            if (hasLoop && concatCount > 2 && !usesStringBuilder) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "String concatenation in loop (" + concatCount + " times)", "use StringBuilder"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Utility Methods
    // =====================================================================

    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }
}
