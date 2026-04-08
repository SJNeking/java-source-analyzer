package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.dataflow.TaintEngine;
import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.*;

/**
 * Taint analysis rule: detects flow of untrusted data to security-sensitive operations.
 *
 * Categories:
 * - SQL Injection: user input → SQL query
 * - Command Injection: user input → system command
 * - Path Traversal: user input → file path
 * - XSS: user input → HTTP response
 * - LDAP Injection: user input → LDAP query
 * - SSRF: user input → network request
 */
public class TaintFlowRule implements QualityRule {

    @Override
    public String getRuleKey() { return "RSPEC-2076"; }

    @Override
    public String getName() { return "User input should not reach security-sensitive operations"; }

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

        for (Map<String, Object> method : methods) {
            String methodBody = (String) method.getOrDefault("body_code", "");
            String methodName = (String) method.get("name");
            int line = (int) method.getOrDefault("line_start", 0);

            if (methodBody == null || methodBody.isEmpty()) continue;

            try {
                String fullMethod = "void dummy() {\n" + methodBody + "\n}";
                MethodDeclaration md = StaticJavaParser.parseMethodDeclaration(fullMethod);
                TaintEngine engine = new TaintEngine();
                List<TaintEngine.TaintFinding> findings = engine.analyze(md);

                for (TaintEngine.TaintFinding finding : findings) {
                    // Determine vulnerability type
                    String vulnType = classifyVulnerability(finding);
                    Severity severity = getVulnerabilitySeverity(vulnType);

                    issues.add(new QualityIssue(
                            getRuleKey(), getName(), severity, getCategory(),
                            filePath, className, methodName, line,
                            vulnType + ": tainted data flows from line " + finding.getSource().getLine() +
                            " to line " + finding.getSink().getLine() + " without sanitization",
                            finding.getSource().getCode() + " → " + finding.getSink().getCode()
                    ));
                }
            } catch (Exception e) {
                // If parsing fails, skip this method
            }
        }

        return issues;
    }

    private String classifyVulnerability(TaintEngine.TaintFinding finding) {
        String sinkPattern = finding.getSink().getPattern().toLowerCase();
        String sinkCode = finding.getSink().getCode().toLowerCase();

        if (sinkPattern.contains("executequery") || sinkPattern.contains("executeupdate") ||
            sinkPattern.contains("preparestatement") || sinkCode.contains("sql")) {
            return "SQL Injection";
        } else if (sinkPattern.contains("exec") || sinkPattern.contains("processbuilder") ||
                   sinkPattern.contains("runtime")) {
            return "Command Injection";
        } else if (sinkPattern.contains("new file") || sinkPattern.contains("paths.get") ||
                   sinkPattern.contains("filereader") || sinkPattern.contains("filewriter")) {
            return "Path Traversal";
        } else if (sinkPattern.contains("print") || sinkPattern.contains("println") ||
                   sinkPattern.contains("getwriter")) {
            return "XSS (Cross-Site Scripting)";
        } else if (sinkPattern.contains("search") || sinkPattern.contains("lookup")) {
            return "LDAP Injection";
        } else if (sinkPattern.contains("openconnection") || sinkPattern.contains("openstream")) {
            return "SSRF (Server-Side Request Forgery)";
        } else if (sinkPattern.contains("readobject")) {
            return "Insecure Deserialization";
        }
        return "Taint Flow";
    }

    private Severity getVulnerabilitySeverity(String vulnType) {
        switch (vulnType) {
            case "SQL Injection":
            case "Command Injection":
            case "Insecure Deserialization":
                return Severity.CRITICAL;
            case "Path Traversal":
            case "LDAP Injection":
            case "SSRF (Server-Side Request Forgery)":
                return Severity.MAJOR;
            case "XSS (Cross-Site Scripting)":
                return Severity.MAJOR;
            default:
                return Severity.MINOR;
        }
    }
}
