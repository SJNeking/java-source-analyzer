package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.arch.ArchitectureAnalyzer;
import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;

/**
 * Architecture violation rule: detects package layer violations and circular dependencies
 * at the class/module level.
 *
 * Detects:
 * - CONTROLLER → REPOSITORY (should go through SERVICE)
 * - DTO → CONTROLLER (reverse dependency)
 * - Circular dependencies between classes/modules
 */
public class ArchitectureViolationRule implements QualityRule {

    @Override
    public String getRuleKey() { return "RSPEC-1200"; }

    @Override
    public String getName() { return "Classes should not depend on packages that should not depend on them"; }

    @Override
    public String getCategory() { return "CODE_SMELL"; }

    @Override
    public List<QualityIssue> check(Map<String, Object> classAsset) {
        // This is a collection-level rule
        return Collections.emptyList();
    }

    /**
     * Run architecture analysis on all class assets and return issues.
     */
    public List<QualityIssue> analyzeAll(List<Map<String, Object>> classAssets) {
        List<QualityIssue> issues = new ArrayList<>();

        ArchitectureAnalyzer analyzer = new ArchitectureAnalyzer();
        for (Map<String, Object> asset : classAssets) {
            analyzer.registerClass(asset);
        }
        analyzer.analyze();

        // Layer violations
        for (ArchitectureAnalyzer.LayerViolation violation : analyzer.getLayerViolations()) {
            Map<String, Object> violationMap = violation.toMap();
            String sourceClass = (String) violationMap.get("source_class");
            Map<String, Object> classAsset = findClassByAddress(classAssets, sourceClass);
            if (classAsset == null) continue;

            issues.add(new QualityIssue(
                    getRuleKey(), getName(), Severity.MAJOR, getCategory(),
                    (String) classAsset.getOrDefault("source_file", ""),
                    sourceClass, "", 0,
                    "Layer violation: " + violationMap.get("source_layer") + " → " + violationMap.get("target_layer"),
                    (String) violationMap.get("source_class") + " (" + violationMap.get("source_layer") + ") → " +
                    violationMap.get("target_class") + " (" + violationMap.get("target_layer") + ")"
            ));
        }

        // Circular dependencies
        for (ArchitectureAnalyzer.CircularDependency cycle : analyzer.getCircularDependencies()) {
            List<String> cycleList = cycle.getCycle();
            if (cycleList.isEmpty()) continue;
            String firstClass = cycleList.get(0);
            Map<String, Object> classAsset = findClassByAddress(classAssets, firstClass);
            if (classAsset == null) continue;

            issues.add(new QualityIssue(
                    getRuleKey(), getName(), Severity.MAJOR, getCategory(),
                    (String) classAsset.getOrDefault("source_file", ""),
                    firstClass, "", 0,
                    "Circular class dependency: " + cycle,
                    cycle.toString()
            ));
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
