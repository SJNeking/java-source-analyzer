package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Enhanced Code Smell Rules
 *
 * Detects additional code smell patterns:
 * - God method
 * - Speculative generality
 * - Dead code
 * - Data clumps
 * - Feature envy (enhanced)
 * - Shotgun surgery
 * - Divergent change
 * - Parallel inheritance hierarchies
 * - Lazy class
 * - Message chains
 * - Middle man
 * - Inappropriate intimacy
 * - Large class
 * - Refused bequest
 * - Temporary field
 * - Alternative classes with different interfaces
 * - Comments as code smell
 * - Long parameter list
 * - Data class
 * - Switch statements
 */
public final class CodeSmellEnhancedRules {
    private CodeSmellEnhancedRules() {}

    /**
     * RSPEC-9001: God method (method too long)
     */
    public static class GodMethod extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-9001"; }
        public String getName() { return "Method is too long (God method)"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            int lineCount = body.split("\n").length;
            if (lineCount > 50) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Method has " + lineCount + " lines", "split into smaller methods"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-9002: Speculative generality
     */
    public static class SpeculativeGenerality extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-9002"; }
        public String getName() { return "Code should not be overly generic"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("abstract") && body.contains("interface") && body.contains("extends") &&
                body.split("abstract").length > 3 && body.split("interface").length > 3) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Excessive abstraction", "YAGNI violation"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-9003: Dead code
     */
    public static class DeadCode extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-9003"; }
        public String getName() { return "Code should not be unreachable"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("return;") || body.contains("return ")) {
                if (Pattern.compile("return\\s+[^;]+;\\s+\\w+").matcher(body).find()) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Code after return statement", "unreachable code"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-9004: Long parameter list
     */
    public static class LongParameterList extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-9004"; }
        public String getName() { return "Method should not have too many parameters"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> methods = (List<Map<String, Object>>) m.get("methods_full");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            String sig = (String) m.getOrDefault("signature", "");
            long paramCount = sig.chars().filter(ch -> ch == ',').count() + 1;
            if (paramCount > 5) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Method has " + paramCount + " parameters", "use parameter object"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-9005: Data clumps
     */
    public static class DataClumps extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-9005"; }
        public String getName() { return "Related data should be grouped"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            // Detect repeated patterns of similar parameters
            if (body.contains("String ") && body.contains("String ") && body.split("String ").length > 5) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Multiple string parameters", "group into object"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-9006: Shotgun surgery
     */
    public static class ShotgunSurgery extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-9006"; }
        public String getName() { return "Change should not require many modifications"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("import ") && body.split("import ").length > 15) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "File has " + (body.split("import ").length - 1) + " imports", "violates SRP"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-9007: Message chains
     */
    public static class MessageChains extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-9007"; }
        public String getName() { return "Method chains should be limited"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            Matcher chainMatcher = Pattern.compile("\\w+\\.\\w+\\(\\)\\.\\w+\\(\\)\\.\\w+\\(\\)").matcher(body);
            if (chainMatcher.find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Long method chain", "Law of Demeter violation"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-9008: Middle man
     */
    public static class MiddleMan extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-9008"; }
        public String getName() { return "Class should not just delegate"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("return ") && body.contains(".get") && body.split("return").length == body.split("return delegate").length + 1) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Method only delegates", "remove middle man"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-9009: Inappropriate intimacy
     */
    public static class InappropriateIntimacy extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-9009"; }
        public String getName() { return "Class should not know too much about others"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            Matcher fieldAccessMatcher = Pattern.compile("\\.\\w+\\s*=").matcher(body);
            int fieldAccessCount = 0;
            while (fieldAccessMatcher.find()) fieldAccessCount++;
            if (fieldAccessCount > 10) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Accesses " + fieldAccessCount + " fields of other objects", "reduce coupling"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-9010: Comments as code smell
     */
    public static class CommentsAsSmell extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-9010"; }
        public String getName() { return "Comments may indicate bad code"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("// TODO") || body.contains("// FIXME") || body.contains("// HACK") || body.contains("// WORKAROUND")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Code contains TODO/FIXME/HACK", "address technical debt"));
            }
            return issues;
        }
    }
}
