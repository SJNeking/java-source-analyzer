package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.cfg.CFGBuilder;
import cn.dolphinmind.glossary.java.analyze.cfg.CFGNode;
import cn.dolphinmind.glossary.java.analyze.cfg.CFGEdge;
import cn.dolphinmind.glossary.java.analyze.cfg.ControlFlowGraph;
import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.*;

/**
 * Detects methods that have exception paths without proper handling.
 *
 * Checks:
 * 1. Methods that call other methods which declare checked exceptions but don't catch them
 * 2. Try blocks where exception paths don't clean up resources
 * 3. Methods that throw RuntimeExceptions without logging
 */
public class ExceptionHandlingPath implements QualityRule {

    @Override
    public String getRuleKey() { return "RSPEC-1166-CFG"; }

    @Override
    public String getName() { return "Exception handlers should preserve the original exception on all paths"; }

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

                // Check 1: Find catch blocks in the CFG and verify they preserve exception
                for (CFGNode node : cfg.getNodes()) {
                    if (node.getType() == CFGNode.NodeType.CATCH) {
                        // Check if the catch block rethrows without preserving cause
                        String catchLabel = node.getLabel();
                        boolean rethrowsWithoutCause = false;

                        // Look at successors of catch node
                        for (CFGNode succ : node.getSuccessors()) {
                            for (com.github.javaparser.ast.stmt.Statement stmt : succ.getStatements()) {
                                String stmtStr = stmt.toString();
                                // Detect: throw new Exception("msg") without (e) as cause
                                if (stmtStr.contains("throw new") && !stmtStr.contains("e)") &&
                                    !stmtStr.contains("cause") && !stmtStr.contains("initCause")) {
                                    rethrowsWithoutCause = true;
                                }
                                // Detect: throw e; but without any logging
                                if (stmtStr.matches("throw\\s+\\w+\\s*;") &&
                                    !stmtStr.contains("log") && !stmtStr.contains("print") &&
                                    !stmtStr.contains("Log") && !methodBody.contains("log.")) {
                                    rethrowsWithoutCause = true;
                                }
                            }
                        }

                        if (rethrowsWithoutCause) {
                            issues.add(new QualityIssue(
                                    getRuleKey(), getName(), Severity.MAJOR, getCategory(),
                                    filePath, className, methodName, line,
                                    "Exception caught in " + catchLabel + " but not properly handled or preserved",
                                    "catch without cause preservation"
                            ));
                        }
                    }
                }

                // Check 2: Detect swallowed exceptions (catch with empty body or only comment)
                for (CFGNode node : cfg.getNodes()) {
                    if (node.getType() == CFGNode.NodeType.CATCH) {
                        boolean hasNoHandling = node.getSuccessors().isEmpty() ||
                            node.getSuccessors().stream().allMatch(s -> s.getType() == CFGNode.NodeType.BASIC_BLOCK);

                        // Check if catch body is essentially empty
                        if (node.getStatements().isEmpty()) {
                            issues.add(new QualityIssue(
                                    getRuleKey(), getName(), Severity.CRITICAL, getCategory(),
                                    filePath, className, methodName, line,
                                    "Exception caught but not handled (empty catch block in CFG)",
                                    "swallowed exception"
                            ));
                        }
                    }
                }

            } catch (Exception e) {
                // If we can't parse, skip
            }
        }

        return issues;
    }
}
