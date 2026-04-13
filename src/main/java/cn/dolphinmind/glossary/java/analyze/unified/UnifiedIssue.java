package cn.dolphinmind.glossary.java.analyze.unified;

import java.util.*;

/**
 * 统一问题模型 — 静态分析 + AI 审查共同使用
 * 
 * 设计原则:
 * 1. 向后兼容现有 QualityIssue (可通过 fromQualityIssue 转换)
 * 2. AI 特有字段用 Optional 包装 (static 分析时为空)
 * 3. 静态分析特有字段同理 (AI 分析时为空)
 * 4. 合并后两个来源的字段同时存在
 */
public class UnifiedIssue {

    // ========== 基础字段 (两者共有) ==========
    
    private String id;                    // 唯一标识 (UUID)
    private IssueSource source;           // 问题来源
    private String ruleKey;               // 规则ID (SonarLint 风格)
    private String ruleName;              // 规则名称
    private String severity;              // CRITICAL | MAJOR | MINOR | INFO
    private String category;              // BUG | CODE_SMELL | SECURITY | DESIGN | PERFORMANCE
    
    private String filePath;              // 文件路径 (相对于项目根)
    private String className;             // 类名 (全限定名)
    private String methodName;            // 方法名 (可为空)
    private int line;                     // 行号
    private String message;               // 问题描述
    
    // ========== AI 特有字段 ==========
    
    private Double confidence;            // 0.0-1.0 置信度 (静态分析时固定为 1.0)
    private String aiSuggestion;          // AI 修复建议 (Markdown)
    private String aiFixedCode;           // AI 修复后的完整代码
    private String aiReasoning;           // AI 推理过程 (调试用, 不展示给最终用户)
    private String aiModel;               // 使用的模型 (gpt-4 / qwen / deepseek)
    private boolean autoFiltered;         // 低置信度自动过滤
    
    // ========== 静态分析特有字段 ==========
    
    private Integer cyclomaticComplexity; // 圈复杂度
    private Integer loc;                  // 代码行数
    private Integer cognitiveComplexity;  // 认知复杂度
    private List<String> relatedAssets;   // 关联的类/方法地址
    private String staticEngineVersion;   // 静态分析引擎版本
    
    // ========== 元数据 ==========
    
    private String staticIssueId;         // 关联的静态分析ID (AI补充静态问题时)
    private String aiIssueId;             // 关联的AI审查ID (静态问题被AI补充时)
    private int retryCount;               // 重试次数 (Harness Engineering)
    private Date createdAt;
    private Date updatedAt;

    // ========== Constructors ==========

    public UnifiedIssue() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // ========== Builder ==========

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final UnifiedIssue issue = new UnifiedIssue();

        public Builder source(IssueSource source) { issue.source = source; return this; }
        public Builder ruleKey(String ruleKey) { issue.ruleKey = ruleKey; return this; }
        public Builder ruleName(String ruleName) { issue.ruleName = ruleName; return this; }
        public Builder severity(String severity) { issue.severity = severity; return this; }
        public Builder category(String category) { issue.category = category; return this; }
        public Builder filePath(String filePath) { issue.filePath = filePath; return this; }
        public Builder className(String className) { issue.className = className; return this; }
        public Builder methodName(String methodName) { issue.methodName = methodName; return this; }
        public Builder line(int line) { issue.line = line; return this; }
        public Builder message(String message) { issue.message = message; return this; }
        
        public Builder confidence(Double confidence) { issue.confidence = confidence; return this; }
        public Builder aiSuggestion(String suggestion) { issue.aiSuggestion = suggestion; return this; }
        public Builder aiFixedCode(String code) { issue.aiFixedCode = code; return this; }
        public Builder aiReasoning(String reasoning) { issue.aiReasoning = reasoning; return this; }
        public Builder aiModel(String model) { issue.aiModel = model; return this; }
        public Builder autoFiltered(boolean filtered) { issue.autoFiltered = filtered; return this; }
        
        public Builder cyclomaticComplexity(Integer cc) { issue.cyclomaticComplexity = cc; return this; }
        public Builder loc(Integer loc) { issue.loc = loc; return this; }
        public Builder cognitiveComplexity(Integer cc) { issue.cognitiveComplexity = cc; return this; }
        public Builder relatedAssets(List<String> assets) { issue.relatedAssets = assets; return this; }
        
        public Builder staticIssueId(String id) { issue.staticIssueId = id; return this; }
        public Builder aiIssueId(String id) { issue.aiIssueId = id; return this; }

        public UnifiedIssue build() {
            issue.updatedAt = new Date();
            return issue;
        }
    }

    // ========== Conversion: 从现有 QualityIssue 转换 ==========

    /**
     * 将现有 QualityIssue 转为 UnifiedIssue
     * 用于渐进式迁移, 不破坏现有代码
     */
    public static UnifiedIssue fromQualityIssue(cn.dolphinmind.glossary.java.analyze.quality.QualityIssue qi) {
        return builder()
                .source(IssueSource.STATIC)
                .ruleKey(qi.getRuleKey())
                .ruleName(qi.getRuleName())
                .severity(qi.getSeverity().name())
                .category(qi.getCategory())
                .filePath(qi.getFilePath())
                .className(qi.getClassName())
                .methodName(qi.getMethodName())
                .line(qi.getLine())
                .message(qi.getMessage())
                .confidence(1.0)              // 静态分析 = 100% 确定
                .build();
    }

    // ========== Merge: 静态 + AI ==========

    /**
     * 将 AI 审查结果合并到已有的静态分析问题
     */
    public UnifiedIssue mergeWithAiReview(
            String aiSuggestion, String aiFixedCode,
            Double confidence, String aiReasoning, String aiModel) {
        
        this.source = IssueSource.MERGED;
        this.aiSuggestion = aiSuggestion;
        this.aiFixedCode = aiFixedCode;
        this.confidence = confidence;
        this.aiReasoning = aiReasoning;
        this.aiModel = aiModel;
        this.autoFiltered = ConfidenceLevel.fromScore(confidence).isAutoAccept() == false 
                         && confidence < ConfidenceLevel.LOW.getMinScore();
        this.updatedAt = new Date();
        return this;
    }

    // ========== JSON Serialization ==========

    /**
     * 转为前端可读的 Map (用于 JSON 序列化)
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        
        // 基础字段
        map.put("id", id);
        map.put("source", source != null ? source.getCode() : null);
        map.put("sourceLabel", source != null ? source.getLabel() : null);
        map.put("ruleKey", ruleKey);
        map.put("ruleName", ruleName);
        map.put("severity", severity);
        map.put("category", category);
        map.put("filePath", filePath);
        map.put("className", className);
        map.put("methodName", methodName);
        map.put("line", line);
        map.put("message", message);
        
        // AI 字段 (仅当存在时输出)
        if (confidence != null) map.put("confidence", confidence);
        if (confidence != null) map.put("confidenceLevel", ConfidenceLevel.fromScore(confidence).name());
        if (aiSuggestion != null) map.put("aiSuggestion", aiSuggestion);
        if (aiFixedCode != null) map.put("aiFixedCode", aiFixedCode);
        if (aiReasoning != null) map.put("aiReasoning", aiReasoning);
        if (aiModel != null) map.put("aiModel", aiModel);
        if (autoFiltered) map.put("autoFiltered", true);
        
        // 静态分析字段 (仅当存在时输出)
        if (cyclomaticComplexity != null) map.put("cyclomaticComplexity", cyclomaticComplexity);
        if (loc != null) map.put("loc", loc);
        if (cognitiveComplexity != null) map.put("cognitiveComplexity", cognitiveComplexity);
        if (relatedAssets != null && !relatedAssets.isEmpty()) map.put("relatedAssets", relatedAssets);
        if (staticEngineVersion != null) map.put("staticEngineVersion", staticEngineVersion);
        
        // 关联ID
        if (staticIssueId != null) map.put("staticIssueId", staticIssueId);
        if (aiIssueId != null) map.put("aiIssueId", aiIssueId);
        
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        
        return map;
    }

    // ========== Getters & Setters ==========

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public IssueSource getSource() { return source; }
    public void setSource(IssueSource source) { this.source = source; }
    
    public String getRuleKey() { return ruleKey; }
    public void setRuleKey(String ruleKey) { this.ruleKey = ruleKey; }
    
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    
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
    
    public String getAiReasoning() { return aiReasoning; }
    public void setAiReasoning(String aiReasoning) { this.aiReasoning = aiReasoning; }
    
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    
    public boolean isAutoFiltered() { return autoFiltered; }
    public void setAutoFiltered(boolean autoFiltered) { this.autoFiltered = autoFiltered; }
    
    public Integer getCyclomaticComplexity() { return cyclomaticComplexity; }
    public void setCyclomaticComplexity(Integer cc) { this.cyclomaticComplexity = cc; }
    
    public Integer getLoc() { return loc; }
    public void setLoc(Integer loc) { this.loc = loc; }
    
    public Integer getCognitiveComplexity() { return cognitiveComplexity; }
    public void setCognitiveComplexity(Integer cc) { this.cognitiveComplexity = cc; }
    
    public List<String> getRelatedAssets() { return relatedAssets; }
    public void setRelatedAssets(List<String> assets) { this.relatedAssets = assets; }
    
    public String getStaticEngineVersion() { return staticEngineVersion; }
    public void setStaticEngineVersion(String v) { this.staticEngineVersion = v; }
    
    public String getStaticIssueId() { return staticIssueId; }
    public void setStaticIssueId(String id) { this.staticIssueId = id; }
    
    public String getAiIssueId() { return aiIssueId; }
    public void setAiIssueId(String id) { this.aiIssueId = id; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int count) { this.retryCount = count; }
    public void incrementRetryCount() { this.retryCount++; }
    
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
}
