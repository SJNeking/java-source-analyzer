package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.spring.SpringBeanGraph;
import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;

/**
 * Spring Bean analysis rule: detects circular dependencies and layer violations.
 *
 * This is a class-level rule that analyzes the entire class asset collection.
 */
public class SpringBeanAnalysisRule implements QualityRule {

    @Override
    public String getRuleKey() { return "RSPEC-5135"; }

    @Override
    public String getName() { return "Spring Bean circular dependencies and layer violations should be avoided"; }

    @Override
    public String getCategory() { return "BUG"; }

    @SuppressWarnings("unchecked")
    @Override
    public List<QualityIssue> check(Map<String, Object> classAsset) {
        // This rule works at the class-collection level, not per-method
        // Issues will be reported against individual classes
        return Collections.emptyList();
    }

    /**
     * Run Spring Bean analysis on all class assets and return issues.
     */
    public List<QualityIssue> analyzeAll(List<Map<String, Object>> classAssets) {
        List<QualityIssue> issues = new ArrayList<>();

        SpringBeanGraph graph = new SpringBeanGraph();
        graph.analyzeAll(classAssets);

        // Circular dependencies
        for (SpringBeanGraph.CircularDependency cycle : graph.getCircularDependencies()) {
            String firstBean = cycle.getCycle().get(0);
            Map<String, Object> classAsset = findClassByAddress(classAssets, firstBean);
            if (classAsset == null) continue;

            issues.add(new QualityIssue(
                    getRuleKey(), getName(), Severity.CRITICAL, getCategory(),
                    (String) classAsset.getOrDefault("source_file", ""),
                    firstBean, "", 0,
                    "Spring Bean circular dependency: " + cycle,
                    cycle.toString()
            ));
        }

        // Layer violations
        for (SpringBeanGraph.LayerViolation violation : graph.getLayerViolations()) {
            Map<String, Object> classAsset = findClassByAddress(classAssets, violation.getSourceClass());
            if (classAsset == null) continue;

            issues.add(new QualityIssue(
                    getRuleKey(), getName(), Severity.MAJOR, getCategory(),
                    (String) classAsset.getOrDefault("source_file", ""),
                    violation.getSourceClass(), violation.getFieldName(), 0,
                    "Layer violation: " + violation,
                    violation.toString()
            ));
        }

        // Missing beans
        for (String missing : graph.getMissingBeans()) {
            String[] parts = missing.split(" → ");
            if (parts.length == 2) {
                Map<String, Object> classAsset = findClassByAddress(classAssets, parts[0]);
                if (classAsset == null) continue;

                issues.add(new QualityIssue(
                        getRuleKey(), getName(), Severity.MINOR, getCategory(),
                        (String) classAsset.getOrDefault("source_file", ""),
                        parts[0], parts[1], 0,
                        "Injected dependency not found in project: " + parts[1],
                        missing
                ));
            }
        }

        return issues;
    }

    private Map<String, Object> findClassByAddress(List<Map<String, Object>> classAssets, String address) {
        for (Map<String, Object> asset : classAssets) {
            String addr = (String) asset.getOrDefault("address", "");
            if (addr.equals(address) || addr.endsWith("." + address) || address.endsWith("." + addr)) {
                return asset;
            }
        }
        return null;
    }
}
