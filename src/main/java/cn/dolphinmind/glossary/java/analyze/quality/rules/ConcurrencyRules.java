package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Concurrency Rules - P4 Priority
 *
 * Detects concurrency anti-patterns:
 * - Thread safety violations
 * - Deadlock risks
 * - Race conditions
 * - Visibility issues
 * - Atomicity violations
 */
public final class ConcurrencyRules {
    private ConcurrencyRules() {}

    // =====================================================================
    // Thread Safety Violations
    // =====================================================================

    /**
     * RSPEC-3067: Synchronization on primitive type
     * Detects synchronization on Integer, Boolean, etc.
     */
    public static class SynchronizedOnPrimitive extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3067"; }
        public String getName() { return "Synchronization should not be based on primitive types"; }
        public String getCategory() { return "CONCURRENCY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            String[] primitives = {"Integer", "Boolean", "Byte", "Short", "Long", "Float", "Double", "String"};
            for (String type : primitives) {
                if (body.contains("synchronized (" + type.toLowerCase())) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Synchronization on " + type, "synchronized " + type));
                    break;
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-3068: Static mutable field not thread-safe
     * Detects static mutable fields accessed without synchronization.
     */
    public static class StaticMutableNotThreadSafe extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3068"; }
        public String getName() { return "Static mutable fields should be thread-safe"; }
        public String getCategory() { return "CONCURRENCY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean hasStaticMutable = body.contains("static") && (body.contains("List<") || body.contains("Map<") ||
                body.contains("Set<") || body.contains("ArrayList") || body.contains("HashMap") || body.contains("HashSet"));
            boolean hasSynchronization = body.contains("synchronized") || body.contains("Lock") ||
                body.contains("ReentrantLock") || body.contains("Concurrent") || body.contains("volatile");

            if (hasStaticMutable && !hasSynchronization) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Static mutable field without synchronization", "static mutable"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Deadlock Risks
    // =====================================================================

    /**
     * RSPEC-3069: Multiple locks acquired in different orders
     * Detects potential deadlock from inconsistent lock ordering.
     */
    public static class InconsistentLockOrdering extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3069"; }
        public String getName() { return "Locks should be acquired in consistent order"; }
        public String getCategory() { return "CONCURRENCY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect nested synchronized blocks (potential lock ordering issue)
            Pattern nestedSync = Pattern.compile("synchronized\\s*\\((\\w+)\\)\\s*\\{[^}]*synchronized\\s*\\((\\w+)\\)");
            Matcher matcher = nestedSync.matcher(body);
            if (matcher.find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Nested synchronized blocks (deadlock risk)", "nested synchronized"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-3070: Lock not released in finally block
     * Detects Lock.lock() without unlock() in finally.
     */
    public static class LockNotReleased extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3070"; }
        public String getName() { return "Locks should be released in finally block"; }
        public String getCategory() { return "CONCURRENCY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean acquiresLock = body.contains(".lock()") || body.contains(".tryLock(");
            boolean releasesInFinally = body.contains("finally") && body.contains(".unlock()");
            boolean usesTryWithLock = body.contains("try (") && body.contains("Lock");

            if (acquiresLock && !releasesInFinally && !usesTryWithLock) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Lock acquired but not released in finally", "lock without unlock"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Race Conditions
    // =====================================================================

    /**
     * RSPEC-3071: Check-then-act race condition
     * Detects non-atomic check-then-act patterns.
     */
    public static class CheckThenActRace extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3071"; }
        public String getName() { return "Check-then-act should be atomic"; }
        public String getCategory() { return "CONCURRENCY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect patterns like: if (!map.containsKey(key)) map.put(key, value);
            Pattern checkThenAct = Pattern.compile("if\\s*\\(\\s*!?\\w+\\.(containsKey|contains|isEmpty)\\s*\\([^)]*\\)\\s*\\)\\s*\\{\\s*\\w+\\.(put|add|remove)\\s*\\(");
            if (checkThenAct.matcher(body).find() && !body.contains("ConcurrentHashMap") && !body.contains("synchronized")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Check-then-act race condition", "check-then-act"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-3072: Double-checked locking
     * Detects double-checked locking without volatile.
     */
    public static class DoubleCheckedLocking extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3072"; }
        public String getName() { return "Double-checked locking should use volatile"; }
        public String getCategory() { return "CONCURRENCY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect double-checked locking pattern
            boolean hasDoubleCheck = body.contains("if") && body.contains("== null") && body.contains("synchronized");
            boolean hasVolatile = body.contains("volatile");

            if (hasDoubleCheck && !hasVolatile) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Double-checked locking without volatile", "DCL without volatile"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Visibility Issues
    // =====================================================================

    /**
     * RSPEC-3073: Shared field without volatile/atomic
     * Detects fields accessed by multiple threads without proper visibility.
     */
    public static class SharedFieldWithoutVolatile extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3073"; }
        public String getName() { return "Shared fields should be volatile or atomic"; }
        public String getCategory() { return "CONCURRENCY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect field accesses that suggest multi-threading without volatile
            boolean hasThreadAccess = body.contains("Thread") || body.contains("Executor") ||
                body.contains("Runnable") || body.contains("Callable") || body.contains("@Async");
            boolean hasVolatileOrAtomic = body.contains("volatile") || body.contains("Atomic") ||
                body.contains("synchronized") || body.contains("Lock");

            if (hasThreadAccess && !hasVolatileOrAtomic && body.contains("this.") && body.contains(" = ")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Shared field accessed by multiple threads without volatile", "field visibility"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Atomicity Violations
    // =====================================================================

    /**
     * RSPEC-3074: Non-atomic compound action
     * Detects compound actions that should be atomic (like i++).
     */
    public static class NonAtomicCompoundAction extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3074"; }
        public String getName() { return "Compound actions should be atomic"; }
        public String getCategory() { return "CONCURRENCY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect i++ in multi-threaded context
            boolean hasIncrement = Pattern.compile("\\w+\\+\\+|\\+\\+\\w+|\\w+\\s*\\+=\\s*\\d+").matcher(body).find();
            boolean hasThreading = body.contains("Thread") || body.contains("Executor") ||
                body.contains("Runnable") || body.contains("synchronized") || body.contains("volatile");

            if (hasIncrement && hasThreading && !body.contains("AtomicInteger") && !body.contains("synchronized")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Non-atomic compound action (use AtomicInteger)", "i++ not atomic"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-3075: Lazy initialization without proper synchronization
     * Detects lazy initialization patterns that aren't thread-safe.
     */
    public static class UnsafeLazyInit extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3075"; }
        public String getName() { return "Lazy initialization should be thread-safe"; }
        public String getCategory() { return "CONCURRENCY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect lazy init pattern without synchronization
            boolean hasLazyInit = body.contains("if") && body.contains("== null") && body.contains("new ");
            boolean hasSynchronization = body.contains("synchronized") || body.contains("volatile") ||
                body.contains("AtomicReference") || body.contains("getInstance()");

            if (hasLazyInit && !hasSynchronization) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Lazy initialization not thread-safe", "unsafe lazy init"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Thread Pool Best Practices
    // =====================================================================

    /**
     * RSPEC-3076: Unbounded thread pool
     * Detects thread pools without maximum size.
     */
    public static class UnboundedThreadPool extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3076"; }
        public String getName() { return "Thread pools should have bounded size"; }
        public String getCategory() { return "CONCURRENCY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("Executors.newCachedThreadPool()") || body.contains("Executors.newWorkStealingPool()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Unbounded thread pool", "newCachedThreadPool"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-3077: Thread.sleep in production code
     * Detects Thread.sleep used for synchronization.
     */
    public static class ThreadSleepInCode extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3077"; }
        public String getName() { return "Thread.sleep should not be used for synchronization"; }
        public String getCategory() { return "CONCURRENCY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("Thread.sleep(")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Thread.sleep used (may indicate synchronization issue)", "Thread.sleep"));
            }
            return issues;
        }
    }
}
