package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.cfg.CFGBuilder;
import cn.dolphinmind.glossary.java.analyze.cfg.CFGNode;
import cn.dolphinmind.glossary.java.analyze.cfg.ControlFlowGraph;
import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.*;
import java.util.regex.*;

/**
 * Detects exception handling issues using CFG analysis.
 *
 * Reports ONLY:
 * 1. Empty catch blocks (no statements at all)
 * 2. Rethrowing without preserving original cause
 * 3. Catching and ignoring exception without ANY handling
 *
 * Does NOT report:
 * - Catch blocks that log the exception (log.error/warn/debug with e)
 * - Catch blocks that wrap exception with cause (new XxxException(msg, e))
 * - Catch blocks that rethrow with cause (throw new XxxException(e))
 * - Catch blocks that call printStackTrace()
 * - Catch blocks that handle the exception meaningfully (rollback, cleanup, etc.)
 */
public class ExceptionHandlingPath implements QualityRule {

    // Patterns that indicate PROPER exception handling
    private static final Pattern[] HANDLING_PATTERNS = {
        // Logging with exception
        Pattern.compile("log\\s*\\.\\w+\\s*\\([^)]*,\\s*\\w+\\s*\\)"),
        Pattern.compile("LOG\\s*\\.\\w+\\s*\\([^)]*,\\s*\\w+\\s*\\)"),
        Pattern.compile("logger\\s*\\.\\w+\\s*\\([^)]*,\\s*\\w+\\s*\\)"),
        // printStackTrace
        Pattern.compile("\\w+\\.printStackTrace\\s*\\("),
        // Wrapping with cause
        Pattern.compile("throw\\s+new\\s+\\w+Exception\\s*\\([^)]*,\\s*\\w+\\s*\\)"),
        // Rethrowing with cause
        Pattern.compile("throw\\s+new\\s+\\w+Exception\\s*\\(\\s*\\w+\\s*\\)"),
        // initCause
        Pattern.compile("\\w+\\.initCause\\s*\\("),
        // Meaningful handling: rollback, cleanup, close, etc.
        Pattern.compile("\\w*\\.rollback\\s*\\("),
        Pattern.compile("\\w*\\.close\\s*\\("),
        Pattern.compile("\\w*\\.release\\s*\\("),
        Pattern.compile("\\w*\\.cleanup\\s*\\("),
        Pattern.compile("\\w*\\.unlock\\s*\\("),
    };

    @Override
    public String getRuleKey() { return "RSPEC-1166-CFG"; }

    @Override
    public String getName() { return "Exception handlers should preserve the original exception"; }

    @Override
    public String getCategory() { return "BUG"; }

    @SuppressWarnings("unchecked")
    @Override
    public List<QualityIssue> check(Map<String, Object> classAsset) {
        List<QualityIssue> issues = new ArrayList<>();
        String className = (String) classAsset.getOrDefault("address", "");
        String filePath = (String) classAsset.getOrDefault("source_file", "");

        List<Map<String, Object>> methods = (List<Map<String, Object>>) classAsset.getOrDefault("methods_full", Collections.emptyList());

        for (Map<String, Object> method : methods) {
            String methodBody = (String) method.getOrDefault("body_code", "");
            String methodName = (String) method.get("name");
            int line = (int) method.getOrDefault("line_start", 0);

            if (methodBody == null || methodBody.isEmpty()) continue;

            try {
                String fullMethod = "void dummy() {\n" + methodBody + "\n}";
                MethodDeclaration md = StaticJavaParser.parseMethodDeclaration(fullMethod);
                CFGBuilder builder = new CFGBuilder();
                ControlFlowGraph cfg = builder.build(md);

                for (CFGNode node : cfg.getNodes()) {
                    if (node.getType() != CFGNode.NodeType.CATCH) continue;

                    // Check if catch body has ANY statements
                    List<com.github.javaparser.ast.stmt.Statement> stmts = node.getStatements();

                    if (stmts.isEmpty()) {
                        // Issue 1: Completely empty catch block
                        issues.add(new QualityIssue(
                                getRuleKey(), getName(), Severity.CRITICAL, getCategory(),
                                filePath, className, methodName, line,
                                "Empty catch block - exception is silently swallowed",
                                "empty catch"
                        ));
                    } else {
                        // Check if catch body has PROPER exception handling
                        boolean hasHandling = false;
                        String catchBody = node.toString();

                        for (Pattern pat : HANDLING_PATTERNS) {
                            if (pat.matcher(catchBody).find()) {
                                hasHandling = true;
                                break;
                            }
                        }

                        // Also check the full method body for logging patterns
                        if (!hasHandling && methodBody.contains("log.")) {
                            // Check if the catch block references the exception variable
                            String catchLabel = node.getLabel();
                            String varName = extractExceptionVarName(catchLabel);
                            if (varName != null && methodBody.contains("log") && methodBody.contains(varName)) {
                                hasHandling = true;
                            }
                        }

                        if (!hasHandling) {
                            // Check if catch just rethrows without cause
                            boolean rethrowsWithoutCause = false;
                            for (com.github.javaparser.ast.stmt.Statement stmt : stmts) {
                                String stmtStr = stmt.toString();
                                if (stmtStr.matches("throw\\s+\\w+\\s*;") ||
                                    (stmtStr.contains("throw new") && !stmtStr.contains("e)") &&
                                     !stmtStr.contains("cause") && !stmtStr.contains("initCause"))) {
                                    rethrowsWithoutCause = true;
                                }
                            }

                            if (rethrowsWithoutCause) {
                                issues.add(new QualityIssue(
                                        getRuleKey(), getName(), Severity.MAJOR, getCategory(),
                                        filePath, className, methodName, line,
                                        "Exception rethrown without preserving original cause",
                                        "rethrow without cause"
                                ));
                            } else if (stmts.size() <= 1) {
                                // Only 1 statement and it's not proper handling
                                // Could be just a comment or simple assignment
                                String stmtStr = stmts.get(0).toString().trim();
                                if (!stmtStr.contains("log") && !stmtStr.contains("print") &&
                                    !stmtStr.contains("throw") && !stmtStr.contains("return")) {
                                    issues.add(new QualityIssue(
                                            getRuleKey(), getName(), Severity.MINOR, getCategory(),
                                            filePath, className, methodName, line,
                                            "Exception caught but not properly handled",
                                            "insufficient handling"
                                    ));
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                // If we can't parse, skip
            }
        }

        return issues;
    }

    /**
     * Extract the exception variable name from catch label.
     * e.g., "catch(Exception e)" → "e"
     */
    private String extractExceptionVarName(String catchLabel) {
        Matcher m = Pattern.compile("catch\\s*\\(\\w+\\s+(\\w+)\\)").matcher(catchLabel);
        if (m.find()) return m.group(1);
        return null;
    }
}
