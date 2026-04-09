package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Design Pattern Rules
 *
 * Detects anti-patterns related to design patterns:
 * - Singleton misuse
 * - Builder pattern incomplete
 * - Factory overuse
 * - Observer not used
 * - Strategy not used
 * - Decorator anti-pattern
 * - Template method violation
 * - Visitor pattern misuse
 * - Command pattern missing
 * - Adapter smell
 */
public final class DesignPatternRules {
    private DesignPatternRules() {}

    /**
     * RSPEC-18001: Singleton with public constructor
     */
    public static class SingletonWithPublicConstructor extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-18001"; }
        public String getName() { return "Singleton should have private constructor"; }
        public String getCategory() { return "DESIGN_PATTERN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("getInstance") && body.contains("static") && !body.contains("private Singleton")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Singleton without private constructor", "enforce singleton"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-18002: Builder pattern incomplete
     */
    public static class IncompleteBuilderPattern extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-18002"; }
        public String getName() { return "Builder should have build() method"; }
        public String getCategory() { return "DESIGN_PATTERN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((name.endsWith("Builder") || name.startsWith("Builder")) && !body.contains("build()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Builder without build() method", "complete builder pattern"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-18003: Factory method too complex
     */
    public static class ComplexFactoryMethod extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-18003"; }
        public String getName() { return "Factory method should not be overly complex"; }
        public String getCategory() { return "DESIGN_PATTERN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((name.startsWith("create") || name.startsWith("build") || name.startsWith("make")) && body.split("if").length > 5) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Complex factory method", "consider abstract factory"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-18004: Observer not used
     */
    public static class MissingObserverPattern extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-18004"; }
        public String getName() { return "Use Observer pattern for event handling"; }
        public String getCategory() { return "DESIGN_PATTERN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("for (") && body.contains(".update(") && body.contains("Listener")) {
                if (!body.contains("Observable") && !body.contains("EventBus") && !body.contains("PropertyChangeListener")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Manual observer implementation", "use Observer pattern"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-18005: Strategy not used
     */
    public static class MissingStrategyPattern extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-18005"; }
        public String getName() { return "Use Strategy pattern for algorithm variation"; }
        public String getCategory() { return "DESIGN_PATTERN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.split("else if").length > 3 || body.split("case ").length > 3) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Multiple algorithm branches", "use Strategy pattern"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-18006: Decorator anti-pattern
     */
    public static class DecoratorAntiPattern extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-18006"; }
        public String getName() { return "Decorator should extend base component"; }
        public String getCategory() { return "DESIGN_PATTERN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name.endsWith("Decorator") && !body.contains("extends") && !body.contains("implements")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Decorator without base type", "follow decorator pattern"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-18007: Template method violation
     */
    public static class TemplateMethodViolation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-18007"; }
        public String getName() { return "Template method should be final"; }
        public String getCategory() { return "DESIGN_PATTERN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("abstract") && body.contains("process") || body.contains("execute") || body.contains("run")) {
                if (!body.contains("final") && body.contains("abstract")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Template method not final", "prevent override"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-18008: Visitor pattern misuse
     */
    public static class VisitorMisuse extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-18008"; }
        public String getName() { return "Visitor should not modify visited objects"; }
        public String getCategory() { return "DESIGN_PATTERN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name.endsWith("Visitor") && (body.contains(".set") || body.contains(".add") || body.contains(".remove"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Visitor modifies objects", "visitor should only traverse"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-18009: Command pattern missing
     */
    public static class MissingCommandPattern extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-18009"; }
        public String getName() { return "Use Command pattern for undoable operations"; }
        public String getCategory() { return "DESIGN_PATTERN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name.toLowerCase().contains("undo") || name.toLowerCase().contains("redo") || name.toLowerCase().contains("rollback")) {
                if (!body.contains("Command") && !body.contains("execute") && !body.contains("unexecute")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Undo/redo without Command pattern", "implement Command"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-18010: Adapter smell
     */
    public static class AdapterSmell extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-18010"; }
        public String getName() { return "Adapter should not contain business logic"; }
        public String getCategory() { return "DESIGN_PATTERN"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name.endsWith("Adapter") && (body.contains("if") || body.contains("for") || body.contains("while"))) {
                if (body.split("if").length > 3 || body.split("for").length > 2) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Adapter with complex logic", "separate business logic"));
                }
            }
            return issues;
        }
    }
}
