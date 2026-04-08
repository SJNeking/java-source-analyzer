package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;

/**
 * Base class for rules that analyze method-level code patterns.
 * Subclasses implement checkMethod() for each method.
 */
public abstract class AbstractMethodRule implements QualityRule {

    @Override
    @SuppressWarnings("unchecked")
    public List<QualityIssue> check(Map<String, Object> classAsset) {
        List<QualityIssue> issues = new ArrayList<>();
        String filePath = (String) classAsset.getOrDefault("source_file", "");
        String className = (String) classAsset.getOrDefault("address", "");

        List<Map<String, Object>> methods = (List<Map<String, Object>>)
                classAsset.getOrDefault("methods_full", Collections.emptyList());

        for (Map<String, Object> method : methods) {
            List<QualityIssue> methodIssues = checkMethod(method, filePath, className);
            issues.addAll(methodIssues);
        }
        return issues;
    }

    protected abstract List<QualityIssue> checkMethod(Map<String, Object> method,
                                                       String filePath, String className);
}
