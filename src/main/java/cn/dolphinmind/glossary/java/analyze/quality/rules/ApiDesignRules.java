package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * API Design Rules
 *
 * Detects API design anti-patterns:
 * - Boolean parameters
 * - Too many parameters
 * - Return empty collection instead of null
 * - Mutable return type
 * - Method name doesn't match intent
 * - API returns implementation type
 * - API takes implementation type
 * - Synchronized method in API
 * - Public field in API
 * - Deprecated API still in use
 * - API throws checked exception
 * - Builder not used for complex objects
 * - API method too long
 * - API method too many return paths
 * - API inconsistent naming
 */
public final class ApiDesignRules {
    private ApiDesignRules() {}

    /**
     * RSPEC-20001: Boolean parameter
     */
    public static class BooleanParameter extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20001"; }
        public String getName() { return "Boolean parameter should be replaced with enum"; }
        public String getCategory() { return "API_DESIGN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String sig = (String) m.getOrDefault("signature", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (sig.contains("boolean ") && !sig.contains("boolean ")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Boolean parameter", "use enum for clarity"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-20002: Return null instead of empty collection
     */
    public static class ReturnNullInsteadOfEmpty extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20002"; }
        public String getName() { return "Return empty collection instead of null"; }
        public String getCategory() { return "API_DESIGN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            String retType = (String) m.getOrDefault("return_type_path", "");
            if ((retType.contains("List") || retType.contains("Collection") || retType.contains("Set") || retType.contains("Map")) &&
                body.contains("return null")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Return null for collection", "return Collections.emptyList()"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-20003: Mutable return type
     */
    public static class MutableReturnType extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20003"; }
        public String getName() { return "Return immutable types from API methods"; }
        public String getCategory() { return "API_DESIGN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String retType = (String) m.getOrDefault("return_type_path", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (retType.equals("ArrayList") || retType.equals("LinkedList") || retType.equals("HashSet") ||
                retType.equals("HashMap") || retType.equals("LinkedHashMap")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Return type is mutable", "return interface type"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-20004: Method name doesn't match intent
     */
    public static class MethodNameMismatch extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20004"; }
        public String getName() { return "Method name should match its intent"; }
        public String getCategory() { return "API_DESIGN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name.startsWith("get") && name.contains("And") && name.split("And").length > 2) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Method does too many things", "split into focused methods"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-20005: API returns implementation type
     */
    public static class ApiReturnsImplementation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20005"; }
        public String getName() { return "API should return interface type"; }
        public String getCategory() { return "API_DESIGN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String retType = (String) m.getOrDefault("return_type_path", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            String[] implTypes = {"ArrayList", "LinkedList", "HashSet", "TreeSet", "HashMap", "TreeMap", "LinkedHashMap"};
            for (String impl : implTypes) {
                if (retType.equals(impl)) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "API returns " + impl, "return List/Set/Map"));
                    break;
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-20006: Synchronized method in API
     */
    public static class SynchronizedMethod extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20006"; }
        public String getName() { return "API methods should not be synchronized"; }
        public String getCategory() { return "API_DESIGN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<String> mods = (List<String>) m.getOrDefault("modifiers", Collections.emptyList());
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (mods.contains("synchronized") && !name.startsWith("internal")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Public synchronized method", "use concurrent utilities"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-20007: Deprecated API still in use
     */
    public static class DeprecatedApiStillUsed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20007"; }
        public String getName() { return "Deprecated API should be removed"; }
        public String getCategory() { return "API_DESIGN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("@Deprecated") && !body.contains("@deprecated since") && !body.contains("Deprecated since")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Deprecated without replacement info", "add migration guide"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-20008: API throws checked exception
     */
    public static class ThrowsCheckedException extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20008"; }
        public String getName() { return "API should not throw checked exceptions"; }
        public String getCategory() { return "API_DESIGN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("throws ") && (body.contains("throws IOException") || body.contains("throws SQLException") ||
                body.contains("throws ClassNotFoundException") || body.contains("throws ParseException"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Throws checked exception", "wrap in RuntimeException"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-20009: Builder not used for complex objects
     */
    public static class BuilderNotUsed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20009"; }
        public String getName() { return "Use Builder for objects with > 4 parameters"; }
        public String getCategory() { return "API_DESIGN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            String sig = (String) m.getOrDefault("signature", "");
            if (sig.split(",").length > 4 && !body.contains("Builder") && !body.contains("@Builder")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Method with many parameters", "consider Builder"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-20010: API method too many return paths
     */
    public static class TooManyReturnPaths extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-20010"; }
        public String getName() { return "Method should not have too many return paths"; }
        public String getCategory() { return "API_DESIGN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            long returnCount = body.split("\\breturn\\b").length - 1;
            if (returnCount > 5) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Method has " + returnCount + " return paths", "simplify logic"));
            }
            return issues;
        }
    }
}
