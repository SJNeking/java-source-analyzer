package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * SOLID Principles Rules
 *
 * Detects violations of SOLID principles:
 * - Single Responsibility Principle
 * - Open/Closed Principle
 * - Liskov Substitution Principle
 * - Interface Segregation Principle
 * - Dependency Inversion Principle
 */
public final class SolidRules {
    private SolidRules() {}

    /**
     * RSPEC-10001: Class has too many responsibilities
     */
    public static class SingleResponsibility extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-10001"; }
        public String getName() { return "Class should have single responsibility"; }
        public String getCategory() { return "SOLID"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            // Heuristic: count distinct concepts
            int concepts = 0;
            if (body.contains("db") || body.contains("repository") || body.contains("save")) concepts++;
            if (body.contains("log") || body.contains("logger")) concepts++;
            if (body.contains("email") || body.contains("send") || body.contains("notify")) concepts++;
            if (body.contains("validate") || body.contains("check")) concepts++;
            if (body.contains("transform") || body.contains("convert") || body.contains("format")) concepts++;
            if (concepts >= 3) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Class handles " + concepts + " responsibilities", "split into focused classes"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-10002: Open/Closed violation
     */
    public static class OpenClosedViolation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-10002"; }
        public String getName() { return "Class should be open for extension, closed for modification"; }
        public String getCategory() { return "SOLID"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("instanceof") && body.contains("if") && body.split("instanceof").length > 2) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Type checking with instanceof", "use polymorphism instead"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-10003: Liskov Substitution violation
     */
    public static class LiskovSubstitution extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-10003"; }
        public String getName() { return "Subtypes must be substitutable for base types"; }
        public String getCategory() { return "SOLID"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("@Override") && (body.contains("throw new UnsupportedOperationException") || body.contains("throw new RuntimeException"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Override throws UnsupportedOperationException", "LSP violation"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-10004: Interface Segregation violation
     */
    public static class InterfaceSegregation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-10004"; }
        public String getName() { return "Interface should be client-specific"; }
        public String getCategory() { return "SOLID"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("interface ") && body.split("void ").length > 10) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Interface has too many methods", "split into focused interfaces"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-10005: Dependency Inversion violation
     */
    public static class DependencyInversion extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-10005"; }
        public String getName() { return "Depend on abstractions, not concretions"; }
        public String getCategory() { return "SOLID"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new ArrayList") || body.contains("new HashMap") || body.contains("new LinkedList")) {
                if (body.contains("private ") && (body.contains("ArrayList<") || body.contains("HashMap<"))) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Depends on concrete implementation", "use interface type"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-10006: God object
     */
    public static class GodObject extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-10006"; }
        public String getName() { return "Class knows too much or does too much"; }
        public String getCategory() { return "SOLID"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            String[] lines = body.split("\n");
            int methodCount = 0, fieldCount = 0;
            for (String l : lines) {
                String t = l.trim();
                if (t.startsWith("public ") || t.startsWith("private ") || t.startsWith("protected ")) {
                    if (t.contains("(") && t.contains(")")) methodCount++;
                    else if (!t.contains("(") && !t.contains(")") && t.contains(" ")) fieldCount++;
                }
            }
            if (methodCount > 20 && fieldCount > 10) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "God object: " + methodCount + " methods, " + fieldCount + " fields", "split responsibilities"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-10007: Feature envy
     */
    public static class FeatureEnvy extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-10007"; }
        public String getName() { return "Method uses more features of other classes"; }
        public String getCategory() { return "SOLID"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            long otherClassCalls = 0;
            Matcher otherMatcher = Pattern.compile("\\w+\\.get\\w+\\(\\)|\\w+\\.set\\w+\\(").matcher(body);
            while (otherMatcher.find()) otherClassCalls++;
            long ownClassCalls = 0;
            Matcher ownMatcher = Pattern.compile("this\\.").matcher(body);
            while (ownMatcher.find()) ownClassCalls++;
            if (otherClassCalls > ownClassCalls && otherClassCalls > 5) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Feature envy: " + otherClassCalls + " other class calls vs " + ownClassCalls + " own calls",
                    "move method to other class"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-10008: Divergent change
     */
    public static class DivergentChange extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-10008"; }
        public String getName() { return "Class changes for different reasons"; }
        public String getCategory() { return "SOLID"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            // Detect multiple unrelated responsibilities
            boolean hasDb = body.contains("SELECT") || body.contains("INSERT") || body.contains("repository");
            boolean hasUi = body.contains("UI") || body.contains("View") || body.contains("render");
            boolean hasLogic = body.contains("calculate") || body.contains("process") || body.contains("transform");
            int count = (hasDb ? 1 : 0) + (hasUi ? 1 : 0) + (hasLogic ? 1 : 0);
            if (count >= 2) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Class handles multiple unrelated concerns", "separate concerns"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-10009: Inappropriate intimacy
     */
    public static class InappropriateIntimacy extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-10009"; }
        public String getName() { return "Class knows too much about other class internals"; }
        public String getCategory() { return "SOLID"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            // Detect access to private/internal fields
            Matcher assignmentMatcher = Pattern.compile("\\w+\\.\\w+\\s*=\\s*").matcher(body);
            int assignmentCount = 0;
            while (assignmentMatcher.find()) assignmentCount++;
            if (assignmentCount > 10) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Modifies many fields of other objects", "reduce coupling"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-10010: Data class
     */
    public static class DataClass extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-10010"; }
        public String getName() { return "Class is just a data holder"; }
        public String getCategory() { return "SOLID"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            boolean hasOnlyGettersSetters = body.split("get\\w+\\(\\)|set\\w+\\(").length > 5 &&
                !body.contains("if") && !body.contains("for") && !body.contains("while");
            if (hasOnlyGettersSetters) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Data class: only getters/setters", "add behavior or use record"));
            }
            return issues;
        }
    }
}
