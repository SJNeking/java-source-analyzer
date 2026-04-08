package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * All quality rules - 100 rules, ALL with real detection logic.
 * NO stubs. Every rule does actual pattern/structure analysis.
 */
public final class AllRules {
    private AllRules() {}

    // =====================================================================
    // BUG RULES
    // =====================================================================

    public static class EmptyCatchBlock extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-108"; }
        public String getName() { return "Empty catch block should not be used"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.isEmpty()) return issues;
            if (Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{(\\s*//[^\\n]*|\\s*/\\*[^*]*\\*/\\s*)*\\}").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Empty catch block", "catch{}"));
            }
            return issues;
        }
    }

    public static class StringLiteralEquality extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4973"; }
        public String getName() { return "Strings should not be compared using =="; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("\"[^\"]*\"\\s*[!=]=\\s*\\w+").matcher(body).find() || Pattern.compile("\\w+\\s*[!=]=\\s*\"[^\"]*\"").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, "String compared with ==", "string equality"));
            }
            return issues;
        }
    }

    public static class IdenticalOperand extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1764"; }
        public String getName() { return "Identical expressions on both sides of operator"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            Matcher matcher = Pattern.compile("(\\w+(?:\\.\\w+)*)\\s*(?:==|!=|&&|\\|\\||\\+|-|\\*|/)\\s*\\1\\b").matcher(body);
            if (matcher.find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Same expression both sides", "identical operand"));
            }
            return issues;
        }
    }

    public static class ThreadRunDirect extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1217"; }
        public String getName() { return "Thread.run() should not be called directly"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("\\w+\\.run\\s*\\(").matcher(body).find() && !body.contains(".start()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Thread.run() called directly", "thread.run()"));
            }
            return issues;
        }
    }

    public static class WaitNotifyNoSync extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3067"; }
        public String getName() { return "wait/notify must be in synchronized context"; }
        public String getCategory() { return "BUG"; }
        @SuppressWarnings("unchecked")
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            List<String> mods = (List<String>) m.getOrDefault("modifiers", Collections.emptyList());
            if ((body.contains(".wait(") || body.contains(".notify(") || body.contains(".notifyAll(") || Pattern.compile("\\bwait\\s*\\(").matcher(body).find()) && !mods.contains("synchronized")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, "wait/notify non-synchronized", "no synchronized"));
            }
            return issues;
        }
    }

    public static class MutableMembersReturned extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2384"; }
        public String getName() { return "Mutable members should not be returned directly"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String retType = (String) m.getOrDefault("return_type_path", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            boolean returnsMutable = retType.contains("List") || retType.contains("Map") || retType.contains("Set") || retType.contains("Collection") || retType.contains("[]");
            if (returnsMutable && !body.contains("Collections.unmodifiable") && !body.contains(".clone()") && !body.contains("new ArrayList") && !body.contains("new HashMap") && !body.contains("toArray")) {
                Matcher matcher = Pattern.compile("return\\s+(\\w+)\\s*;").matcher(body);
                while (matcher.find()) {
                    String var = matcher.group(1);
                    if (!var.equals("null") && !var.equals("this")) {
                        issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Mutable " + retType + " returned", "return " + var));
                        break;
                    }
                }
            }
            return issues;
        }
    }

    public static class FinalizerUsed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1111"; }
        public String getName() { return "finalize() should not be used"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ("finalize".equals(name.trim())) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "finalize() is deprecated", "finalize"));
            }
            return issues;
        }
    }

    public static class MissingSerialVersionUID implements QualityRule {
        public String getRuleKey() { return "RSPEC-2057"; }
        public String getName() { return "Serializable class must have serialVersionUID"; }
        public String getCategory() { return "BUG"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            String kind = (String) classAsset.getOrDefault("kind", "");
            Map<String, List<String>> hierarchy = (Map<String, List<String>>) classAsset.get("hierarchy");
            boolean isSerializable = false;
            if (hierarchy != null) {
                isSerializable = hierarchy.getOrDefault("implements", Collections.emptyList()).stream().anyMatch(s -> s.contains("Serializable"));
            }
            if ("CLASS".equals(kind) && isSerializable) {
                List<Map<String, Object>> fields = (List<Map<String, Object>>) classAsset.getOrDefault("fields_matrix", Collections.emptyList());
                boolean hasSUID = fields.stream().anyMatch(f -> "serialVersionUID".equals(f.get("name")));
                if (!hasSUID) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), (String) classAsset.get("source_file"), (String) classAsset.get("address"), "", 0, "Missing serialVersionUID", "Serializable"));
                }
            }
            return issues;
        }
    }

    public static class NullDereference extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2259"; }
        public String getName() { return "Null pointers should not be dereferenced"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            Matcher setNull = Pattern.compile("(\\w+)\\s*=\\s*null\\s*;").matcher(body);
            while (setNull.find()) {
                String var = setNull.group(1);
                if (Pattern.compile("\\b" + var + "\\s*\\.\\w+\\s*\\(").matcher(body).find() && !Pattern.compile("if\\s*\\(\\s*" + var + "\\s*!=?\\s*null\\s*\\)").matcher(body).find()) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, var + "=null then dereferenced", var + "=null"));
                    break;
                }
            }
            return issues;
        }
    }

    public static class DeadStore extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1854"; }
        public String getName() { return "Dead stores should be removed"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            Matcher assignMatcher = Pattern.compile("(?:final\\s+)?(?:\\w+(?:<[^>]+>)?\\s+)(\\w+)\\s*=").matcher(body);
            while (assignMatcher.find()) {
                String var = assignMatcher.group(1);
                if (var.equals("null") || var.equals("this") || var.equals("true") || var.equals("false") || var.length() < 2) continue;
                String after = body.substring(assignMatcher.end());
                if (!Pattern.compile("\\b" + var + "\\b").matcher(after).find()) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Unused variable: " + var, var));
                }
            }
            return issues;
        }
    }

    public static class AssertSideEffect extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3047"; }
        public String getName() { return "Assertions should not contain side effects"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("assert ")) {
                if (Pattern.compile("assert\\s+.*\\b=\\b").matcher(body).find() || Pattern.compile("assert\\s+.*\\+\\+|assert\\s+.*--").matcher(body).find()) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Assertion has side effect", "assert with ="));
                }
            }
            return issues;
        }
    }

    public static class LoopBranchUpdate extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-135"; }
        public String getName() { return "Loop should not branch to update statement"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("for (") || body.contains("for(")) && body.contains("continue;")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "continue in for loop", "continue"));
            }
            return issues;
        }
    }

    public static class PublicStaticMutableField implements QualityRule {
        public String getRuleKey() { return "RSPEC-1444"; }
        public String getName() { return "public static fields should be final and immutable"; }
        public String getCategory() { return "BUG"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            List<Map<String, Object>> fields = (List<Map<String, Object>>) classAsset.getOrDefault("fields_matrix", Collections.emptyList());
            String cn = (String) classAsset.getOrDefault("address", "");
            String fp = (String) classAsset.getOrDefault("source_file", "");
            for (Map<String, Object> f : fields) {
                @SuppressWarnings("unchecked")
                List<String> mods = (List<String>) f.getOrDefault("modifiers", Collections.emptyList());
                String typePath = (String) f.getOrDefault("type_path", "");
                boolean isPublic = mods.contains("public");
                boolean isStatic = mods.contains("static");
                boolean isFinal = mods.contains("final");
                boolean isMutable = !typePath.contains("String") && !typePath.contains("int") && !typePath.contains("long") && !typePath.contains("boolean");
                if (isPublic && isStatic && !isFinal && isMutable) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, (String) f.get("name"), 0, "public static mutable: " + f.get("name"), "public static " + typePath));
                }
            }
            return issues;
        }
    }

    public static class DeprecatedUsage extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1874"; }
        public String getName() { return "Deprecated code should not be used"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            String[] deprecatedPatterns = {"new Thread().stop\\(", "new Date\\s*\\([^)]\\)", "new Integer\\s*\\(", "new Double\\s*\\(", "new Float\\s*\\(", "new Long\\s*\\(", "new Boolean\\s*\\(", "new Short\\s*\\(", "new Byte\\s*\\("};
            for (String pat : deprecatedPatterns) {
                if (Pattern.compile(pat).matcher(body).find()) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Deprecated API used", pat));
                    break;
                }
            }
            return issues;
        }
    }

    public static class EqualsOnArrays extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2677"; }
        public String getName() { return "Arrays should not be compared using equals"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("].equals(") || (body.contains("==") && body.contains("new") && body.contains("["))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Array compared with equals", "array.equals()"));
            }
            return issues;
        }
    }

    public static class BigDecimalDouble extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2111"; }
        public String getName() { return "BigDecimal(double) should not be used"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("new\\s+BigDecimal\\s*\\(\\s*\\d+\\.\\d+").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "BigDecimal(double) loses precision", "BigDecimal(double)"));
            }
            return issues;
        }
    }

    public static class ToStringReturnsNull extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2225"; }
        public String getName() { return "toString() should not return null"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ("toString".equals(name) && Pattern.compile("return\\s+null\\s*;").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "toString() returns null", "return null"));
            }
            return issues;
        }
    }

    public static class ClassLoaderMisuse extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3078"; }
        public String getName() { return "Use Thread.currentThread().getContextClassLoader()"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".getClassLoader()") && !body.contains("Thread.currentThread().getContextClassLoader()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Use context classloader", "getClassLoader()"));
            }
            return issues;
        }
    }

    public static class ExceptionRethrown extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1696"; }
        public String getName() { return "Exception caught and rethrown without handling"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{\\s*throw\\s+\\w+\\s*;\\s*\\}").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Exception caught and rethrown", "catch { throw }"));
            }
            return issues;
        }
    }

    public static class UncheckedCatch extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4970"; }
        public String getName() { return "Unchecked exceptions should not be caught"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("catch\\s*\\(\\s*(?:RuntimeException|NullPointerException|IndexOutOfBoundsException|IllegalArgumentException)\\s+").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Catching unchecked exception", "catch RuntimeException"));
            }
            return issues;
        }
    }

    public static class UnclosedResource extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2095"; }
        public String getName() { return "Resources should be closed"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("new FileInputStream(") || body.contains("new FileOutputStream(") || body.contains("new BufferedReader(") || body.contains("new Socket(")) && !body.contains("try (") && !body.contains("finally") && !body.contains(".close()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, "Resource not closed", "resource leak"));
            }
            return issues;
        }
    }

    public static class InterruptedExceptionSwallowed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2142"; }
        public String getName() { return "InterruptedException should not be ignored"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("InterruptedException") && !body.contains("Thread.currentThread().interrupt()") && !body.contains("throw") && !body.contains("log.") && !body.contains("LOG.")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "InterruptedException swallowed", "InterruptedException"));
            }
            return issues;
        }
    }

    public static class LongToIntCast extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2184"; }
        public String getName() { return "Long should not be cast to int"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("\\(\\s*int\\s*\\)\\s*\\w*[Ll]ong").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Long cast to int", "(int) long"));
            }
            return issues;
        }
    }

    public static class RedundantCast extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2276"; }
        public String getName() { return "Primitive wrappers should not be cast to primitives"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("\\(\\s*int\\s*\\)\\s*Integer|\\(\\s*long\\s*\\)\\s*Long|\\(\\s*double\\s*\\)\\s*Double").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Redundant cast", "(int) Integer"));
            }
            return issues;
        }
    }

    public static class EqualsWithoutHashCode implements QualityRule {
        public String getRuleKey() { return "RSPEC-1206"; }
        public String getName() { return "equals() and hashCode() should be overridden in pairs"; }
        public String getCategory() { return "CODE_SMELL"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            List<Map<String, Object>> methods = (List<Map<String, Object>>) classAsset.getOrDefault("methods_full", Collections.emptyList());
            boolean hasEquals = methods.stream().anyMatch(m -> "equals".equals(m.get("name")));
            boolean hasHashCode = methods.stream().anyMatch(m -> "hashCode".equals(m.get("name")));
            if (hasEquals && !hasHashCode) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), (String) classAsset.get("source_file"), (String) classAsset.get("address"), "equals", 0, "equals without hashCode", "equals without hashCode"));
            }
            return issues;
        }
    }

    public static class StringConcatInLoop extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1132"; }
        public String getName() { return "String concatenation in loops should be avoided"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("for (") || body.contains("while (")) && (body.contains("+ \"") || body.contains("\" +")) && !body.contains("StringBuilder") && !body.contains("StringBuffer")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "String concatenation in loop", "+= in loop"));
            }
            return issues;
        }
    }

    public static class EmptyMethodBody extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1186"; }
        public String getName() { return "Empty method bodies should be removed or documented"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            List<String> mods = (List<String>) m.getOrDefault("modifiers", Collections.emptyList());
            boolean isEmpty = body.isEmpty() || body.trim().equals("{}");
            if (isEmpty && mods.contains("public") && !mods.contains("abstract") && !mods.contains("native")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Empty method body", "empty method"));
            }
            return issues;
        }
    }

    public static class BooleanLiteralInCondition extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2166"; }
        public String getName() { return "Boolean literals should not be redundant"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("if\\s*\\(\\s*\\w+\\s*==\\s*(true|false)\\s*\\)").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Redundant boolean literal", "== true/false"));
            }
            return issues;
        }
    }

    public static class StringEqualsCaseSensitive extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2253"; }
        public String getName() { return "Use equalsIgnoreCase for case-insensitive comparison"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".toLowerCase().equals(") || body.contains(".toUpperCase().equals(")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Use equalsIgnoreCase()", "toLowerCase().equals()"));
            }
            return issues;
        }
    }

    public static class SensitiveToString extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2254"; }
        public String getName() { return "toString() should not return sensitive information"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ("toString".equals(name) && (body.contains("password") || body.contains("secret") || body.contains("token") || body.contains("key"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "toString may leak sensitive data", "sensitive toString"));
            }
            return issues;
        }
    }

    public static class BooleanMethodName extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2675"; }
        public String getName() { return "Boolean methods should be named is/has/can/should"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            String retType = (String) m.getOrDefault("return_type_path", "");
            // Exclude Object overrides and standard methods
            if ("equals".equals(name) || "hashCode".equals(name) || "canEqual".equals(name) ||
                "matches".equals(name) || "iterator".equals(name) || "test".equals(name) ||
                "accept".equals(name) || "getAsBoolean".equals(name)) {
                return issues;
            }
            if ("boolean".equals(retType) && !name.matches("^(is|has|can|should|was|will|are|contains|matches|exists|isValid|isEmpty|isEnabled|isPresent|supports|allows)\\b")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Boolean method naming", name));
            }
            return issues;
        }
    }

    public static class MethodTooLongName extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2676"; }
        public String getName() { return "Method names should not be too long"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name.length() > 40) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Method name too long: " + name.length(), "name=" + name.length()));
            }
            return issues;
        }
    }

    public static class ThreadSleepInCode extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2925"; }
        public String getName() { return "Thread.sleep should not be used in production code"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("Thread.sleep(") && !name.toLowerCase().contains("test")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Thread.sleep in production code", "Thread.sleep"));
            }
            return issues;
        }
    }

    public static class MagicNumber extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3415"; }
        public String getName() { return "Magic numbers should not be used"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            Matcher matcher = Pattern.compile("[^a-zA-Z_](\\d{3,})[^a-zA-Z_]").matcher(body);
            while (matcher.find()) {
                String num = matcher.group(1);
                if (!num.equals("100") && !num.equals("200") && !num.equals("404") && !num.equals("500")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Magic number: " + num, num));
                    break;
                }
            }
            return issues;
        }
    }

    public static class OptionalParameter extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3553"; }
        public String getName() { return "Optional should not be used in method parameters"; }
        public String getCategory() { return "CODE_SMELL"; }
        @SuppressWarnings("unchecked")
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            List<Map<String, String>> params = (List<Map<String, String>>) m.getOrDefault("parameters_inventory", Collections.emptyList());
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            for (Map<String, String> p : params) {
                if (p.getOrDefault("type_path", "").contains("Optional")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Optional as parameter", "Optional param"));
                    break;
                }
            }
            return issues;
        }
    }

    public static class OptionalGetWithoutCheck extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3658"; }
        public String getName() { return "Optional.get() should not be called without isPresent check"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".get()") && !body.contains(".isPresent()") && !body.contains(".orElse") && !body.contains(".orElseGet") && !body.contains(".ifPresent")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Optional.get() without check", ".get()"));
            }
            return issues;
        }
    }

    public static class StreamNotConsumed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4248"; }
        public String getName() { return "Stream should be consumed or collected"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains(".stream()") || body.contains(".parallelStream()")) && !body.contains(".collect(") && !body.contains(".forEach(") && !body.contains(".count(") && !body.contains(".findFirst(") && !body.contains(".anyMatch(")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Stream not consumed", ".stream()"));
            }
            return issues;
        }
    }

    public static class OptionalField implements QualityRule {
        public String getRuleKey() { return "RSPEC-4347"; }
        public String getName() { return "Optional should not be used for fields"; }
        public String getCategory() { return "CODE_SMELL"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            List<Map<String, Object>> fields = (List<Map<String, Object>>) classAsset.getOrDefault("fields_matrix", Collections.emptyList());
            for (Map<String, Object> f : fields) {
                String typePath = (String) f.getOrDefault("type_path", "");
                if (typePath.contains("Optional")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), (String) classAsset.get("source_file"), (String) classAsset.get("address"), (String) f.get("name"), 0, "Optional field", "Optional field"));
                }
            }
            return issues;
        }
    }

    public static class SSLServerSocket extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4423"; }
        public String getName() { return "SSL Server socket should use strong protocols"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("SSLServerSocket") || body.contains("SSLContext.getInstance(\"SSL\")") || body.contains("\"TLSv1\"") || body.contains("\"SSLv3\"")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Weak SSL/TLS", "SSL/TLS"));
            }
            return issues;
        }
    }

    public static class WeakRSAKey extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4426"; }
        public String getName() { return "RSA keys should be at least 2048 bits"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("KeyPairGenerator") && (body.contains("1024") || body.contains("512"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, "Weak RSA key", "1024 bit RSA"));
            }
            return issues;
        }
    }

    public static class DOMParserXXE extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4434"; }
        public String getName() { return "XML parsers should not be vulnerable to XXE"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("DocumentBuilder") && !body.contains("setFeature") && !body.contains("http://javax.xml.XMLConstants")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "XML parser XXE vulnerable", "DocumentBuilder"));
            }
            return issues;
        }
    }

    public static class AllocationInLoop extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4517"; }
        public String getName() { return "Allocation of expensive objects in loops should be avoided"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("for (") || body.contains("while (")) && (body.contains("new SimpleDateFormat(") || body.contains("new Pattern[") || body.contains("new BigDecimal(") || body.contains("new DecimalFormat("))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Expensive object in loop", "new in loop"));
            }
            return issues;
        }
    }

    public static class CatchingError extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4719"; }
        public String getName() { return "Error should not be caught"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("catch (Error ") || body.contains("catch (Throwable ")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Error/Throwable caught", "catch (Error"));
            }
            return issues;
        }
    }

    public static class OptionalChaining extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4738"; }
        public String getName() { return "Use Optional chaining instead of isPresent + get"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".isPresent()") && body.contains(".get()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Use Optional chaining", "isPresent + get"));
            }
            return issues;
        }
    }

    public static class TLSProtocol extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5332"; }
        public String getName() { return "TLS should use secure protocol versions"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("TLSv1\"") || body.contains("TLSv1.1\"") || body.contains("SSLv3\"")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Insecure TLS protocol", "TLSv1/SSLv3"));
            }
            return issues;
        }
    }

    public static class AutoboxingPerformance extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5411"; }
        public String getName() { return "Autoboxing/unboxing in loops is expensive"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("for (") || body.contains("while (")) && (body.contains("Map<") && (body.contains("Long") || body.contains("Integer")))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Autoboxing in loop", "boxing in loop"));
            }
            return issues;
        }
    }

    public static class WeakHashFunction extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5547"; }
        public String getName() { return "MD5 and SHA1 should not be used for security"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("MessageDigest.getInstance(\"MD5\")") || body.contains("MessageDigest.getInstance(\"SHA-1\")") || body.contains("MessageDigest.getInstance(\"SHA1\")")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Weak hash", "MD5/SHA1"));
            }
            return issues;
        }
    }

    public static class JWTWithoutExpiry extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5659"; }
        public String getName() { return "JWT should have an expiration time"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("Jwts.builder()") && !body.contains("setExpiration") && !body.contains("exp")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "JWT without expiry", "JWT no expiry"));
            }
            return issues;
        }
    }

    public static class RegexComplexity extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5831"; }
        public String getName() { return "Regular expressions should not be too complex"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            Matcher regexMatcher = Pattern.compile("Pattern\\.compile\\s*\\(\\s*\"([^\"]+)\"").matcher(body);
            while (regexMatcher.find()) {
                String regex = regexMatcher.group(1);
                int nestDepth = 0, maxDepth = 0;
                for (char c : regex.toCharArray()) {
                    if (c == '(') { nestDepth++; maxDepth = Math.max(maxDepth, nestDepth); }
                    else if (c == ')') nestDepth--;
                }
                if (maxDepth > 3 || regex.length() > 50) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Complex regex: depth=" + maxDepth, regex.substring(0, Math.min(30, regex.length()))));
                    break;
                }
            }
            return issues;
        }
    }

    public static class RegexLookaround extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5860"; }
        public String getName() { return "Regex lookaround should not be used"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("(?=") || body.contains("(?!") || body.contains("(?<=") || body.contains("(?<!")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Regex lookaround", "lookaround"));
            }
            return issues;
        }
    }

    public static class RegexDoS extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6202"; }
        public String getName() { return "Regex with catastrophic backtracking should not be used"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("(.*)*") || body.contains("(.*).*")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Regex catastrophic backtracking", "(.*)*"));
            }
            return issues;
        }
    }

    public static class InsecureRandom extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6303"; }
        public String getName() { return "java.util.Random should not be used for security"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("new Random(") || body.contains("Random().nextInt")) && (body.contains("password") || body.contains("token") || body.contains("secret") || body.contains("key") || body.contains("session"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, "Insecure random for security", "Random()"));
            }
            return issues;
        }
    }

    public static class NullCheckAfterDeref extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6347"; }
        public String getName() { return "Null check should be done before dereferencing"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            Matcher derefMatcher = Pattern.compile("(\\w+)\\.\\w+\\s*\\(").matcher(body);
            while (derefMatcher.find()) {
                String var = derefMatcher.group(1);
                String afterDeref = body.substring(derefMatcher.end());
                if (Pattern.compile("if\\s*\\(\\s*" + var + "\\s*==\\s*null\\s*\\)").matcher(afterDeref).find()) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, var + " dereferenced before null check", "deref before null"));
                    break;
                }
            }
            return issues;
        }
    }

    public static class LogInjection extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6437"; }
        public String getName() { return "Log injection should be prevented"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("log.info(") || body.contains("log.warn(") || body.contains("log.error(")) && body.contains("+ ") && !body.contains("replace") && !body.contains("escape")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Log injection risk", "log + param"));
            }
            return issues;
        }
    }

    public static class HashWithoutSalt extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6535"; }
        public String getName() { return "Hashing without salt is insecure"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("MessageDigest") || body.contains("digest(")) && (body.contains("password") || body.contains("Password")) && !body.contains("salt")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Password hashed without salt", "hash password no salt"));
            }
            return issues;
        }
    }

    public static class InsecureCookie extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6544"; }
        public String getName() { return "Cookies should have Secure flag"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new Cookie(") && !body.contains("setSecure(true)") && !body.contains("secure")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Cookie without Secure flag", "Cookie"));
            }
            return issues;
        }
    }

    public static class HttpOnlyCookie extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6546"; }
        public String getName() { return "Cookies should have HttpOnly flag"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new Cookie(") && !body.contains("setHttpOnly(true)")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Cookie without HttpOnly", "Cookie"));
            }
            return issues;
        }
    }

    public static class ContentTypeSniffing extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6549"; }
        public String getName() { return "Content-Type sniffing should be prevented"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("X-Content-Type-Options") && !body.contains("nosniff")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Content-Type sniffing not prevented", "no nosniff"));
            }
            return issues;
        }
    }

    public static class BigDecimalPrecisionLoss extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6557"; }
        public String getName() { return "BigDecimal operations should not lose precision"; }
        public String getCategory() { return "BUG"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("BigDecimal") && (body.contains("divide(") || body.contains(".doubleValue()") || body.contains(".floatValue()"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "BigDecimal precision loss", "BigDecimal divide/double"));
            }
            return issues;
        }
    }

    public static class FilePermissionTooPermissive extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6568"; }
        public String getName() { return "File permissions should not be overly permissive"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("setReadable(true, false)") || body.contains("setWritable(true, false)") || body.contains("setReadable(true)") || body.contains("setWritable(true)")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "File permission too permissive", "setReadable/setWritable"));
            }
            return issues;
        }
    }

    // =====================================================================
    // SECURITY RULES (continued)
    // =====================================================================

    public static class HardcodedPassword extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2068"; }
        public String getName() { return "Hard-coded credentials are security-sensitive"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            String[] patterns = {"password\\s*=\\s*\"[^\"]+\"", "passwd\\s*=\\s*\"[^\"]+\"", "secret\\s*=\\s*\"[^\"]+\"", "apiKey\\s*=\\s*\"[^\"]+\"", "token\\s*=\\s*\"[^\"]+\"", "privateKey\\s*=\\s*\"[^\"]+\"", "accessKey\\s*=\\s*\"[^\"]+\"", "secretKey\\s*=\\s*\"[^\"]+\"", "pwd\\s*=\\s*\"[^\"]+\""};
            for (String pat : patterns) {
                if (Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(body).find()) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, "Hardcoded credential", pat));
                    break;
                }
            }
            return issues;
        }
    }

    public static class SQLInjection extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2077"; }
        public String getName() { return "SQL queries should not be constructed dynamically"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            boolean hasSQL = body.contains("SELECT") || body.contains("INSERT") || body.contains("UPDATE") || body.contains("DELETE");
            boolean hasConcat = body.contains("+ \"") || body.contains("\" +") || body.contains("String.format") || body.contains("MessageFormat.format");
            if (hasSQL && hasConcat) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, "SQL injection risk", "SQL + concat"));
            }
            return issues;
        }
    }

    public static class HardcodedIP extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1313"; }
        public String getName() { return "Hard-coded IP addresses should not be used"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b").matcher(body).find() && !body.contains("127.0.0.1") && !body.contains("0.0.0.0")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Hard-coded IP", "IP literal"));
            }
            return issues;
        }
    }

    public static class HTTPNotHTTPS extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5145"; }
        public String getName() { return "HTTP requests should use HTTPS"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("http://") && !body.contains("https://") && !body.contains("localhost") && !body.contains("127.0.0.1")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "HTTP instead of HTTPS", "http://"));
            }
            return issues;
        }
    }

    public static class InsecureRandomGenerator extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2245"; }
        public String getName() { return "java.util.Random should not be used cryptographically"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new Random()") || body.contains("Math.random()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Insecure random", "Random()"));
            }
            return issues;
        }
    }

    public static class Deserialization extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2186"; }
        public String getName() { return "ObjectInputStream should not be used to deserialize untrusted data"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("ObjectInputStream") || body.contains("readObject()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, "Unsafe deserialization", "ObjectInputStream"));
            }
            return issues;
        }
    }

    public static class CommandInjection extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5131"; }
        public String getName() { return "Runtime.exec should not be used with user input"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("Runtime.getRuntime().exec(") || body.contains("ProcessBuilder(")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, "Command injection risk", "Runtime.exec"));
            }
            return issues;
        }
    }

    public static class WeakMAC extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5167"; }
        public String getName() { return "MAC should use strong algorithms"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("HmacMD5") || body.contains("HmacSHA1")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Weak MAC", "HmacMD5/SHA1"));
            }
            return issues;
        }
    }

    public static class ReflectionOnSensitive extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5247"; }
        public String getName() { return "Reflection should not be used to increase accessibility"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("setAccessible(true)")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "setAccessible(true) bypasses access control", "setAccessible"));
            }
            return issues;
        }
    }

    public static class HardcodedSecretKey extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5324"; }
        public String getName() { return "Secret keys should not be hard-coded"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("SecretKeySpec") || body.contains("KeyGenerator")) && body.contains("\"")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, "Cryptographic key hard-coded", "SecretKeySpec"));
            }
            return issues;
        }
    }

    public static class SessionFixation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5764"; }
        public String getName() { return "Session ID should be changed after login"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("HttpServletRequest") && (body.contains("login") || body.contains("authenticate") || body.contains("Login")) && !body.contains("changeSessionId") && !body.contains("invalidate()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Session fixation risk", "session fixation"));
            }
            return issues;
        }
    }

    public static class InsecureTempFile extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-5883"; }
        public String getName() { return "Temporary files should not be created insecurely"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("createTempFile(") && !body.contains("deleteOnExit")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Temp file not deleted on exit", "createTempFile"));
            }
            return issues;
        }
    }

    public static class LDAPInjection extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6280"; }
        public String getName() { return "LDAP queries should not be constructed dynamically"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("ldap://") && (body.contains("+ \"") || body.contains("\" +") || body.contains("String.format"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, "LDAP injection risk", "LDAP + concat"));
            }
            return issues;
        }
    }

    public static class PathTraversal extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6422"; }
        public String getName() { return "Path traversal should be prevented"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("new File(") || body.contains("Paths.get(")) && !body.contains("getCanonicalPath()") && !body.contains("normalize()") && (body.contains("request.getParameter") || body.contains("input"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Path traversal risk", "File + user input"));
            }
            return issues;
        }
    }

    public static class OpenRedirect extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6432"; }
        public String getName() { return "Open redirect should be prevented"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("sendRedirect(") && (body.contains("getParameter") || body.contains("+ \"")) && !body.contains("allowed") && !body.contains("whitelist")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Open redirect risk", "sendRedirect + user input"));
            }
            return issues;
        }
    }

    public static class XXEInTransformerFactory extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6477"; }
        public String getName() { return "TransformerFactory should be secured against XXE"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("TransformerFactory") && !body.contains("ACCESS_EXTERNAL_DTD") && !body.contains("setFeature")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "TransformerFactory XXE vulnerable", "TransformerFactory"));
            }
            return issues;
        }
    }

    public static class XPathInjection extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6478"; }
        public String getName() { return "XPath queries should not be constructed dynamically"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("XPathFactory") || body.contains("compile(")) && (body.contains("+ \"") || body.contains("getParameter")) && !body.contains("sanitize")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "XPath injection risk", "XPath + concat"));
            }
            return issues;
        }
    }

    public static class CORSMisconfiguration extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-6509"; }
        public String getName() { return "CORS should not allow all origins"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("Access-Control-Allow-Origin") && body.contains("*")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "CORS allows all origins", "Access-Control-Allow-Origin: *"));
            }
            return issues;
        }
    }

    // =====================================================================
    // CODE_SMELL RULES (remaining)
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
            int lc = body.isEmpty() ? 0 : body.split("\n").length;
            if (lc > 30) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Method is " + lc + " lines", "lines=" + lc));
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
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Method has " + params.size() + " params", "params=" + params.size()));
            }
            return issues;
        }
    }

    public static class TooManyReturns extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1142"; }
        public String getName() { return "Jump statements should not occur too frequently"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            int rc = body.split("\\breturn\\b").length - 1;
            if (rc > 5) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Method has " + rc + " returns", "returns=" + rc));
            }
            return issues;
        }
    }

    public static class CyclomaticComplexity extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3776"; }
        public String getName() { return "Cognitive Complexity should not be too high"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            int cc = 1;
            String[] keywords = {"if", "else if", "for", "while", "case", "&&", "||", "catch", "switch"};
            for (String kw : keywords) {
                Matcher matcher = Pattern.compile("\\b" + Pattern.quote(kw.trim()) + "\\b").matcher(body);
                while (matcher.find()) cc++;
            }
            if (cc > 15) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Cyclomatic complexity is " + cc, "cc=" + cc));
            }
            return issues;
        }
    }

    public static class PrintStackTrace extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1148"; }
        public String getName() { return "Throwable.printStackTrace() should not be called"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains(".printStackTrace()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Use logging instead", "printStackTrace"));
            }
            return issues;
        }
    }

    public static class GodClass implements QualityRule {
        public String getRuleKey() { return "RSPEC-1444"; }
        public String getName() { return "Classes should not have too many methods and fields"; }
        public String getCategory() { return "CODE_SMELL"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            List<Map<String, Object>> methods = (List<Map<String, Object>>) classAsset.getOrDefault("methods_full", Collections.emptyList());
            List<Map<String, Object>> fields = (List<Map<String, Object>>) classAsset.getOrDefault("fields_matrix", Collections.emptyList());
            String cn = (String) classAsset.getOrDefault("address", "");
            String fp = (String) classAsset.getOrDefault("source_file", "");
            if (methods.size() > 30) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, "", 0, "Class has " + methods.size() + " methods", "methods=" + methods.size()));
            }
            if (fields.size() > 20) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, "", 0, "Class has " + fields.size() + " fields", "fields=" + fields.size()));
            }
            return issues;
        }
    }

    public static class MissingJavadoc extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-159"; }
        public String getName() { return "Methods should have documentation comments"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String desc = (String) m.getOrDefault("description", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            List<String> mods = (List<String>) m.getOrDefault("modifiers", Collections.emptyList());
            if ((desc == null || desc.isEmpty() || desc.equals("暂无描述")) && mods.contains("public") && !name.equals("<init>")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "No Javadoc", "no description"));
            }
            return issues;
        }
    }

    public static class WildcardImport implements QualityRule {
        public String getRuleKey() { return "RSPEC-2208"; }
        public String getName() { return "Wildcard imports should not be used"; }
        public String getCategory() { return "CODE_SMELL"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            List<String> imports = (List<String>) classAsset.getOrDefault("import_dependencies", Collections.emptyList());
            String cn = (String) classAsset.getOrDefault("address", "");
            String fp = (String) classAsset.getOrDefault("source_file", "");
            for (String imp : imports) {
                if (imp.endsWith(".*")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, "", 0, "Wildcard import: " + imp, imp));
                }
            }
            return issues;
        }
    }

    public static class SystemOutPrintln extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-106"; }
        public String getName() { return "System.out.println should not be used for logging"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("System.out.print") || body.contains("System.err.print")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Use logging instead", "System.out"));
            }
            return issues;
        }
    }

    public static class TooManyConstructors implements QualityRule {
        public String getRuleKey() { return "RSPEC-3400"; }
        public String getName() { return "Classes should not have too many constructors"; }
        public String getCategory() { return "CODE_SMELL"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            List<Map<String, Object>> ctors = (List<Map<String, Object>>) classAsset.getOrDefault("constructor_matrix", Collections.emptyList());
            if (ctors.size() > 5) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), (String) classAsset.get("source_file"), (String) classAsset.get("address"), "", 0, "Too many constructors: " + ctors.size(), "ctors=" + ctors.size()));
            }
            return issues;
        }
    }

    public static class EmptyStatement extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-012"; }
        public String getName() { return "Empty statements should be removed"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.matches("(?s).*\\;\\s*\\;.*")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Empty statement", ";;"));
            }
            return issues;
        }
    }

    public static class UnusedLocalVariable extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1481"; }
        public String getName() { return "Unused local variables should be removed"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            Matcher varMatcher = Pattern.compile("(?:final\\s+)?(?:\\w+(?:<[^>]+>)?\\s+)(\\w+)\\s*=").matcher(body);
            while (varMatcher.find()) {
                String var = varMatcher.group(1);
                if (var.length() < 2 || var.equals("null") || var.equals("this")) continue;
                String after = body.substring(varMatcher.end());
                if (!Pattern.compile("\\b" + Pattern.quote(var) + "\\b").matcher(after).find()) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Unused variable: " + var, var));
                }
            }
            return issues;
        }
    }

    public static class TooManyStringLiterals extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1192"; }
        public String getName() { return "String literals should not be duplicated"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            Map<String, Integer> literalCounts = new HashMap<>();
            Matcher matcher = Pattern.compile("\"([^\"]{4,})\"").matcher(body);
            while (matcher.find()) {
                String lit = matcher.group(1);
                literalCounts.merge(lit, 1, Integer::sum);
            }
            for (Map.Entry<String, Integer> e : literalCounts.entrySet()) {
                if (e.getValue() >= 3) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "String repeated " + e.getValue() + " times", "duplicated string"));
                    break;
                }
            }
            return issues;
        }
    }

    public static class ExceptionIgnored extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1166"; }
        public String getName() { return "Exception handlers should preserve the original exception"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{\\s*throw\\s+new\\s+\\w+Exception\\s*\\([^)]*\\)\\s*;\\s*\\}").matcher(body).find() && !body.contains("e)") && !body.contains("cause") && !body.contains("initCause")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line, "Exception cause not preserved", "exception wrapping"));
            }
            return issues;
        }
    }

    public static class CSRFDisabled extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4502"; }
        public String getName() { return "CSRF should not be disabled"; }
        public String getCategory() { return "SECURITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("csrf().disable()") || body.contains("CsrfConfigurer")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line, "CSRF disabled", "csrf().disable()"));
            }
            return issues;
        }
    }

    public static class JDBCInjection extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4433"; }
        public String getName() { return "JDBC connections should not be retrieved from JNDI"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("InitialContext") && body.contains("lookup(") && body.contains("jdbc")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "JDBC from JNDI", "JNDI jdbc"));
            }
            return issues;
        }
    }

    public static class VariableDeclaredFarFromUsage extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-4488"; }
        public String getName() { return "Variables should be declared close to their usage"; }
        public String getCategory() { return "CODE_SMELL"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            String[] bodyLines = body.split("\n");
            Matcher varMatcher = Pattern.compile("(?:final\\s+)?(?:\\w+(?:<[^>]+>)?\\s+)(\\w+)\\s*=").matcher(body);
            while (varMatcher.find()) {
                String var = varMatcher.group(1);
                if (var.length() < 2) continue;
                int declLine = body.substring(0, varMatcher.start()).split("\n").length;
                String after = body.substring(varMatcher.end());
                Matcher useMatcher = Pattern.compile("\\b" + Pattern.quote(var) + "\\b").matcher(after);
                if (useMatcher.find()) {
                    int useLine = after.substring(0, useMatcher.start()).split("\n").length;
                    if (Math.abs(declLine - useLine) > 10) {
                        issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line, "Variable declared far from usage", var));
                        break;
                    }
                }
            }
            return issues;
        }
    }

    public static class SerializableField implements QualityRule {
        public String getRuleKey() { return "RSPEC-1948"; }
        public String getName() { return "Fields in Serializable classes should be serializable or transient"; }
        public String getCategory() { return "BUG"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            Map<String, List<String>> h = (Map<String, List<String>>) classAsset.get("hierarchy");
            boolean isSerializable = false;
            if (h != null) {
                isSerializable = h.getOrDefault("implements", Collections.emptyList()).stream().anyMatch(s -> s.contains("Serializable"));
            }
            if ("CLASS".equals(classAsset.get("kind")) && isSerializable) {
                List<Map<String, Object>> fields = (List<Map<String, Object>>) classAsset.getOrDefault("fields_matrix", Collections.emptyList());
                Set<String> knownSerializable = new HashSet<>(Arrays.asList("String", "Integer", "Long", "Double", "Float", "Boolean", "Short", "Byte", "Character", "BigDecimal", "BigInteger", "Date", "UUID", "URI", "URL", "Optional"));
                for (Map<String, Object> f : fields) {
                    @SuppressWarnings("unchecked")
                    List<String> mods = (List<String>) f.getOrDefault("modifiers", Collections.emptyList());
                    String type = (String) f.getOrDefault("type_path", "");
                    if (mods.contains("transient") || mods.contains("static")) continue;
                    if (!knownSerializable.contains(type) && !type.contains("List") && !type.contains("Map") && !type.contains("Set")) {
                        issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), (String) classAsset.get("source_file"), (String) classAsset.get("address"), (String) f.get("name"), 0, "Field may not be serializable: " + type, type));
                    }
                }
            }
            return issues;
        }
    }
}
