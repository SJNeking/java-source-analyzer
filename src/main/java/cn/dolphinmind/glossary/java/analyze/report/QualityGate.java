package cn.dolphinmind.glossary.java.analyze.report;

import java.util.*;

/**
 * Quality Gate: determines whether a build/PR passes based on configurable thresholds.
 *
 * This is the CI/CD integration point - used in GitHub Actions, Jenkins, etc.
 * Returns PASS/FAIL with detailed reasons.
 *
 * Default thresholds (configurable):
 * - No CRITICAL issues allowed
 * - Max MAJOR issues: 0
 * - Max total issues: 10
 * - Max technical debt ratio: 5%
 * - Min code coverage: 0% (not enforced yet, placeholder)
 */
public class QualityGate {

    public static class GateResult {
        private final boolean passed;
        private final List<String> reasons;
        private final Map<String, Object> metrics;

        public GateResult(boolean passed, List<String> reasons, Map<String, Object> metrics) {
            this.passed = passed;
            this.reasons = Collections.unmodifiableList(reasons);
            this.metrics = Collections.unmodifiableMap(metrics);
        }

        public boolean isPassed() { return passed; }
        public List<String> getReasons() { return reasons; }
        public Map<String, Object> getMetrics() { return metrics; }
    }

    /**
     * Evaluate the quality gate against scan results.
     */
    @SuppressWarnings("unchecked")
    public GateResult evaluate(Map<String, Object> scanResult, Map<String, Object> debtEstimate) {
        List<String> reasons = new ArrayList<>();
        Map<String, Object> metrics = new LinkedHashMap<>();
        boolean passed = true;

        List<Map<String, Object>> qualityIssues =
                (List<Map<String, Object>>) scanResult.getOrDefault("quality_issues", Collections.emptyList());
        Map<String, Object> qualitySummary =
                (Map<String, Object>) scanResult.getOrDefault("quality_summary", Collections.emptyMap());
        Map<String, Number> bySeverity =
                (Map<String, Number>) qualitySummary.getOrDefault("by_severity", Collections.emptyMap());

        int critical = bySeverity.getOrDefault("CRITICAL", 0).intValue();
        int major = bySeverity.getOrDefault("MAJOR", 0).intValue();
        int total = qualityIssues.size();
        double debtRatio = 0;
        if (debtEstimate != null && debtEstimate.containsKey("technical_debt_ratio_pct")) {
            debtRatio = ((Number) debtEstimate.get("technical_debt_ratio_pct")).doubleValue();
        }

        // Rule 1: No CRITICAL issues
        if (critical > 0) {
            passed = false;
            reasons.add("CRITICAL issues found: " + critical);
        }
        metrics.put("critical_count", critical);

        // Rule 2: No MAJOR issues
        if (major > 0) {
            passed = false;
            reasons.add("MAJOR issues found: " + major);
        }
        metrics.put("major_count", major);

        // Rule 3: Total issues threshold
        if (total > 10) {
            passed = false;
            reasons.add("Total issues exceeds threshold: " + total + " > 10");
        }
        metrics.put("total_issues", total);

        // Rule 4: Technical debt ratio
        if (debtRatio > 5) {
            passed = false;
            reasons.add("Technical debt ratio too high: " + String.format("%.1f%%", debtRatio) + " > 5%");
        }
        metrics.put("debt_ratio_pct", debtRatio);

        if (passed) {
            reasons.add("All quality gates passed");
        }

        return new GateResult(passed, reasons, metrics);
    }

    /**
     * Generate GitHub Check Run compatible output.
     */
    public String toGitHubCheckJson(QualityGate.GateResult result, String frameworkName, String version) {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("name", "java-source-analyzer");
        check.put("head_sha", "HEAD");
        check.put("status", "completed");
        check.put("conclusion", result.isPassed() ? "success" : "failure");

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("title", result.isPassed() ? "✅ Quality Gate Passed" : "❌ Quality Gate Failed");

        StringBuilder summary = new StringBuilder();
        summary.append("## ").append(frameworkName).append(" v").append(version).append("\n\n");
        summary.append("| Metric | Value |\n|---|---|\n");
        for (Map.Entry<String, Object> entry : result.getMetrics().entrySet()) {
            summary.append("| ").append(entry.getKey()).append(" | ").append(entry.getValue()).append(" |\n");
        }
        summary.append("\n### Reasons\n");
        for (String reason : result.getReasons()) {
            summary.append("- ").append(reason).append("\n");
        }
        output.put("summary", summary.toString());

        check.put("output", output);

        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(check);
    }
}
