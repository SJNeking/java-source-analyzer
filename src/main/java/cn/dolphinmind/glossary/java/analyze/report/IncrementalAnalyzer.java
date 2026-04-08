package cn.dolphinmind.glossary.java.analyze.report;

import java.util.*;

/**
 * Performs incremental/delta analysis by comparing current scan results with a previous baseline.
 *
 * Reports:
 * - New issues introduced since last scan
 * - Fixed issues (resolved since last scan)
 * - Trend (improving, stable, degrading)
 *
 * This is critical for enterprise CI/CD integration where teams only
 * care about what changed in their PR, not the full scan results.
 */
public class IncrementalAnalyzer {

    /**
     * Compare current scan with baseline and return delta.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyze(List<Map<String, Object>> currentIssues,
                                        List<Map<String, Object>> baselineIssues) {
        Map<String, Object> delta = new LinkedHashMap<>();

        // Create unique keys for each issue
        Set<String> currentKeys = new HashSet<>();
        Map<String, Map<String, Object>> currentMap = new LinkedHashMap<>();
        for (Map<String, Object> issue : currentIssues) {
            String key = issueKey(issue);
            currentKeys.add(key);
            currentMap.put(key, issue);
        }

        Set<String> baselineKeys = new HashSet<>();
        Map<String, Map<String, Object>> baselineMap = new LinkedHashMap<>();
        for (Map<String, Object> issue : baselineIssues) {
            String key = issueKey(issue);
            baselineKeys.add(key);
            baselineMap.put(key, issue);
        }

        // New issues (in current but not in baseline)
        List<Map<String, Object>> newIssues = new ArrayList<>();
        for (String key : currentKeys) {
            if (!baselineKeys.contains(key)) {
                newIssues.add(currentMap.get(key));
            }
        }

        // Fixed issues (in baseline but not in current)
        List<Map<String, Object>> fixedIssues = new ArrayList<>();
        for (String key : baselineKeys) {
            if (!currentKeys.contains(key)) {
                fixedIssues.add(baselineMap.get(key));
            }
        }

        // Trend analysis
        String trend = calculateTrend(currentIssues.size(), baselineIssues.size(), newIssues.size(), fixedIssues.size());

        delta.put("new_issues", newIssues);
        delta.put("fixed_issues", fixedIssues);
        delta.put("new_count", newIssues.size());
        delta.put("fixed_count", fixedIssues.size());
        delta.put("current_total", currentIssues.size());
        delta.put("baseline_total", baselineIssues.size());
        delta.put("trend", trend);
        delta.put("net_change", newIssues.size() - fixedIssues.size());

        return delta;
    }

    /**
     * Generate a unique key for an issue to match across scans.
     */
    private String issueKey(Map<String, Object> issue) {
        String ruleKey = (String) issue.getOrDefault("rule_key", "");
        String cls = (String) issue.getOrDefault("class", "");
        String method = (String) issue.getOrDefault("method", "");
        String message = (String) issue.getOrDefault("message", "");
        // Use first 50 chars of message as part of key (line numbers may shift)
        String msgPrefix = message.length() > 50 ? message.substring(0, 50) : message;
        return ruleKey + "|" + cls + "|" + method + "|" + msgPrefix;
    }

    /**
     * Calculate the quality trend direction.
     */
    private String calculateTrend(int currentTotal, int baselineTotal, int newCount, int fixedCount) {
        if (newCount == 0 && fixedCount == 0) return "STABLE";
        if (newCount > fixedCount) return "DEGRADING";
        if (newCount < fixedCount) return "IMPROVING";
        return "STABLE";
    }
}
