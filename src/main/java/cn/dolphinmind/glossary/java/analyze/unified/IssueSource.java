package cn.dolphinmind.glossary.java.analyze.unified;

/**
 * 问题来源枚举
 * 
 * STATIC: 仅静态分析发现 (Java Source Analyzer)
 * AI:     仅 AI 审查发现 (CodeGuardian AI)
 * MERGED: 两者都发现并已合并
 */
public enum IssueSource {
    
    /** 静态规则引擎发现 (确定性强) */
    STATIC("static", "静态分析"),
    
    /** AI 语义审查发现 (灵活性强) */
    AI("ai", "AI审查"),
    
    /** 静态 + AI 双重确认 (最高置信度) */
    MERGED("merged", "双重确认");
    
    private final String code;
    private final String label;
    
    IssueSource(String code, String label) {
        this.code = code;
        this.label = label;
    }
    
    public String getCode() { return code; }
    public String getLabel() { return label; }
    
    @Override
    public String toString() { return code; }
}
