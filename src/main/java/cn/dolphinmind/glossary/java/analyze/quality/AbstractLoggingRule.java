package cn.dolphinmind.glossary.java.analyze.quality;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for rules with proper error handling and logging.
 *
 * Provides:
 * - Unified exception handling with logging
 * - Rule execution context for performance tracking
 * - Template method pattern for subclasses
 *
 * @param <T> the type of AST node this rule analyzes
 */
public abstract class AbstractLoggingRule<T> implements QualityRule {

    protected final Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public java.util.List<QualityIssue> check(java.util.Map<String, Object> classAsset) {
        java.util.List<QualityIssue> issues = new java.util.ArrayList<>();
        String className = (String) classAsset.getOrDefault("address", "");

        try {
            issues = doCheck(classAsset);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Rule {0} failed on class {1}: {2}",
                new Object[]{getRuleKey(), className, e.getMessage()});
            logger.log(Level.FINE, "Stack trace", e);
        }

        return issues;
    }

    /**
     * Subclasses implement this to perform actual analysis.
     * Exceptions thrown here will be caught and logged, not propagated.
     */
    protected abstract java.util.List<QualityIssue> doCheck(java.util.Map<String, Object> classAsset);

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
