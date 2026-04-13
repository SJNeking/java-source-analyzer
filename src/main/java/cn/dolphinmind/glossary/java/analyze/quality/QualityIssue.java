package cn.dolphinmind.glossary.java.analyze.quality;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a quality issue found in the source code.
 * Supports both legacy line-based location and precise AST-based range location.
 */
public class QualityIssue {

    private final String ruleKey;
    private final String ruleName;
    private final Severity severity;
    private final String category; // BUG, CODE_SMELL, SECURITY, VULNERABILITY
    private final String filePath;
    private final String className;
    private final String methodName;
    private final int line;
    private final String message;
    private final String evidence;

    // Precise location from AST analysis (optional, defaults to line-based)
    private final int startLine;
    private final int endLine;
    private final int startColumn;
    private final int endColumn;
    private final String codeSnippet;

    /**
     * Legacy constructor for backward compatibility.
     */
    public QualityIssue(String ruleKey, String ruleName, Severity severity, String category,
                        String filePath, String className, String methodName, int line,
                        String message, String evidence) {
        this(ruleKey, ruleName, severity, category, filePath, className, methodName,
             line, line, 1, 0, message, evidence, null);
    }

    /**
     * Full constructor with precise location support.
     */
    public QualityIssue(String ruleKey, String ruleName, Severity severity, String category,
                        String filePath, String className, String methodName,
                        int startLine, int endLine, int startColumn, int endColumn,
                        String message, String evidence, String codeSnippet) {
        this.ruleKey = ruleKey;
        this.ruleName = ruleName;
        this.severity = severity;
        this.category = category;
        this.filePath = filePath;
        this.className = className;
        this.methodName = methodName;
        this.line = startLine;
        this.startLine = startLine;
        this.endLine = endLine;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
        this.message = message;
        this.evidence = evidence;
        this.codeSnippet = codeSnippet;
    }

    public String getRuleKey() { return ruleKey; }
    public String getRuleName() { return ruleName; }
    public Severity getSeverity() { return severity; }
    public String getCategory() { return category; }
    public String getFilePath() { return filePath; }
    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    /** @deprecated use {@link #getStartLine()} for precise location */
    @Deprecated
    public int getLine() { return startLine; }
    public String getMessage() { return message; }
    public String getEvidence() { return evidence; }

    public int getStartLine() { return startLine; }
    public int getEndLine() { return endLine; }
    public int getStartColumn() { return startColumn; }
    public int getEndColumn() { return endColumn; }
    public String getCodeSnippet() { return codeSnippet; }

    /**
     * Check if this issue has precise AST-based location info.
     */
    public boolean hasPreciseLocation() {
        return startColumn > 0 || endColumn > 0 || codeSnippet != null;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("rule_key", ruleKey);
        map.put("rule_name", ruleName);
        map.put("severity", severity.name());
        map.put("category", category);
        map.put("file", filePath);
        map.put("class", className);
        map.put("method", methodName);
        map.put("line", startLine);
        map.put("start_line", startLine);
        map.put("end_line", endLine);
        map.put("start_column", startColumn);
        map.put("end_column", endColumn);
        map.put("message", message);
        map.put("evidence", evidence);
        if (codeSnippet != null) {
            map.put("code_snippet", codeSnippet);
        }
        return map;
    }

    /**
     * Builder for constructing QualityIssue with fluent API.
     */
    public static class Builder {
        private String ruleKey, ruleName, category, filePath, className, methodName;
        private Severity severity = Severity.MINOR;
        private int startLine = 0, endLine = 0, startColumn = 0, endColumn = 0;
        private String message = "", evidence = "", codeSnippet = null;

        public Builder ruleKey(String ruleKey) { this.ruleKey = ruleKey; return this; }
        public Builder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public Builder severity(Severity severity) { this.severity = severity; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder className(String className) { this.className = className; return this; }
        public Builder methodName(String methodName) { this.methodName = methodName; return this; }
        public Builder line(int line) { this.startLine = line; this.endLine = line; return this; }
        public Builder location(int startLine, int endLine, int startColumn, int endColumn) {
            this.startLine = startLine; this.endLine = endLine;
            this.startColumn = startColumn; this.endColumn = endColumn;
            return this;
        }
        public Builder message(String message) { this.message = message; return this; }
        public Builder evidence(String evidence) { this.evidence = evidence; return this; }
        public Builder codeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; return this; }

        public QualityIssue build() {
            return new QualityIssue(ruleKey, ruleName, severity, category, filePath,
                className, methodName, startLine, endLine, startColumn, endColumn,
                message, evidence, codeSnippet);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QualityIssue that = (QualityIssue) o;
        return ruleKey.equals(that.ruleKey) &&
               filePath.equals(that.filePath) &&
               startLine == that.startLine &&
               (className != null ? className.equals(that.className) : that.className == null) &&
               (methodName != null ? methodName.equals(that.methodName) : that.methodName == null);
    }

    @Override
    public int hashCode() {
        int result = ruleKey.hashCode();
        result = 31 * result + filePath.hashCode();
        result = 31 * result + startLine;
        result = 31 * result + (className != null ? className.hashCode() : 0);
        result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s:%d - %s (%s)", ruleKey, filePath, startLine, message, severity);
    }
}
