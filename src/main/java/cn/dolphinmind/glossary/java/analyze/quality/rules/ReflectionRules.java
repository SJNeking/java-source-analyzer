package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Reflection and Introspection Rules
 *
 * Detects reflection/introspection anti-patterns:
 * - Reflection used for normal operations
 * - setAccessible(true) without justification
 * - Dynamic class loading
 * - Method.invoke performance issue
 * - Field access via reflection
 * - Proxy misuse
 * - Class.forName without classloader
 */
public final class ReflectionRules {
    private ReflectionRules() {}

    /**
     * RSPEC-21001: setAccessible(true) without justification
     */
    public static class SetAccessible extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-21001"; }
        public String getName() { return "setAccessible(true) bypasses access control"; }
        public String getCategory() { return "REFLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("setAccessible(true)")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "setAccessible(true) used", "security risk"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-21002: Method.invoke performance issue
     */
    public static class MethodInvokePerformance extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-21002"; }
        public String getName() { return "Method.invoke has performance overhead"; }
        public String getCategory() { return "REFLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".invoke(") && body.split("\\.invoke\\(").length > 5) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Frequent Method.invoke calls", "consider MethodHandle"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-21003: Field access via reflection
     */
    public static class ReflectionFieldAccess extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-21003"; }
        public String getName() { return "Field access via reflection is slow"; }
        public String getCategory() { return "REFLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("Field.get(") || body.contains("Field.set(")) && !body.contains("MethodHandle")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Field access via reflection", "use accessor methods"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-21004: Class.forName without classloader
     */
    public static class ClassForNameWithoutLoader extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-21004"; }
        public String getName() { return "Use classloader-aware Class.forName"; }
        public String getCategory() { return "REFLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("Class.forName(") && !body.contains("getClassLoader()") && !body.contains("Thread.currentThread")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Class.forName without classloader", "use Thread.currentThread().getContextClassLoader()"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-21005: Proxy misuse
     */
    public static class ProxyMisuse extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-21005"; }
        public String getName() { return "Dynamic proxy should be used carefully"; }
        public String getCategory() { return "REFLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("Proxy.newProxyInstance(") && !body.contains("InvocationHandler")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Proxy without proper InvocationHandler", "potential bugs"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-21006: Constructor.newInstance performance
     */
    public static class ConstructorNewInstancePerformance extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-21006"; }
        public String getName() { return "Constructor.newInstance has performance overhead"; }
        public String getCategory() { return "REFLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".newInstance(") && body.split("\\.newInstance\\(").length > 3) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Frequent Constructor.newInstance", "cache constructors"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-21007: Reflection in hot path
     */
    public static class ReflectionInHotPath extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-21007"; }
        public String getName() { return "Avoid reflection in performance-critical code"; }
        public String getCategory() { return "REFLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            int reflectionCalls = 0;
            if (body.contains("Class.forName")) reflectionCalls++;
            if (body.contains(".invoke(")) reflectionCalls++;
            if (body.contains(".getField(")) reflectionCalls++;
            if (body.contains(".getMethod(")) reflectionCalls++;
            if (body.contains(".newInstance(")) reflectionCalls++;
            if (reflectionCalls > 3) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Heavy reflection usage", "consider alternatives"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-21008: Dynamic class loading
     */
    public static class DynamicClassLoading extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-21008"; }
        public String getName() { return "Dynamic class loading should be secured"; }
        public String getCategory() { return "REFLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("Class.forName(") && (body.contains("request.") || body.contains("getParameter(") || body.contains("header("))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Dynamic class loading from user input", "code execution risk"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-21009: Method handle not cached
     */
    public static class MethodHandleNotCached extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-21009"; }
        public String getName() { return "MethodHandle should be cached"; }
        public String getCategory() { return "REFLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("MethodHandles.lookup(") && body.split("MethodHandles.lookup").length > 2) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "MethodHandles.lookup called repeatedly", "cache lookup result"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-21010: Array length check missing
     */
    public static class ArrayLengthCheckMissing extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-21010"; }
        public String getName() { return "Array access should check length"; }
        public String getCategory() { return "REFLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("array[") || body.contains("Array.get(")) {
                if (!body.contains(".length") && !body.contains("Array.getLength")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Array access without length check", "ArrayIndexOutOfBounds risk"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-21011: Enum ordinal used
     */
    public static class EnumOrdinalUsed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-21011"; }
        public String getName() { return "Use Enum.name() instead of ordinal()"; }
        public String getCategory() { return "REFLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".ordinal()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Enum.ordinal() used", "use Enum.name()"));
            }
            return issues;
        }
    }
}
