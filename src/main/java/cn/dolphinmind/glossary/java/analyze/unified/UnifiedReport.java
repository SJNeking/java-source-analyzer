package cn.dolphinmind.glossary.java.analyze.unified;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统一审查报告 — 静态分析 + AI 审查的合并结果
 * 
 * 这个报告可以直接喂给前端可视化, 也可以作为 CI/CD 的最终产物
 */
public class UnifiedReport {

    // ========== 项目元信息 ==========
    
    private String projectName;
    private String projectVersion;
    private String commitSha;
    private String branch;
    private long timestamp;
    private long analysisDurationMs;

    // ========== 引擎信息 ==========
    
    private EngineInfo staticEngine;         // 静态分析引擎信息
    private EngineInfo aiEngine;             // AI 审查引擎信息

    // ========== 问题列表 ==========
    
    private List<UnifiedIssue> issues;       // 所有问题 (静态 + AI + 合并)

    // ========== 统计摘要 ==========

    private Summary summary;

    // ========== RAG 上下文 (AST 切片) ==========

    @SuppressWarnings("unchecked")
    private Map<String, Object> ragContext;

    // ========== 构造函数 ==========

    public UnifiedReport() {
        this.issues = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    // ========== 构建报告 ==========

    /**
     * 从独立的静态分析结果和 AI 审查结果构建合并报告
     */
    public static UnifiedReport merge(
            List<UnifiedIssue> staticIssues,
            List<UnifiedIssue> aiIssues) {
        
        UnifiedReport report = new UnifiedReport();
        Map<String, UnifiedIssue> issueMap = new LinkedHashMap<>();

        // 1. 先加入所有静态分析问题
        for (UnifiedIssue si : staticIssues) {
            issueMap.put(locationKey(si), si);
        }

        // 2. 合并 AI 审查结果
        for (UnifiedIssue ai : aiIssues) {
            String key = locationKey(ai);
            UnifiedIssue existing = issueMap.get(key);

            if (existing != null) {
                // AI 补充了静态分析发现的问题 → 合并
                existing.mergeWithAiReview(
                        ai.getAiSuggestion(),
                        ai.getAiFixedCode(),
                        ai.getConfidence(),
                        ai.getAiReasoning(),
                        ai.getAiModel()
                );
                existing.setAiIssueId(ai.getId());
            } else if (ai.getConfidence() != null 
                    && ai.getConfidence() >= ConfidenceLevel.LOW.getMinScore()) {
                // AI 独立发现新问题 (且置信度足够)
                issueMap.put(key, ai);
            }
            // 低置信度的 AI 问题 → 保留但标记 autoFiltered
        }

        report.issues = new ArrayList<>(issueMap.values());
        report.rebuildSummary();
        return report;
    }

    /**
     * 仅静态分析结果 (无 AI 审查时的降级)
     */
    public static UnifiedReport fromStaticAnalysis(List<UnifiedIssue> staticIssues) {
        UnifiedReport report = new UnifiedReport();
        report.issues = new ArrayList<>(staticIssues);
        report.rebuildSummary();
        return report;
    }

    // ========== 统计摘要 ==========

    private void rebuildSummary() {
        Summary s = new Summary();

        s.totalIssues = issues.size();
        s.staticOnlyIssues = issues.stream().filter(i -> i.getSource() == IssueSource.STATIC).count();
        s.aiOnlyIssues = issues.stream().filter(i -> i.getSource() == IssueSource.AI).count();
        s.mergedIssues = issues.stream().filter(i -> i.getSource() == IssueSource.MERGED).count();
        s.autoFilteredIssues = issues.stream().filter(UnifiedIssue::isAutoFiltered).count();

        // 按严重程度统计 (排除已过滤的)
        List<UnifiedIssue> activeIssues = issues.stream()
                .filter(i -> !i.isAutoFiltered())
                .collect(Collectors.toList());

        s.activeIssues = activeIssues.size();
        s.criticalCount = activeIssues.stream().filter(i -> "CRITICAL".equals(i.getSeverity())).count();
        s.majorCount = activeIssues.stream().filter(i -> "MAJOR".equals(i.getSeverity())).count();
        s.minorCount = activeIssues.stream().filter(i -> "MINOR".equals(i.getSeverity())).count();
        s.infoCount = activeIssues.stream().filter(i -> "INFO".equals(i.getSeverity())).count();

        // AI 统计
        List<UnifiedIssue> aiIssues = issues.stream()
                .filter(i -> i.getSource() == IssueSource.AI || i.getSource() == IssueSource.MERGED)
                .collect(Collectors.toList());
        
        if (!aiIssues.isEmpty()) {
            double avgConfidence = aiIssues.stream()
                    .mapToDouble(i -> i.getConfidence() != null ? i.getConfidence() : 0)
                    .average().orElse(0);
            s.aiAverageConfidence = Math.round(avgConfidence * 100.0) / 100.0;
            
            long highConfidence = aiIssues.stream()
                    .filter(i -> i.getConfidence() != null && i.getConfidence() >= 0.7)
                    .count();
            s.aiHighConfidenceRate = aiIssues.isEmpty() ? 0 : 
                    Math.round((double) highConfidence / aiIssues.size() * 1000) / 10.0;
        }

        // 按类别统计
        s.byCategory = activeIssues.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getCategory() != null ? i.getCategory() : "OTHER",
                        Collectors.counting()
                ));

        this.summary = s;
    }

    // ========== JSON 序列化 ==========

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        
        map.put("projectName", projectName);
        map.put("projectVersion", projectVersion);
        map.put("commitSha", commitSha);
        map.put("branch", branch);
        map.put("timestamp", timestamp);
        map.put("analysisDurationMs", analysisDurationMs);
        
        if (staticEngine != null) map.put("staticEngine", staticEngine.toMap());
        if (aiEngine != null) map.put("aiEngine", aiEngine.toMap());
        
        if (summary != null) map.put("summary", summary.toMap());
        if (ragContext != null) map.put("ragContext", ragContext);
        
        map.put("issues", issues.stream()
                .map(UnifiedIssue::toMap)
                .collect(Collectors.toList()));
        
        return map;
    }

    // ========== 内部类 ==========

    /**
     * 引擎信息
     */
    public static class EngineInfo {
        private String name;
        private String version;
        private String modelUsed;   // AI 引擎特有

        public EngineInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("version", version);
            if (modelUsed != null) m.put("modelUsed", modelUsed);
            return m;
        }

        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getModelUsed() { return modelUsed; }
        public void setModelUsed(String model) { this.modelUsed = model; }
    }

    /**
     * 统计摘要
     */
    public static class Summary {
        long totalIssues;
        long staticOnlyIssues;
        long aiOnlyIssues;
        long mergedIssues;
        long autoFilteredIssues;
        long activeIssues;
        long criticalCount;
        long majorCount;
        long minorCount;
        long infoCount;
        double aiAverageConfidence;
        double aiHighConfidenceRate;
        Map<String, Long> byCategory = new LinkedHashMap<>();

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("totalIssues", totalIssues);
            m.put("staticOnly", staticOnlyIssues);
            m.put("aiOnly", aiOnlyIssues);
            m.put("merged", mergedIssues);
            m.put("autoFiltered", autoFilteredIssues);
            m.put("activeIssues", activeIssues);
            m.put("critical", criticalCount);
            m.put("major", majorCount);
            m.put("minor", minorCount);
            m.put("info", infoCount);
            m.put("aiAvgConfidence", aiAverageConfidence);
            m.put("aiHighConfidenceRate", aiHighConfidenceRate);
            m.put("byCategory", byCategory);
            return m;
        }
    }

    // ========== 工具方法 ==========

    /**
     * 规范化文件路径用于匹配
     */
    private static String normalizePath(String filePath) {
        if (filePath == null) return "";
        int idx = filePath.indexOf("/src/main/java/");
        if (idx > 0) return filePath.substring(idx + 1);
        return filePath;
    }

    private static String locationKey(UnifiedIssue issue) {
        return normalizePath(issue.getFilePath()) + ":" + issue.getLine()
                + (issue.getMethodName() != null ? ":" + issue.getMethodName() : "");
    }

    // ========== Getters ==========

    public String getProjectName() { return projectName; }
    public void setProjectName(String name) { this.projectName = name; }
    
    public String getProjectVersion() { return projectVersion; }
    public void setProjectVersion(String v) { this.projectVersion = v; }
    
    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String sha) { this.commitSha = sha; }
    
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    
    public long getTimestamp() { return timestamp; }
    public long getAnalysisDurationMs() { return analysisDurationMs; }
    public void setAnalysisDurationMs(long ms) { this.analysisDurationMs = ms; }
    
    public EngineInfo getStaticEngine() { return staticEngine; }
    public void setStaticEngine(EngineInfo e) { this.staticEngine = e; }
    
    public EngineInfo getAiEngine() { return aiEngine; }
    public void setAiEngine(EngineInfo e) { this.aiEngine = e; }
    
    public List<UnifiedIssue> getIssues() { return Collections.unmodifiableList(issues); }
    public void setIssues(List<UnifiedIssue> issues) { this.issues = issues; }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getRagContext() { return ragContext; }
    @SuppressWarnings("unchecked")
    public void setRagContext(Map<String, Object> ctx) { this.ragContext = ctx; }

    public Summary getSummary() { return summary; }
}
