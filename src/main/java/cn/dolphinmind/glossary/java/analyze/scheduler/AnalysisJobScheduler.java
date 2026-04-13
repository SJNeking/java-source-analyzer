package cn.dolphinmind.glossary.java.analyze.scheduler;

import cn.dolphinmind.glossary.java.analyze.dto.AnalysisRequestDTO;
import cn.dolphinmind.glossary.java.analyze.event.AnalysisEvent;
import cn.dolphinmind.glossary.java.analyze.event.EventBus;
import cn.dolphinmind.glossary.java.analyze.service.AnalysisService;

import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * 分析任务调度器
 * 
 * 管理后台分析任务的执行、队列和生命周期。
 */
public class AnalysisJobScheduler {

    private static final Logger logger = Logger.getLogger(AnalysisJobScheduler.class.getName());
    
    private final ExecutorService executor;
    private final AnalysisService analysisService;
    private final Map<String, JobStatus> activeJobs = new ConcurrentHashMap<>();

    public enum JobStatus {
        QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    public AnalysisJobScheduler(AnalysisService analysisService) {
        this.analysisService = analysisService;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("analysis-worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 提交分析任务到后台执行
     */
    public String submit(AnalysisRequestDTO request) {
        String taskId = analysisService.startAnalysis(request);
        activeJobs.put(taskId, JobStatus.RUNNING);
        
        logger.info("Job submitted: " + taskId);
        EventBus.getInstance().publish(new AnalysisEvent(
            AnalysisEvent.Type.STARTED, taskId, null, "Job queued"));
            
        return taskId;
    }

    /**
     * 取消正在运行的任务
     */
    public boolean cancel(String taskId) {
        if (activeJobs.containsKey(taskId)) {
            activeJobs.put(taskId, JobStatus.CANCELLED);
            logger.info("Job cancelled: " + taskId);
            return true;
        }
        return false;
    }

    /**
     * 查询任务状态
     */
    public JobStatus getStatus(String taskId) {
        return activeJobs.getOrDefault(taskId, null);
    }

    /**
     * 获取所有活跃任务
     */
    public Map<String, JobStatus> getActiveJobs() {
        return Map.copyOf(activeJobs);
    }
}
