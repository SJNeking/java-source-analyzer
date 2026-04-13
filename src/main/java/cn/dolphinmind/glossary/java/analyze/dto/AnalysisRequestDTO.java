package cn.dolphinmind.glossary.java.analyze.dto;

/**
 * 分析请求 DTO
 * 
 * 客户端发起分析任务时的入参。
 */
public class AnalysisRequestDTO {

    private String sourceRoot;      // 必填：源码根目录
    private String projectName;     // 可选：项目名称
    private String branch;          // 可选：分支名
    private String commitSha;       // 可选：Commit Hash
    private boolean enableRag;      // 是否启用 AI 审查
    private String query;           // AI 审查 Prompt

    public String getSourceRoot() { return sourceRoot; }
    public void setSourceRoot(String sourceRoot) { this.sourceRoot = sourceRoot; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
    public boolean isEnableRag() { return enableRag; }
    public void setEnableRag(boolean enableRag) { this.enableRag = enableRag; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
}
