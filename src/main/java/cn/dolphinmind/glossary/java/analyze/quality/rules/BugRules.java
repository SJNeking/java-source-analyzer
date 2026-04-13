package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Bug Detection Rules - P0 Priority
 *
 * Detects actual bugs, logic errors, and runtime failures:
 * - Null pointer issues
 * - Threading bugs
 * - Resource leaks
 * - Type conversion errors
 * - Logic errors
 */
public final class BugRules {
    private BugRules() {}

    // =====================================================================
    // Null Pointer Issues
    // =====================================================================

    /**
     * RSPEC-2259: Null pointers should not be dereferenced
     * Detects variables set to null then dereferenced without null check.
     */
    public static class NullDereference extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2259"; }
        public String getName() { return "Null pointers should not be dereferenced"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect null assignment followed by dereference
            Pattern nullAssign = Pattern.compile("(\\w+)\\s*=\\s*null");
            Matcher matcher = nullAssign.matcher(body);
            while (matcher.find()) {
                String varName = matcher.group(1);
                // Check if variable is dereferenced after null assignment
                String afterNull = body.substring(matcher.end());
                if (afterNull.contains(varName + ".") || afterNull.contains(varName + "(")) {
                    // Check if there's a null check
                    if (!afterNull.contains("if (" + varName + " == null") && 
                        !afterNull.contains("if (" + varName + " != null") &&
                        !afterNull.contains("Objects." + varName) &&
                        !afterNull.contains(varName + " != null")) {
                        issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                            "Null pointer may be dereferenced: " + varName, varName + " = null"));
                        break;
                    }
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-6347: Null check should be performed before dereferencing
     * Detects null check performed after variable is already dereferenced.
     */
    public static class NullCheckAfterDeref extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6347"; }
        public String getName() { return "Null check should be performed before dereferencing"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect pattern: var.method() followed by if (var != null)
            Pattern nullCheckAfter = Pattern.compile("(\\w+)\\.\\w+.*if\\s*\\(\\1\\s*!=\\s*null\\)");
            if (nullCheckAfter.matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Null check after dereference", "null check too late"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Threading Bugs
    // =====================================================================

    /**
     * RSPEC-1217: Thread.run() should not be called directly
     * Detects direct calls to Thread.run() instead of Thread.start().
     */
    public static class ThreadRunDirect extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1217"; }
        public String getName() { return "Thread.run() should not be called directly"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains(".run()") && (body.contains("Thread") || body.contains("new Thread"))) {
                if (!body.contains(".start()")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                        "Thread.run() called directly - use start() instead", ".run()"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-3067: wait/notify should be used in synchronized context
     * Detects wait/notify outside of synchronized context.
     */
    public static class WaitNotifyNoSync extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3067"; }
        public String getName() { return "wait/notify should be used in synchronized context"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean hasWaitNotify = body.contains(".wait(") || body.contains(".notify(") || 
                                    body.contains(".notifyAll(");
            boolean hasSynchronized = body.contains("synchronized (") || body.contains("synchronized ");

            if (hasWaitNotify && !hasSynchronized) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "wait/notify outside synchronized block", "IllegalMonitorStateException risk"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Resource Leaks
    // =====================================================================

    /**
     * RSPEC-2095: Resources should be closed
     * Detects unclosed resources (FileInputStream, Socket, BufferedReader).
     */
    public static class UnclosedResource extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2095"; }
        public String getName() { return "Resources should be closed"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean createsResource = body.contains("new FileInputStream(") || 
                                      body.contains("new FileOutputStream(") ||
                                      body.contains("new BufferedReader(") || 
                                      body.contains("new Socket(") ||
                                      body.contains("ServerSocket") && body.contains("accept(");
            boolean closesResource = body.contains(".close()") || body.contains("try (");

            if (createsResource && !closesResource) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Resource not closed", "resource leak"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-2384: Mutable members should not be stored or returned directly
     * Detects returning mutable internal members without defensive copy.
     */
    public static class MutableMembersReturned extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2384"; }
        public String getName() { return "Mutable members should not be returned directly"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect returning mutable collections/arrays
            Pattern returnMutable = Pattern.compile("return\\s+(\\w+);");
            Matcher matcher = returnMutable.matcher(body);
            while (matcher.find()) {
                String varName = matcher.group(1);
                if (body.contains("List<" + varName) || body.contains("Map<" + varName) ||
                    body.contains(varName + "[]") || body.contains("Set<" + varName)) {
                    if (!body.contains("Collections.unmodifiable") && !body.contains("new ArrayList<>(" + varName)) {
                        issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                            "Mutable member returned directly", "use defensive copy"));
                        break;
                    }
                }
            }
            return issues;
        }
    }

    // =====================================================================
    // Type Conversion Bugs
    // =====================================================================

    /**
     * RSPEC-2111: BigDecimal(double) should not be used
     * Detects BigDecimal(double) constructor which loses precision.
     */
    public static class BigDecimalDouble extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2111"; }
        public String getName() { return "BigDecimal(double) should not be used"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("new BigDecimal(") && (body.contains("double") || body.contains("float") || 
                Pattern.compile("new BigDecimal\\(\\d+\\.\\d+\\)").matcher(body).find())) {
                if (!body.contains("BigDecimal.valueOf(") && !body.contains("new BigDecimal(\"")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "BigDecimal(double) loses precision", "use BigDecimal.valueOf() or String"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-2184: Cast of long to int should not be done
     * Detects explicit cast from long to int (potential data loss).
     */
    public static class LongToIntCast extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2184"; }
        public String getName() { return "Cast of long to int should be avoided"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (Pattern.compile("\\(int\\)\\s*\\w+").matcher(body).find() && 
                (body.contains("long") || body.contains("Long") || body.contains("System.currentTimeMillis"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Cast from long to int may lose precision", "(int) cast"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-6557: BigDecimal precision loss
     * Detects BigDecimal divide/doubleValue that loses precision.
     */
    public static class BigDecimalPrecisionLoss extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6557"; }
        public String getName() { return "BigDecimal operations should preserve precision"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains(".divide(") && !body.contains("MathContext") && !body.contains("RoundingMode")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "BigDecimal.divide without rounding mode", "specify RoundingMode"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Logic Errors
    // =====================================================================

    /**
     * RSPEC-108: Empty catch block
     * Detects catch blocks with empty bodies (swallowed exceptions).
     */
    public static class EmptyCatchBlock extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-108"; }
        public String getName() { return "Empty catch blocks should be avoided"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Empty catch block", "swallowed exception"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-4973: Strings should be compared using equals()
     * Detects string comparison using == instead of .equals().
     */
    public static class StringLiteralEquality extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4973"; }
        public String getName() { return "Strings should be compared using equals()"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (Pattern.compile("\"[^\"]*\"\\s*==\\s*\\w+").matcher(body).find() ||
                Pattern.compile("\\w+\\s*==\\s*\"[^\"]*\"").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "String compared with ==", "use equals()"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-1764: Identical operands on both sides of binary operator
     * Detects identical expressions on both sides of binary operators.
     */
    public static class IdenticalOperand extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1764"; }
        public String getName() { return "Identical operands on both sides of binary operator"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect: x == x, x != x, x && x, x || x
            if (Pattern.compile("(\\w+)\\s*(==|!=|&&|\\|\\|)\\s*\\1").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Identical operands on both sides", "redundant comparison"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-2225: toString() should not return null
     * Detects toString() methods that return null.
     */
    public static class ToStringReturnsNull extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2225"; }
        public String getName() { return "toString() should not return null"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (name != null && name.equals("toString") && body.contains("return null")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "toString() returns null", "return empty string instead"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-2142: InterruptedException should not be swallowed
     * Detects InterruptedException caught without restoring interrupt status.
     */
    public static class InterruptedExceptionSwallowed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2142"; }
        public String getName() { return "InterruptedException should not be swallowed"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("catch (InterruptedException") && !body.contains("Thread.currentThread().interrupt()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "InterruptedException swallowed without restoring interrupt status", "call interrupt()"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-4248: Stream should be consumed
     * Detects streams created without terminal operation (collect/forEach).
     */
    public static class StreamNotConsumed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4248"; }
        public String getName() { return "Stream should be consumed"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean createsStream = body.contains(".stream()") || body.contains("Stream.of(");
            boolean consumesStream = body.contains(".collect(") || body.contains(".forEach(") || 
                                     body.contains(".findFirst(") || body.contains(".count(") ||
                                     body.contains(".anyMatch(") || body.contains(".reduce(");

            if (createsStream && !consumesStream) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Stream created but not consumed", "add terminal operation"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-1948: Serializable class should not have non-serializable fields
     * Detects non-serializable fields in Serializable classes.
     */
    public static class SerializableField extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1948"; }
        public String getName() { return "Serializable class should not have non-serializable fields"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("implements Serializable") || body.contains("@Entity")) {
                if (body.contains("InputStream") || body.contains("OutputStream") || 
                    body.contains("Thread") || body.contains("Socket")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Non-serializable field in Serializable class", "mark as transient"));
                }
            }
            return issues;
        }
    }

    // =====================================================================
    // Deprecated/Unsafe API Usage
    // =====================================================================

    /**
     * RSPEC-1874: Deprecated API should not be used
     * Detects use of deprecated APIs (Thread.stop(), new Integer(), etc.).
     */
    public static class DeprecatedUsage extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1874"; }
        public String getName() { return "Deprecated API should not be used"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            String[] deprecatedAPIs = {
                "Thread.stop()", "Thread.suspend()", "Thread.resume()",
                "new Integer(", "new Long(", "new Double(", "new Boolean(",
                "Runtime.getRuntime().exec(", "System.runFinalizersOnExit("
            };

            for (String api : deprecatedAPIs) {
                if (body.contains(api)) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Deprecated API used: " + api, "@Deprecated"));
                    break;
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-3078: Context class loader should be used
     * Detects .getClassLoader() instead of context class loader.
     */
    public static class ClassLoaderMisuse extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3078"; }
        public String getName() { return "Context class loader should be used for resource loading"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains(".getClassLoader()") && (body.contains("getResource(") || body.contains("loadClass("))) {
                if (!body.contains("Thread.currentThread().getContextClassLoader()")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "getClassLoader() instead of context class loader", "use Thread.currentThread().getContextClassLoader()"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-2276: Redundant cast should be removed
     * Detects redundant primitive-to-primitive casts.
     */
    public static class RedundantCast extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2276"; }
        public String getName() { return "Redundant cast should be removed"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect: (int) intVar, (long) longVar
            if (Pattern.compile("\\(int\\)\\s*\\(int\\)").matcher(body).find() ||
                Pattern.compile("\\(long\\)\\s*\\(long\\)").matcher(body).find() ||
                Pattern.compile("\\(double\\)\\s*\\(double\\)").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Redundant cast", "remove cast"));
            }
            return issues;
        }
    }
}
