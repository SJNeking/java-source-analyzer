package cn.dolphinmind.glossary.java.analyze.quality;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates rule execution and aggregates results.
 *
 * Features:
 * - Rule execution time tracking
 * - Issue deduplication
 * - Rule filtering by class features (tags)
 * - Proper logging instead of silent exception swallowing
 * - Parallel execution support
 */
public class RuleEngine {

    private static final Logger logger = Logger.getLogger(RuleEngine.class.getName());

    private final List<QualityRule> rules = new ArrayList<>();
    private final Map<String, Integer> ruleHitCounts = new LinkedHashMap<>();
    private final Map<String, Long> ruleExecutionTimesMs = new LinkedHashMap<>();
    private final Map<String, Integer> ruleSkipCounts = new LinkedHashMap<>();

    // Deduplication set: tracks (ruleKey + filePath + line) to avoid duplicate reports
    private final Set<String> seenIssues = new HashSet<>();

    public void registerRule(QualityRule rule) {
        rules.add(rule);
        ruleHitCounts.put(rule.getRuleKey(), 0);
        ruleExecutionTimesMs.put(rule.getRuleKey(), 0L);
        ruleSkipCounts.put(rule.getRuleKey(), 0);
    }

    /**
     * Run all rules against a list of class assets.
     */
    public List<QualityIssue> run(List<Map<String, Object>> classAssets) {
        List<QualityIssue> allIssues = new ArrayList<>();
        seenIssues.clear();

        for (Map<String, Object> classAsset : classAssets) {
            for (QualityRule rule : rules) {
                // Skip rules that don't match class features
                if (!isRuleApplicable(rule, classAsset)) {
                    ruleSkipCounts.merge(rule.getRuleKey(), 1, Integer::sum);
                    continue;
                }

                long startMs = System.currentTimeMillis();
                try {
                    List<QualityIssue> issues = rule.check(classAsset);
                    for (QualityIssue issue : issues) {
                        if (addUniqueIssue(issue)) {
                            allIssues.add(issue);
                        }
                    }
                    ruleHitCounts.merge(rule.getRuleKey(), issues.size(), Integer::sum);
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                        "Rule {0} failed on class {1}: {2}",
                        new Object[]{rule.getRuleKey(),
                            classAsset.getOrDefault("address", "unknown"),
                            e.getMessage()});
                    logger.log(Level.FINE, "Stack trace", e);
                } finally {
                    long elapsed = System.currentTimeMillis() - startMs;
                    ruleExecutionTimesMs.merge(rule.getRuleKey(), elapsed, Long::sum);
                }
            }
        }

        return allIssues;
    }

    /**
     * Run all rules against a list of class assets in parallel.
     * Use with caution: parallel execution order is not guaranteed.
     */
    public List<QualityIssue> runParallel(List<Map<String, Object>> classAssets) {
        List<QualityIssue> allIssues = Collections.synchronizedList(new ArrayList<>());
        seenIssues.clear();

        classAssets.parallelStream().forEach(classAsset -> {
            for (QualityRule rule : rules) {
                if (!isRuleApplicable(rule, classAsset)) {
                    continue;
                }

                long startMs = System.currentTimeMillis();
                try {
                    List<QualityIssue> issues = rule.check(classAsset);
                    for (QualityIssue issue : issues) {
                        if (addUniqueIssue(issue)) {
                            allIssues.add(issue);
                        }
                    }
                    synchronized (ruleHitCounts) {
                        ruleHitCounts.merge(rule.getRuleKey(), issues.size(), Integer::sum);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                        "Rule {0} failed on class {1}: {2}",
                        new Object[]{rule.getRuleKey(),
                            classAsset.getOrDefault("address", "unknown"),
                            e.getMessage()});
                } finally {
                    long elapsed = System.currentTimeMillis() - startMs;
                    synchronized (ruleExecutionTimesMs) {
                        ruleExecutionTimesMs.merge(rule.getRuleKey(), elapsed, Long::sum);
                    }
                }
            }
        });

        return allIssues;
    }

    /**
     * Check if a rule is applicable to the given class asset based on tags.
     * Rules can declare required tags (e.g., "spring", "jpa") and will be
     * skipped if the class doesn't have those features.
     */
    @SuppressWarnings("unchecked")
    protected boolean isRuleApplicable(QualityRule rule, Map<String, Object> classAsset) {
        // Check for rule-specific tags in the class asset
        Map<String, Object> tags = (Map<String, Object>) classAsset.getOrDefault("tags", Collections.emptyMap());

        // Check for annotations that might indicate framework usage
        List<String> annotations = (List<String>) classAsset.getOrDefault("annotations", Collections.emptyList());

        // Spring rules only apply to Spring-annotated classes
        if (rule.getClass().getName().contains("SpringBoot")) {
            boolean hasSpringAnnotation = annotations.stream()
                .anyMatch(a -> a.contains("Spring") || a.contains("Component") ||
                               a.contains("Service") || a.contains("Repository") ||
                               a.contains("Controller") || a.contains("RestController"));
            if (!hasSpringAnnotation) {
                return false;
            }
        }

        // Database rules only apply to classes with DB-related annotations
        if (rule.getClass().getName().contains("Database")) {
            boolean hasDbAnnotation = annotations.stream()
                .anyMatch(a -> a.contains("Entity") || a.contains("Repository") ||
                               a.contains("Jpa") || a.contains("Table"));
            if (!hasDbAnnotation) {
                return false;
            }
        }

        // Test rules only apply to test classes
        if (rule.getClass().getName().contains("TestQuality")) {
            boolean isTestClass = classAsset.getOrDefault("address", "").toString().contains("Test") ||
                annotations.stream().anyMatch(a -> a.contains("Test"));
            if (!isTestClass) {
                return false;
            }
        }

        return true;
    }

    /**
     * Add a unique issue (deduplication based on ruleKey + file + line).
     * @return true if the issue was added, false if it was a duplicate
     */
    private boolean addUniqueIssue(QualityIssue issue) {
        String key = issue.getRuleKey() + "|" + issue.getFilePath() + "|" +
                     issue.getStartLine() + "|" + issue.getMethodName();
        return seenIssues.add(key);
    }

    public List<QualityRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public Map<String, Integer> getRuleHitCounts() {
        return Collections.unmodifiableMap(ruleHitCounts);
    }

    /**
     * Get rule execution times in milliseconds.
     */
    public Map<String, Long> getRuleExecutionTimesMs() {
        return Collections.unmodifiableMap(ruleExecutionTimesMs);
    }

    /**
     * Get number of times each rule was skipped due to inapplicability.
     */
    public Map<String, Integer> getRuleSkipCounts() {
        return Collections.unmodifiableMap(ruleSkipCounts);
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
        // Collect all unique categories from issues
        Set<String> categories = new LinkedHashSet<>();
        for (QualityIssue issue : issues) {
            categories.add(issue.getCategory());
        }
        for (String cat : categories) {
            final String c = cat;
            byCategory.put(cat, issues.stream().filter(i -> i.getCategory().equals(c)).count());
        }
        summary.put("by_category", byCategory);

        summary.put("top_rules", getTopViolatedRules(10));

        // Add performance stats
        Map<String, Object> perfStats = new LinkedHashMap<>();
        long totalTime = ruleExecutionTimesMs.values().stream().mapToLong(Long::longValue).sum();
        perfStats.put("total_analysis_time_ms", totalTime);
        Map<String, Long> topSlow = ruleExecutionTimesMs.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), LinkedHashMap::putAll);
        perfStats.put("slowest_rules_ms", topSlow);
        summary.put("performance", perfStats);

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
