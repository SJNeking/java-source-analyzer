package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collection of all quality rules implemented as inline rule classes.
 * Organized by category: BUG, CODE_SMELL, SECURITY.
 */
public final class RuleCollection {

    private RuleCollection() {}

    // =====================================================================
    // BUG RULES
    // =====================================================================

    public static class EqualsOnFloat extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1244"; }
        public String getName() { return "Float and Double values should not be compared using equals"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".equals(") && (body.contains("Float") || body.contains("Double") || body.contains("float") || body.contains("double"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Float/Double equality check may be imprecise", ".equals on Float/Double"));
            }
            return issues;
        }
    }

    public static class StringLiteralEquality extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4973"; }
        public String getName() { return "Strings should not be compared using == or !="; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("\"[^\"]*\"\\s*[!=]{2}\\s*").matcher(body).find() ||
                Pattern.compile("\\s*[!=]{2}\\s*\"[^\"]*\"").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, "String literal compared with ==/!=, use .equals()", "string literal equality"));
            }
            return issues;
        }
    }

    public static class CollectionIncompatibleType extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2259"; }
        public String getName() { return "Null pointers should not be dereferenced"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            // Pattern: calling method on variable that was checked null before
            if (Pattern.compile("if\\s*\\(\\s*(\\w+)\\s*==\\s*null\\s*\\)").matcher(body).find() &&
                Pattern.compile("(\\w+)\\.\\w+\\(").matcher(body).find()) {
                // Basic check - would need data flow analysis for precision
            }
            return issues;
        }
    }

    public static class IdenticalOperandRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1764"; }
        public String getName() { return "Identical expressions should not be used on both sides of a binary operator"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("(\\w+)\\s*[!=]=\\s*\\1\\b").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Identical operands in comparison", "var == var"));
            }
            return issues;
        }
    }

    public static class DeadStoreRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1854"; }
        public String getName() { return "Dead stores should be removed"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            return Collections.emptyList(); // Requires data flow analysis
        }
    }

    // =====================================================================
    // CODE SMELL RULES
    // =====================================================================

    public static class TooLongMethod extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-138"; }
        public String getName() { return "Methods should not be too long"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            int lineCount = body.isEmpty() ? 0 : body.split("\n").length;
            if (lineCount > 30) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Method is " + lineCount + " lines long (threshold: 30)", "line_count=" + lineCount));
            }
            return issues;
        }
    }

    public static class TooManyParameters extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-107"; }
        public String getName() { return "Methods should not have too many parameters"; }
        public String getCategory() { return "CODE_SMELL"; }
        @SuppressWarnings("unchecked")
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            List<Map<String, String>> params = (List<Map<String, String>>) m.getOrDefault("parameters_inventory", Collections.emptyList());
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (params.size() > 7) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Method has " + params.size() + " parameters (threshold: 7)", "param_count=" + params.size()));
            }
            return issues;
        }
    }

    public static class TooManyReturn extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1142"; }
        public String getName() { return "Jump statements should not occur in too many locations"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            int returnCount = body.split("\\breturn\\b").length - 1;
            if (returnCount > 5) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Method has " + returnCount + " return statements (threshold: 5)", "return_count=" + returnCount));
            }
            return issues;
        }
    }

    public static class CyclomaticComplexityRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3776"; }
        public String getName() { return "Cognitive Complexity of methods should not be too high"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            int complexity = calculateCyclomaticComplexity(body);
            if (complexity > 15) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Cyclomatic complexity is " + complexity + " (threshold: 15)", "cc=" + complexity));
            }
            return issues;
        }

        private int calculateCyclomaticComplexity(String body) {
            int complexity = 1;
            // Each decision point adds 1
            String[] keywords = {"if", "else if", "for", "while", "case", "&&", "||", "catch", "?:", "switch"};
            for (String kw : keywords) {
                Pattern p = Pattern.compile("\\b" + Pattern.quote(kw.trim()) + "\\b");
                Matcher matcher = p.matcher(body);
                while (matcher.find()) complexity++;
            }
            return complexity;
        }
    }

    public static class HardcodedPassword extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2068"; }
        public String getName() { return "Hard-coded passwords are security hotspots"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            String[] patterns = {"password\\s*=\\s*\"[^\"]+\"", "passwd\\s*=\\s*\"[^\"]+\"", "secret\\s*=\\s*\"[^\"]+\"", "apiKey\\s*=\\s*\"[^\"]+\"", "token\\s*=\\s*\"[^\"]+\""};
            for (String pat : patterns) {
                if (Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(body).find()) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                            "Hardcoded password/secret detected", pat));
                    break;
                }
            }
            return issues;
        }
    }

    public static class SQLInjectionRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2077"; }
        public String getName() { return "SQL queries should not be constructed using string concatenation"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            // Detect string concatenation in SQL
            if ((body.contains("SELECT") || body.contains("INSERT") || body.contains("UPDATE") || body.contains("DELETE")) &&
                body.contains("+ \"") || body.contains("\" +")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                        "Potential SQL injection via string concatenation", "SQL + concatenation"));
            }
            return issues;
        }
    }

    public static class UseOfPrintStackTrace extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1148"; }
        public String getName() { return "Throwable.printStackTrace(...) should not be called"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".printStackTrace()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "e.printStackTrace() should be replaced with proper logging", "printStackTrace call"));
            }
            return issues;
        }
    }

    public static class GodClassRule implements QualityRule {
        public String getRuleKey() { return "RSPEC-1444"; }
        public String getName() { return "Classes should not have too many public methods"; }
        public String getCategory() { return "CODE_SMELL"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            String className = (String) classAsset.getOrDefault("address", "");
            String filePath = (String) classAsset.getOrDefault("source_file", "");
            List<Map<String, Object>> methods = (List<Map<String, Object>>) classAsset.getOrDefault("methods_full", Collections.emptyList());
            List<Map<String, Object>> fields = (List<Map<String, Object>>) classAsset.getOrDefault("fields_matrix", Collections.emptyList());

            int methodCount = methods.size();
            int fieldCount = fields.size();

            if (methodCount > 30) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), filePath, className, "", 0,
                        "Class has " + methodCount + " methods (threshold: 30)", "method_count=" + methodCount));
            }
            if (fieldCount > 20) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), filePath, className, "", 0,
                        "Class has " + fieldCount + " fields (threshold: 20)", "field_count=" + fieldCount));
            }
            return issues;
        }
    }

    public static class NoCommentRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-159"; }
        public String getName() { return "Methods should have documentation comments"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String desc = (String) m.getOrDefault("description", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (desc == null || desc.isEmpty() || desc.equals("暂无描述")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Method has no Javadoc comment", "no description"));
            }
            return issues;
        }
    }

    public static class WildcardImportRule implements QualityRule {
        public String getRuleKey() { return "RSPEC-2208"; }
        public String getName() { return "Wildcard imports should not be used"; }
        public String getCategory() { return "CODE_SMELL"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            String className = (String) classAsset.getOrDefault("address", "");
            String filePath = (String) classAsset.getOrDefault("source_file", "");
            List<String> imports = (List<String>) classAsset.getOrDefault("import_dependencies", Collections.emptyList());
            for (String imp : imports) {
                if (imp.endsWith(".*")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), filePath, className, "", 0,
                            "Wildcard import: " + imp, imp));
                }
            }
            return issues;
        }
    }

    public static class SystemOutRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-106"; }
        public String getName() { return "System.out.println should not be used for logging"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("System.out.print") || body.contains("System.err.print")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "System.out.println used instead of proper logging framework", "System.out"));
            }
            return issues;
        }
    }

    public static class ThreadRunRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1217"; }
        public String getName() { return "Thread.run() should not be called directly"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".run()") && (body.contains("Thread") || body.contains("thread"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Thread.run() called directly - use .start() instead", "thread.run()"));
            }
            return issues;
        }
    }

    public static class WaitWhileSynchronizedRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3067"; }
        public String getName() { return "wait() and notify() should only be called in a synchronized context"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            List<String> mods = (List<String>) m.getOrDefault("modifiers", Collections.emptyList());
            if ((body.contains(".wait(") || body.contains(".notify(") || body.contains(".notifyAll(")) &&
                !mods.contains("synchronized")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                        "wait/notify called in non-synchronized method", "wait/notify without synchronized"));
            }
            return issues;
        }
    }

    public static class MutableMembersReturnedRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2384"; }
        public String getName() { return "Mutable members should not be stored or returned directly"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String returnType = (String) m.getOrDefault("return_type", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            // Heuristic: returning array/collection directly without copy
            if ((returnType.contains("[]") || returnType.contains("List") || returnType.contains("Map") || returnType.contains("Set")) &&
                body.matches("(?s).*return\\s+\\w+\\s*;.*") &&
                !body.contains("Collections.unmodifiable") && !body.contains(".toArray(new") && !body.contains(".clone()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Mutable collection/array returned directly without defensive copy", "return " + returnType));
            }
            return issues;
        }
    }

    public static class UnusedLocalVariableRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1481"; }
        public String getName() { return "Unused local variables should be removed"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            return Collections.emptyList(); // Requires semantic analysis
        }
    }

    public static class FinalizerRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1111"; }
        public String getName() { return "finalize() methods should not be used"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ("finalize".equals(name)) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "finalize() is deprecated and should not be used", "finalize method"));
            }
            return issues;
        }
    }

    public static class AssertSideEffectRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3047"; }
        public String getName() { return "Assertions should not contain side effects"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            return Collections.emptyList(); // Requires semantic analysis
        }
    }

    public static class ConstructorOverloadingRule implements QualityRule {
        public String getRuleKey() { return "RSPEC-3400"; }
        public String getName() { return "Constructors should only call accessible constructors or the super constructor"; }
        public String getCategory() { return "CODE_SMELL"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            List<Map<String, Object>> constructors = (List<Map<String, Object>>) classAsset.getOrDefault("constructor_matrix", Collections.emptyList());
            if (constructors.size() > 5) {
                String cn = (String) classAsset.getOrDefault("address", "");
                String fp = (String) classAsset.getOrDefault("source_file", "");
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, "", 0,
                        "Class has " + constructors.size() + " constructors (consider Builder pattern)", "constructor_count=" + constructors.size()));
            }
            return issues;
        }
    }

    public static class HardcodedIPRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1313"; }
        public String getName() { return "Hard-coded IP addresses should not be used"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Hard-coded IP address detected", "IP address literal"));
            }
            return issues;
        }
    }

    public static class LoopExecRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-135"; }
        public String getName() { return "Loop should not branch to the update statement"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            return Collections.emptyList(); // Requires AST analysis
        }
    }

    public static class PublicStaticFieldRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1444"; }
        public String getName() { return "public static fields should be constant"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            return Collections.emptyList(); // Class-level check needed
        }
    }

    public static class EmptyStatementRule extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-012"; }
        public String getName() { return "Empty statements should be removed"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.matches("(?s).*\\;\\s*\\;.*")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Empty statement detected (double semicolon)", ";;"));
            }
            return issues;
        }
    }

    public static class SerialVersionUIDRule implements QualityRule {
        public String getRuleKey() { return "RSPEC-2057"; }
        public String getName() { return "Serializable classes should have a serialVersionUID"; }
        public String getCategory() { return "BUG"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            String kind = (String) classAsset.getOrDefault("kind", "");
            List<String> hierarchy = new ArrayList<>();
            Map<String, List<String>> h = (Map<String, List<String>>) classAsset.get("hierarchy");
            if (h != null && h.get("extends") != null) hierarchy.addAll(h.get("extends"));
            if (h != null && h.get("implements") != null) hierarchy.addAll(h.get("implements"));

            boolean isSerializable = hierarchy.stream().anyMatch(s -> s.contains("Serializable"));
            if ("CLASS".equals(kind) && isSerializable) {
                List<Map<String, Object>> fields = (List<Map<String, Object>>) classAsset.getOrDefault("fields_matrix", Collections.emptyList());
                boolean hasSerialVersionUID = fields.stream().anyMatch(f -> "serialVersionUID".equals(f.get("name")));
                if (!hasSerialVersionUID) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(),
                            (String) classAsset.get("source_file"), (String) classAsset.get("address"), "", 0,
                            "Serializable class missing serialVersionUID", "implements Serializable"));
                }
            }
            return issues;
        }
    }

    public static class DeprecatedCodeUsage extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1874"; }
        public String getName() { return "Deprecated code should not be used"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            // This would need cross-reference analysis
            return issues;
        }
    }

    public static class URLShouldUseHTTPS extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5145"; }
        public String getName() { return "HTTP requests should use HTTPS"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("http://") && !body.contains("https://")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "HTTP URL used instead of HTTPS", "http://"));
            }
            return issues;
        }
    }
}
