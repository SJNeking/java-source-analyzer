package cn.dolphinmind.glossary.java.analyze.core;

/**
 * Unified interface for all analysis modules.
 *
 * Implementations analyze a project or dataset and produce typed results.
 * The generic type T allows each module to return its natural result type,
 * while toMap() ensures backward compatibility with existing JSON output.
 *
 * Usage:
 *   AnalyzerModule<T> analyzer = new SpringAnalyzer();
 *   T result = analyzer.analyze(context);
 *   Map<String, Object> map = analyzer.toMap(result); // for JSON output
 *
 * @param <T> the natural result type of this analyzer
 */
public interface AnalyzerModule<T> {

    /**
     * Unique identifier for this analyzer module.
     * Used for configuration, logging, and selective enable/disable.
     */
    String getId();

    /**
     * Human-readable name for display and reporting.
     */
    String getName();

    /**
     * Whether this analyzer is enabled given the current configuration.
     * Default implementation always returns true.
     */
    default boolean isEnabled(AnalysisContext context) {
        return true;
    }

    /**
     * Execute the analysis and return the result.
     * @param context shared analysis context
     * @return analysis result (may be null if nothing found)
     * @throws Exception if analysis fails
     */
    T analyze(AnalysisContext context) throws Exception;

    /**
     * Convert the result to a Map for JSON serialization.
     * Default returns null — implementations should override.
     */
    default java.util.Map<String, Object> toMap(T result) {
        return null;
    }
}
