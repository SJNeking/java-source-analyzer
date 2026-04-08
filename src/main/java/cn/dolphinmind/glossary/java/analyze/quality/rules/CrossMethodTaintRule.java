package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.dataflow.CrossMethodTaintAnalyzer;
import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;

/**
 * Cross-method taint analysis rule.
 * Detects vulnerabilities where tainted data flows across method boundaries
 * to security-sensitive operations without sanitization.
 */
public class CrossMethodTaintRule implements QualityRule {

    @Override
    public String getRuleKey() { return "RSPEC-2076-XMETHOD"; }

    @Override
    public String getName() { return "Cross-method taint flow vulnerability"; }

    @Override
    public String getCategory() { return "SECURITY"; }

    @Override
    public List<QualityIssue> check(Map<String, Object> classAsset) {
        return Collections.emptyList(); // Collection-level rule
    }

    /**
     * Analyze all methods for cross-method taint flows.
     */
    @SuppressWarnings("unchecked")
    public List<QualityIssue> analyzeAll(List<Map<String, Object>> classAssets,
                                          List<Map<String, String>> callGraph) {
        List<QualityIssue> issues = new ArrayList<>();
        CrossMethodTaintAnalyzer analyzer = new CrossMethodTaintAnalyzer();

        // Collect all method assets
        List<Map<String, Object>> allMethods = new ArrayList<>();
        for (Map<String, Object> classAsset : classAssets) {
            List<Map<String, Object>> methods =
                    (List<Map<String, Object>>) classAsset.getOrDefault("methods_full", Collections.emptyList());
            allMethods.addAll(methods);
        }

        List<CrossMethodTaintAnalyzer.CrossMethodTaintPath> findings =
                analyzer.analyze(allMethods, callGraph);

        for (CrossMethodTaintAnalyzer.CrossMethodTaintPath finding : findings) {
            String vulnType = finding.getVulnerabilityType();
            Severity severity = getSeverity(vulnType);

            // Build path description
            StringBuilder pathDesc = new StringBuilder();
            pathDesc.append(finding.getSource().getClassName()).append("#")
                    .append(finding.getSource().getMethodName());
            for (CrossMethodTaintAnalyzer.TaintNode node : finding.getPath()) {
                pathDesc.append(" → ").append(node.getClassName()).append("#").append(node.getMethodName());
            }
            pathDesc.append(" → [").append(vulnType).append("]");

            issues.add(new QualityIssue(
                    getRuleKey(), getName(), severity, getCategory(),
                    "", finding.getSink().getClassName(), finding.getSink().getMethodName(), 0,
                    "Cross-method taint: " + vulnType + " through " + finding.getMethodDepth() + " methods",
                    pathDesc.toString()
            ));
        }

        return issues;
    }

    private Severity getSeverity(String vulnType) {
        switch (vulnType) {
            case "SQL Injection":
            case "Command Injection":
                return Severity.CRITICAL;
            case "Path Traversal":
            case "SSRF":
            case "LDAP Injection":
            case "XPath Injection":
                return Severity.MAJOR;
            default:
                return Severity.MINOR;
        }
    }
}
