package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.dataflow.EnhancedTaintEngine;
import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.*;

/**
 * Enhanced taint analysis rule using Control Flow Aware Data Flow Graph.
 *
 * Detects vulnerabilities where untrusted data flows from sources to sinks
 * without sanitization, using precise AST-based data flow analysis.
 *
 * Improvements over TaintFlowRule:
 * - Uses CFG-DFG for accurate variable tracking
 * - Path-sensitive analysis through branches and loops
 * - Lower false positive rate
 * - Handles string concatenation in sink arguments
 */
public class EnhancedTaintFlowRule implements QualityRule {

    @Override
    public String getRuleKey() { return "RSPEC-2076-ENHANCED"; }

    @Override
    public String getName() { return "User input should not reach security-sensitive operations (enhanced)"; }

    @Override
    public String getCategory() { return "SECURITY"; }

    @SuppressWarnings("unchecked")
    @Override
    public List<QualityIssue> check(Map<String, Object> classAsset) {
        List<QualityIssue> issues = new ArrayList<>();
        String className = (String) classAsset.getOrDefault("address", "");
        String filePath = (String) classAsset.getOrDefault("source_file", "");

        List<Map<String, Object>> methods = (List<Map<String, Object>>)
                classAsset.getOrDefault("methods_full", Collections.emptyList());

        EnhancedTaintEngine engine = new EnhancedTaintEngine();

        for (Map<String, Object> method : methods) {
            String methodBody = (String) method.getOrDefault("body_code", "");
            String methodName = (String) method.get("name");
            int line = (int) method.getOrDefault("line_start", 0);

            if (methodBody == null || methodBody.isEmpty()) continue;

            try {
                // Parse method and run enhanced taint analysis
                String fullMethod = "void dummy() {\n" + methodBody + "\n}";
                MethodDeclaration md = StaticJavaParser.parseMethodDeclaration(fullMethod);

                List<EnhancedTaintEngine.EnhancedTaintFinding> findings = engine.analyze(md);

                for (EnhancedTaintEngine.EnhancedTaintFinding finding : findings) {
                    Severity severity = getSeverityForSinkType(finding.getSinkType());

                    issues.add(new QualityIssue.Builder()
                        .ruleKey(getRuleKey())
                        .ruleName(getName())
                        .severity(severity)
                        .category(getCategory())
                        .filePath(filePath)
                        .className(className)
                        .methodName(methodName)
                        .location(
                            finding.getSourceLine(),
                            finding.getSinkLine(),
                            1, 0
                        )
                        .message(finding.getSinkType() + ": tainted variable '" +
                            finding.getTaintedVar() + "' flows from line " +
                            finding.getSourceLine() + " to line " + finding.getSinkLine() +
                            " without sanitization")
                        .evidence(finding.getSource().getCode() + " → " + finding.getSink().getCode())
                        .codeSnippet("Source: " + finding.getSource().getCode() + "\n" +
                            "Sink: " + finding.getSink().getCode())
                        .build());
                }
            } catch (Exception e) {
                // If parsing fails, skip this method
            }
        }

        return issues;
    }

    private Severity getSeverityForSinkType(String sinkType) {
        switch (sinkType) {
            case "SQL Injection":
            case "Command Injection":
                return Severity.CRITICAL;
            case "Path Traversal":
            case "XSS":
            case "XSS/Response Manipulation":
                return Severity.MAJOR;
            default:
                return Severity.MINOR;
        }
    }
}
