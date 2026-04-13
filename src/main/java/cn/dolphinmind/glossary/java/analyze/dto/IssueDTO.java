package cn.dolphinmind.glossary.java.analyze.dto;

/**
 * 代码问题 DTO
 * 
 * 统一了静态分析和 AI 审查发现的问题。
 */
public class IssueDTO {

    private String id;
    private String source;      // "static" | "ai" | "merged"
    private String severity;    // "CRITICAL" | "MAJOR" | "MINOR" | "INFO"
    private String category;    // "BUG" | "CODE_SMELL" | "SECURITY"
    private String filePath;
    private String className;
    private String methodName;
    private int line;
    private String message;
    private Double confidence;
    private String aiSuggestion;
    private String aiFixedCode;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getAiSuggestion() { return aiSuggestion; }
    public void setAiSuggestion(String aiSuggestion) { this.aiSuggestion = aiSuggestion; }
    public String getAiFixedCode() { return aiFixedCode; }
    public void setAiFixedCode(String aiFixedCode) { this.aiFixedCode = aiFixedCode; }
}
