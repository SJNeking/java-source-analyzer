package cn.dolphinmind.glossary.java.analyze.api;

import cn.dolphinmind.glossary.java.analyze.dto.AnalysisRequestDTO;
import cn.dolphinmind.glossary.java.analyze.dto.AnalysisResultDTO;
import cn.dolphinmind.glossary.java.analyze.dto.IssueDTO;
import cn.dolphinmind.glossary.java.analyze.scheduler.AnalysisJobScheduler;
import cn.dolphinmind.glossary.java.analyze.scheduler.AnalysisJobScheduler.JobStatus;
import cn.dolphinmind.glossary.java.analyze.service.AnalysisService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分析控制器
 * 
 * 对外暴露分析任务管理接口（模拟 HTTP API 层）。
 */
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AnalysisJobScheduler scheduler;

    public AnalysisController() {
        this.analysisService = new AnalysisService();
        this.scheduler = new AnalysisJobScheduler(analysisService);
    }

    /**
     * 提交分析任务
     */
    public ApiResponse<String> submitAnalysis(AnalysisRequestDTO request) {
        try {
            String taskId = scheduler.submit(request);
            return ApiResponse.ok("Analysis started", taskId);
        } catch (Exception e) {
            return ApiResponse.error("Failed to start analysis: " + e.getMessage());
        }
    }

    /**
     * 查询任务状态
     */
    public ApiResponse<String> getTaskStatus(String taskId) {
        JobStatus status = scheduler.getStatus(taskId);
        if (status == null) {
            return ApiResponse.error("Task not found: " + taskId, "404");
        }
        return ApiResponse.ok("Task status", status.name());
    }

    /**
     * 获取所有活跃任务
     */
    public ApiResponse<List<Map<String, String>>> getActiveTasks() {
        Map<String, JobStatus> jobs = scheduler.getActiveJobs();
        List<Map<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, JobStatus> entry : jobs.entrySet()) {
            Map<String, String> item = new java.util.HashMap<>();
            item.put("taskId", entry.getKey());
            item.put("status", entry.getValue().name());
            result.add(item);
        }
        return ApiResponse.ok("Active tasks", result);
    }

    /**
     * 取消任务
     */
    public ApiResponse<Void> cancelTask(String taskId) {
        boolean cancelled = scheduler.cancel(taskId);
        if (cancelled) {
            return ApiResponse.ok("Task cancelled", null);
        }
        return ApiResponse.error("Task not found or already completed", "404");
    }
}
