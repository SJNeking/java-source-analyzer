package cn.dolphinmind.glossary.java.analyze.unified;

/**
 * AI 审查置信度级别
 * 
 * 用于决定 AI 发现的问题是否需要人工确认
 */
public enum ConfidenceLevel {
    
    /** ≥ 0.9 — 高置信度，直接采纳 */
    HIGH(0.9, "高置信度", true),
    
    /** ≥ 0.7 — 中置信度，建议人工确认 */
    MEDIUM(0.7, "中置信度", false),
    
    /** ≥ 0.5 — 低置信度，建议过滤 */
    LOW(0.5, "低置信度", false),
    
    /** < 0.5 — 已自动过滤 */
    FILTERED(0.0, "已过滤", false);
    
    private final double minScore;
    private final String label;
    private final boolean autoAccept;
    
    ConfidenceLevel(double minScore, String label, boolean autoAccept) {
        this.minScore = minScore;
        this.label = label;
        this.autoAccept = autoAccept;
    }
    
    public double getMinScore() { return minScore; }
    public String getLabel() { return label; }
    public boolean isAutoAccept() { return autoAccept; }
    
    /**
     * 根据分数判断置信度级别
     */
    public static ConfidenceLevel fromScore(double score) {
        if (score >= HIGH.minScore) return HIGH;
        if (score >= MEDIUM.minScore) return MEDIUM;
        if (score >= LOW.minScore) return LOW;
        return FILTERED;
    }
    
    @Override
    public String toString() { return label; }
}
