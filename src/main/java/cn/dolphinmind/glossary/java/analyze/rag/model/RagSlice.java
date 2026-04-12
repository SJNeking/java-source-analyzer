package cn.dolphinmind.glossary.java.analyze.rag.model;

import java.util.*;

/**
 * 代码切片 — RAG 检索和生成的基本单元
 * 
 * 从 AST 精准提取, 用于 Embedding 和向量化
 */
public class RagSlice {

    private String id;
    private String filePath;
    private String className;
    private String methodName;
    private int startLine;
    private int endLine;
    private String code;
    private int tokenCount;
    private SliceType type;
    private Map<String, Object> metadata;  // issueRule, issueSeverity, etc.
    private float[] embedding;             // 向量 (延迟计算)
    private Double bm25Score;              // BM25 分数 (延迟计算)
    private Double hybridScore;            // RRF 混合分数 (延迟计算)

    public enum SliceType {
        CLASS, METHOD, ISSUE_AREA
    }

    public RagSlice() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.metadata = new LinkedHashMap<>();
    }

    // ========== 用于检索排名的方法 ==========

    /**
     * 计算文本表示 (用于 BM25 和 Embedding)
     */
    public String toSearchableText() {
        StringBuilder sb = new StringBuilder();
        if (className != null) sb.append(className).append(" ");
        if (methodName != null) sb.append(methodName).append(" ");
        if (code != null) sb.append(code);
        if (metadata.containsKey("issueMessage")) {
            sb.append(" ").append(metadata.get("issueMessage"));
        }
        return sb.toString();
    }

    /**
     * 计算 BM25 分数
     */
    public double computeBm25Score(String query, Map<String, Integer> queryTerms) {
        if (queryTerms.isEmpty()) return 0;
        String text = toSearchableText().toLowerCase();
        double score = 0;
        for (Map.Entry<String, Integer> entry : queryTerms.entrySet()) {
            String term = entry.getKey().toLowerCase();
            int tf = countOccurrences(text, term);
            if (tf > 0) {
                // 简化 BM25: score = tf * idf (假设 idf=1)
                score += tf;
            }
        }
        this.bm25Score = score;
        return score;
    }

    /**
     * 计算 RRF (Reciprocal Rank Fusion) 混合分数
     */
    public double computeHybridScore(double vectorRank, double bm25Rank, double k) {
        double rrfVector = 1.0 / (k + vectorRank);
        double rrfBm25 = 1.0 / (k + bm25Rank);
        this.hybridScore = rrfVector + rrfBm25;
        return this.hybridScore;
    }

    /**
     * 计算与查询向量的余弦相似度
     */
    public double cosineSimilarity(float[] queryVector) {
        if (this.embedding == null || queryVector == null) return 0;
        double dot = 0, normA = 0, normB = 0;
        int len = Math.min(this.embedding.length, queryVector.length);
        for (int i = 0; i < len; i++) {
            dot += this.embedding[i] * queryVector[i];
            normA += this.embedding[i] * this.embedding[i];
            normB += queryVector[i] * queryVector[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ========== 序列化 ==========

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("filePath", filePath);
        m.put("className", className);
        m.put("methodName", methodName);
        m.put("startLine", startLine);
        m.put("endLine", endLine);
        m.put("code", code);
        m.put("tokenCount", tokenCount);
        m.put("type", type != null ? type.name() : null);
        if (metadata != null && !metadata.isEmpty()) m.put("metadata", metadata);
        if (bm25Score != null) m.put("bm25Score", bm25Score);
        if (hybridScore != null) m.put("hybridScore", hybridScore);
        return m;
    }

    // ========== 内部方法 ==========

    private static int countOccurrences(String text, String term) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(term, idx)) != -1) {
            count++;
            idx += term.length();
        }
        return count;
    }

    // ========== Getters & Setters ==========

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String fp) { this.filePath = fp; }
    public String getClassName() { return className; }
    public void setClassName(String cn) { this.className = cn; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String mn) { this.methodName = mn; }
    public int getStartLine() { return startLine; }
    public void setStartLine(int sl) { this.startLine = sl; }
    public int getEndLine() { return endLine; }
    public void setEndLine(int el) { this.endLine = el; }
    public String getCode() { return code; }
    public void setCode(String c) { this.code = c; }
    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tc) { this.tokenCount = tc; }
    public SliceType getType() { return type; }
    public void setType(SliceType t) { this.type = t; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> m) { this.metadata = m; }
    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] e) { this.embedding = e; }
    public Double getBm25Score() { return bm25Score; }
    public void setBm25Score(Double s) { this.bm25Score = s; }
    public Double getHybridScore() { return hybridScore; }
    public void setHybridScore(Double s) { this.hybridScore = s; }
}
