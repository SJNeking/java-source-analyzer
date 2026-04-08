package cn.dolphinmind.glossary.java.analyze.quality;

import java.util.*;

/**
 * Orchestrates rule execution and aggregates results
 */
public class RuleEngine {

    private final List<QualityRule> rules = new ArrayList<>();
    private final Map<String, Integer> ruleHitCounts = new LinkedHashMap<>();

    public void registerRule(QualityRule rule) {
        rules.add(rule);
        ruleHitCounts.put(rule.getRuleKey(), 0);
    }

    /**
     * Run all rules against a list of class assets
     */
    public List<QualityIssue> run(List<Map<String, Object>> classAssets) {
        List<QualityIssue> allIssues = new ArrayList<>();

        for (Map<String, Object> classAsset : classAssets) {
            for (QualityRule rule : rules) {
                try {
                    List<QualityIssue> issues = rule.check(classAsset);
                    allIssues.addAll(issues);
                    ruleHitCounts.merge(rule.getRuleKey(), issues.size(), Integer::sum);
                } catch (Exception e) {
                    // Rule should not crash the engine
                }
            }
        }

        return allIssues;
    }

    public List<QualityRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public Map<String, Integer> getRuleHitCounts() {
        return Collections.unmodifiableMap(ruleHitCounts);
    }

    /**
     * Get summary statistics
     */
    public Map<String, Object> getSummary(List<QualityIssue> issues) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_issues", issues.size());

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Severity s : Severity.values()) {
            bySeverity.put(s.name(), issues.stream().filter(i -> i.getSeverity() == s).count());
        }
        summary.put("by_severity", bySeverity);

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (String cat : Arrays.asList("BUG", "CODE_SMELL", "SECURITY")) {
            byCategory.put(cat, issues.stream().filter(i -> i.getCategory().equals(cat)).count());
        }
        summary.put("by_category", byCategory);

        summary.put("top_rules", getTopViolatedRules(10));

        return summary;
    }

    private List<Map<String, Object>> getTopViolatedRules(int limit) {
        List<Map<String, Object>> top = new ArrayList<>();
        ruleHitCounts.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .forEach(e -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("rule_key", e.getKey());
                    entry.put("violations", e.getValue());
                    top.add(entry);
                });
        return top;
    }
}
