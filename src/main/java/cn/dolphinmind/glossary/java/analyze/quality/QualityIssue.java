package cn.dolphinmind.glossary.java.analyze.quality;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a quality issue found in the source code
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

    public QualityIssue(String ruleKey, String ruleName, Severity severity, String category,
                        String filePath, String className, String methodName, int line,
                        String message, String evidence) {
        this.ruleKey = ruleKey;
        this.ruleName = ruleName;
        this.severity = severity;
        this.category = category;
        this.filePath = filePath;
        this.className = className;
        this.methodName = methodName;
        this.line = line;
        this.message = message;
        this.evidence = evidence;
    }

    public String getRuleKey() { return ruleKey; }
    public String getRuleName() { return ruleName; }
    public Severity getSeverity() { return severity; }
    public String getCategory() { return category; }
    public String getFilePath() { return filePath; }
    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public int getLine() { return line; }
    public String getMessage() { return message; }
    public String getEvidence() { return evidence; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("rule_key", ruleKey);
        map.put("rule_name", ruleName);
        map.put("severity", severity.name());
        map.put("category", category);
        map.put("file", filePath);
        map.put("class", className);
        map.put("method", methodName);
        map.put("line", line);
        map.put("message", message);
        map.put("evidence", evidence);
        return map;
    }
}
