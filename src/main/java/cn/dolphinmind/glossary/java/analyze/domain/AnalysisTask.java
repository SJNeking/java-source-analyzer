package cn.dolphinmind.glossary.java.analyze.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 分析任务实体 (映射 analysis_results 表)
 */
public class AnalysisTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String projectName;
    private String source;           // 'static' | 'ai' | 'unified'
    private String rawJsonUrl;       // 报告在 MinIO 中的链接
    private Long analysisMs;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getRawJsonUrl() { return rawJsonUrl; }
    public void setRawJsonUrl(String rawJsonUrl) { this.rawJsonUrl = rawJsonUrl; }
    public Long getAnalysisMs() { return analysisMs; }
    public void setAnalysisMs(Long analysisMs) { this.analysisMs = analysisMs; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
