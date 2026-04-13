package cn.dolphinmind.glossary.java.analyze.dto;

import java.util.List;
import java.util.Map;

/**
 * 分析结果 DTO
 * 
 * 分析任务完成后的出参，包含摘要和详情列表。
 */
public class AnalysisResultDTO {

    private String taskId;
    private String projectName;
    private long analysisTimeMs;
    private Map<String, Integer> summary; // { "critical": 1, "major": 5, ... }
    private List<IssueDTO> issues;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public long getAnalysisTimeMs() { return analysisTimeMs; }
    public void setAnalysisTimeMs(long analysisTimeMs) { this.analysisTimeMs = analysisTimeMs; }
    public Map<String, Integer> getSummary() { return summary; }
    public void setSummary(Map<String, Integer> summary) { this.summary = summary; }
    public List<IssueDTO> getIssues() { return issues; }
    public void setIssues(List<IssueDTO> issues) { this.issues = issues; }
}
