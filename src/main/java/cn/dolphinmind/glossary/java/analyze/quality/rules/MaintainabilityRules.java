package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Maintainability Rules - P3 Priority
 *
 * Detects code maintainability anti-patterns:
 * - Duplicate code
 * - Naming violations
 * - Comment quality issues
 * - Exception handling anti-patterns
 * - Logging anti-patterns
 * - Dead code
 * - Magic numbers
 */
public final class MaintainabilityRules {
    private MaintainabilityRules() {}

    // =====================================================================
    // Duplicate Code Detection
    // =====================================================================

    /**
     * RSPEC-2200: Duplicate code blocks
     * Detects similar code blocks (simplified token-based comparison).
     */
    public static class DuplicateCodeBlock extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2200"; }
        public String getName() { return "Duplicate code blocks should be refactored"; }
        public String getCategory() { return "MAINTAINABILITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Simple duplicate detection: look for repeated patterns
            String[] bodyLines = body.split("\n");
            if (bodyLines.length < 10) return issues;

            Set<String> seenPatterns = new HashSet<>();
            for (int i = 0; i < bodyLines.length - 4; i++) {
                StringBuilder pattern = new StringBuilder();
                for (int j = i; j < i + 5 && j < bodyLines.length; j++) {
                    String normalized = bodyLines[j].trim().replaceAll("\\s+", " ");
                    if (!normalized.isEmpty() && !normalized.startsWith("//")) {
                        pattern.append(normalized).append("\n");
                    }
                }
                if (pattern.length() > 50) {
                    String key = pattern.toString();
                    if (seenPatterns.contains(key)) {
                        issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                            "Duplicate code block (5+ lines)", "duplicate block"));
                        break;
                    }
                    seenPatterns.add(key);
                }
            }
            return issues;
        }
    }

    // =====================================================================
    // Naming Violations
    // =====================================================================

    /**
     * RSPEC-2201: Class name doesn't match file name
     * Detects mismatched class/file names.
     */
    public static class ClassNameMismatch implements QualityRule {
        public String getRuleKey() { return "RSPEC-2201"; }
        public String getName() { return "Class name should match file name"; }
        public String getCategory() { return "MAINTAINABILITY"; }
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            String className = (String) classAsset.getOrDefault("address", "");
            String sourceFile = (String) classAsset.getOrDefault("source_file", "");

            if (sourceFile != null && !sourceFile.isEmpty()) {
                String fileName = sourceFile.substring(sourceFile.lastIndexOf('/') + 1).replace(".java", "");
                String simpleClassName = className.contains(".") ? className.substring(className.lastIndexOf('.') + 1) : className;
                if (!simpleClassName.equals(fileName) && !className.endsWith("$" + fileName)) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), sourceFile, className,
                        "", 0, "Class name '" + simpleClassName + "' doesn't match file '" + fileName + "'", "name mismatch"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-2202: Variable name too short
     * Detects variables with names shorter than 3 characters.
     */
    public static class ShortVariableName extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2202"; }
        public String getName() { return "Variable names should be meaningful (>= 3 chars)"; }
        public String getCategory() { return "MAINTAINABILITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Find variable declarations with short names
            Matcher varMatcher = Pattern.compile("(?:final\\s+)?(?:\\w+(?:<[^>]+>)?\\s+)([a-z]\\w{0,1})\\s*[=;,(]").matcher(body);
            while (varMatcher.find()) {
                String varName = varMatcher.group(1);
                // Skip common short names like 'i', 'j', 'k' in loops
                if (!varName.matches("^[ijk]$") && !varName.equals("id") && !varName.equals("x") && !varName.equals("y")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Short variable name: '" + varName + "'", "short var=" + varName));
                    break;
                }
            }
            return issues;
        }
    }

    // =====================================================================
    // Comment Quality
    // =====================================================================

    /**
     * RSPEC-2203: Misleading comment
     * Detects comments that don't match the code (heuristic).
     */
    public static class MisleadingComment extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2203"; }
        public String getName() { return "Comments should not be misleading"; }
        public String getCategory() { return "MAINTAINABILITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect TODO comments that are older than implementation
            if (body.contains("// TODO") && body.contains("return null")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "TODO comment with null return", "TODO + return null"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-2204: Commented-out code
     * Detects blocks of commented-out code.
     */
    public static class CommentedOutCode extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2204"; }
        public String getName() { return "Commented-out code should be removed"; }
        public String getCategory() { return "MAINTAINABILITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect consecutive lines of commented-out code
            Pattern commentedCodePattern = Pattern.compile("(?m)(^\\s*//.*\\w+.*;\\s*\n){3,}");
            if (commentedCodePattern.matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Commented-out code block detected", "commented code"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Exception Handling Anti-Patterns
    // =====================================================================

    /**
     * RSPEC-2205: Generic exception caught
     * Detects catch blocks that catch Exception or Throwable.
     */
    public static class GenericExceptionCaught extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2205"; }
        public String getName() { return "Generic exceptions should not be caught"; }
        public String getCategory() { return "MAINTAINABILITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("catch (Exception") || body.contains("catch (Throwable") ||
                body.contains("catch (RuntimeException")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Generic exception caught", "catch Exception"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-2206: Exception swallowed without logging
     * Detects catch blocks that don't log or rethrow.
     */
    public static class ExceptionSwallowed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2206"; }
        public String getName() { return "Exceptions should not be swallowed"; }
        public String getCategory() { return "MAINTAINABILITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect catch blocks that do nothing
            Pattern emptyCatch = Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{\\s*\\}");
            if (emptyCatch.matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Exception caught and swallowed", "empty catch{}"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Logging Anti-Patterns
    // =====================================================================

    /**
     * RSPEC-2207: Inefficient logging
     * Detects string concatenation in logging calls.
     */
    public static class InefficientLogging extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2207"; }
        public String getName() { return "Logging should be efficient"; }
        public String getCategory() { return "MAINTAINABILITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect string concatenation in log calls
            Pattern concatLog = Pattern.compile("(log|logger|LOG|LOGGER)\\.(debug|info|warn|error|trace)\\s*\\([^)]*\\+");
            if (concatLog.matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "String concatenation in logging", "log + string concat"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-2208: Logging level mismatch
     * Detects exceptions logged at wrong level.
     */
    public static class LoggingLevelMismatch extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2208"; }
        public String getName() { return "Exceptions should be logged at appropriate level"; }
        public String getCategory() { return "MAINTAINABILITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect exceptions logged at INFO level
            if (body.contains(".info(") && (body.contains("Exception") || body.contains("Error") || body.contains("throw"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Exception logged at INFO level", "info + exception"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Dead Code
    // =====================================================================

    /**
     * RSPEC-2209: Unreachable code
     * Detects code after return/throw/break.
     */
    public static class UnreachableCode extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2209"; }
        public String getName() { return "Unreachable code should be removed"; }
        public String getCategory() { return "MAINTAINABILITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect code after return in same block
            Pattern returnThenCode = Pattern.compile("return\\s+[^;]+;\\s+[^\\s/}]+");
            if (returnThenCode.matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Code after return statement", "unreachable after return"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Magic Numbers
    // =====================================================================

    /**
     * RSPEC-2210: Magic number
     * Detects magic numbers in code (except common ones like 0, 1, -1).
     */
    public static class MagicNumber extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2210"; }
        public String getName() { return "Magic numbers should be named constants"; }
        public String getCategory() { return "MAINTAINABILITY"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect magic numbers > 10 (excluding common ones)
            Pattern magicNumber = Pattern.compile("\\b([2-9]\\d{1,}|[1-9]\\d{2,})\\b");
            Matcher matcher = magicNumber.matcher(body);
            if (matcher.find()) {
                String magicNum = matcher.group(1);
                // Skip common numbers in loops, array indices, etc.
                if (!body.contains("for") || !body.contains("int i")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Magic number: " + magicNum, "magic=" + magicNum));
                }
            }
            return issues;
        }
    }
}
