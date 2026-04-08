package cn.dolphinmind.glossary.java.analyze.quality;

/**
 * Severity level for quality issues
 */
public enum Severity {
    CRITICAL,  // Will cause runtime failure or security breach
    MAJOR,     // Significant code smell or bug risk
    MINOR,     // Minor code smell or style issue
    INFO       // Informational suggestion
}
