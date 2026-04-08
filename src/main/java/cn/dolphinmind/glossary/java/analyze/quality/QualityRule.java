package cn.dolphinmind.glossary.java.analyze.quality;

import java.util.List;
import java.util.Map;

/**
 * Base interface for all quality rules
 */
public interface QualityRule {

    /**
     * Unique rule key (e.g., "RSPEC-106")
     */
    String getRuleKey();

    /**
     * Human-readable rule name
     */
    String getName();

    /**
     * Category: BUG, CODE_SMELL, SECURITY
     */
    String getCategory();

    /**
     * Check the given Java class AST and return quality issues found
     */
    List<QualityIssue> check(Map<String, Object> classAsset);
}
