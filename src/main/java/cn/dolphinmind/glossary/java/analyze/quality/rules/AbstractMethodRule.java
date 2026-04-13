package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for rules that analyze method-level code patterns.
 *
 * Features:
 * - Unified exception handling with logging
 * - Iteration over methods_full AST data
 * - Template method checkMethod() for subclasses
 */
public abstract class AbstractMethodRule implements QualityRule {

    protected final Logger logger = Logger.getLogger(getClass().getName());

    @Override
    @SuppressWarnings("unchecked")
    public List<QualityIssue> check(Map<String, Object> classAsset) {
        List<QualityIssue> issues = new ArrayList<>();
        String filePath = (String) classAsset.getOrDefault("source_file", "");
        String className = (String) classAsset.getOrDefault("address", "");

        List<Map<String, Object>> methods = (List<Map<String, Object>>)
                classAsset.getOrDefault("methods_full", Collections.emptyList());

        for (Map<String, Object> method : methods) {
            String methodName = (String) method.getOrDefault("name", "");
            try {
                List<QualityIssue> methodIssues = checkMethod(method, filePath, className);
                issues.addAll(methodIssues);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Rule {0} failed on method {1} in class {2}: {3}",
                    new Object[]{getRuleKey(), methodName, className, e.getMessage()});
                logger.log(Level.FINE, "Stack trace", e);
            }
        }
        return issues;
    }

    /**
     * Analyze a single method AST node and return quality issues.
     *
     * @param method      method AST data (name, body_code, line_start, etc.)
     * @param filePath    source file path
     * @param className   fully qualified class name
     * @return list of quality issues found in this method
     */
    protected abstract List<QualityIssue> checkMethod(Map<String, Object> method,
                                                       String filePath, String className);

    /**
     * Create a QualityIssue using the builder pattern.
     */
    protected QualityIssue.Builder issueBuilder() {
        return new QualityIssue.Builder()
            .ruleKey(getRuleKey())
            .ruleName(getName())
            .category(getCategory());
    }
}
