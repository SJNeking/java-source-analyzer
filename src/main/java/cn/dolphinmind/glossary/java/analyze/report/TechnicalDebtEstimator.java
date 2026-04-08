package cn.dolphinmind.glossary.java.analyze.report;

import java.util.*;

/**
 * Estimates technical debt using the SQALE (Software Quality Assessment based on Lifecycle Expectations) method.
 *
 * Each issue has a remediation cost (minutes to fix):
 * - CRITICAL: 30 minutes
 * - MAJOR: 15 minutes
 * - MINOR: 5 minutes
 *
 * Technical Debt Ratio = Remediation Cost / Development Cost
 * Development Cost = (LOC × cost per line)
 * Cost per line = 0.06 USD (industry average)
 *
 * Rating Scale (like SonarQube):
 * A: <= 5%
 * B: <= 10%
 * C: <= 20%
 * D: <= 50%
 * E: > 50%
 */
public class TechnicalDebtEstimator {

    private static final Map<String, Integer> REMEDIATION_MINUTES = new LinkedHashMap<>();
    static {
        REMEDIATION_MINUTES.put("CRITICAL", 30);
        REMEDIATION_MINUTES.put("MAJOR", 15);
        REMEDIATION_MINUTES.put("MINOR", 5);
        REMEDIATION_MINUTES.put("INFO", 1);
    }

    private static final double COST_PER_LINE = 0.06; // USD per line of code

    @SuppressWarnings("unchecked")
    public Map<String, Object> estimate(Map<String, Object> scanResult) {
        List<Map<String, Object>> qualityIssues =
                (List<Map<String, Object>>) scanResult.getOrDefault("quality_issues", Collections.emptyList());
        List<Map<String, Object>> assets =
                (List<Map<String, Object>>) scanResult.getOrDefault("assets", Collections.emptyList());

        // Calculate total lines of code
        long totalLoc = 0;
        for (Map<String, Object> asset : assets) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> methods =
                    (List<Map<String, Object>>) asset.getOrDefault("methods_full", Collections.emptyList());
            for (Map<String, Object> method : methods) {
                int lineCount = ((Number) method.getOrDefault("line_count", 0)).intValue();
                totalLoc += lineCount;
            }
        }

        // Calculate remediation cost
        int totalMinutes = 0;
        Map<String, Integer> costByCategory = new LinkedHashMap<>();
        Map<String, Integer> costBySeverity = new LinkedHashMap<>();

        for (Map<String, Object> issue : qualityIssues) {
            String severity = (String) issue.getOrDefault("severity", "MINOR");
            String category = (String) issue.getOrDefault("category", "CODE_SMELL");
            int minutes = REMEDIATION_MINUTES.getOrDefault(severity, 5);

            totalMinutes += minutes;
            costByCategory.merge(category, minutes, Integer::sum);
            costBySeverity.merge(severity, minutes, Integer::sum);
        }

        // Development cost
        double devCostUsd = totalLoc * COST_PER_LINE;
        double remediationCostUsd = totalMinutes / 60.0 * 50.0; // $50/hour developer rate
        double debtRatio = devCostUsd > 0 ? (remediationCostUsd / devCostUsd * 100) : 0;
        String rating = getRating(debtRatio);

        // Build result
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_lines_of_code", totalLoc);
        result.put("total_issues", qualityIssues.size());
        result.put("total_remediation_minutes", totalMinutes);
        result.put("total_remediation_hours", Math.round(totalMinutes / 60.0 * 10) / 10.0);
        result.put("development_cost_usd", Math.round(devCostUsd * 100) / 100.0);
        result.put("remediation_cost_usd", Math.round(remediationCostUsd * 100) / 100.0);
        result.put("technical_debt_ratio_pct", Math.round(debtRatio * 10) / 10.0);
        result.put("rating", rating);
        result.put("cost_by_category", costByCategory);
        result.put("cost_by_severity", costBySeverity);

        // Per-category breakdown
        Map<String, Map<String, Object>> categoryBreakdown = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> issuesByCategory = new LinkedHashMap<>();
        for (Map<String, Object> issue : qualityIssues) {
            String cat = (String) issue.getOrDefault("category", "UNKNOWN");
            issuesByCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(issue);
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : issuesByCategory.entrySet()) {
            Map<String, Object> catData = new LinkedHashMap<>();
            catData.put("issue_count", entry.getValue().size());
            int catMinutes = 0;
            for (Map<String, Object> issue : entry.getValue()) {
                String sev = (String) issue.getOrDefault("severity", "MINOR");
                catMinutes += REMEDIATION_MINUTES.getOrDefault(sev, 5);
            }
            catData.put("remediation_minutes", catMinutes);
            catData.put("remediation_hours", Math.round(catMinutes / 60.0 * 10) / 10.0);
            categoryBreakdown.put(entry.getKey(), catData);
        }
        result.put("category_breakdown", categoryBreakdown);

        return result;
    }

    private String getRating(double debtRatio) {
        if (debtRatio <= 5) return "A";
        if (debtRatio <= 10) return "B";
        if (debtRatio <= 20) return "C";
        if (debtRatio <= 50) return "D";
        return "E";
    }
}
