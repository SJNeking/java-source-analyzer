package cn.dolphinmind.glossary.java.analyze.service;

import cn.dolphinmind.glossary.java.analyze.dto.AnalysisRequestDTO;
import cn.dolphinmind.glossary.java.analyze.dto.AnalysisResultDTO;
import cn.dolphinmind.glossary.java.analyze.dto.IssueDTO;
import cn.dolphinmind.glossary.java.analyze.event.AnalysisEvent;
import cn.dolphinmind.glossary.java.analyze.event.EventBus;
import cn.dolphinmind.glossary.java.analyze.exception.ApiException;
import cn.dolphinmind.glossary.java.analyze.pipeline.AnalysisOrchestrator;
import cn.dolphinmind.glossary.java.analyze.pipeline.PipelineException;
import cn.dolphinmind.glossary.java.analyze.repository.AnalysisRepository;
import cn.dolphinmind.glossary.java.analyze.repository.IssueRepository;
import cn.dolphinmind.glossary.java.analyze.repository.impl.AnalysisRepositoryImpl;
import cn.dolphinmind.glossary.java.analyze.repository.impl.IssueRepositoryImpl;
import cn.dolphinmind.glossary.java.analyze.unified.UnifiedReport;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 分析服务
 * 
 * 连接编排层 (Orchestrator) 和数据访问层 (Repository) 的核心业务逻辑。
 */
public class AnalysisService {

    private static final Logger logger = Logger.getLogger(AnalysisService.class.getName());
    
    private final AnalysisRepository analysisRepo;
    private final IssueRepository issueRepo;
    private final AnalysisOrchestrator orchestrator;
    private final EventBus eventBus;

    public AnalysisService() {
        this(new AnalysisRepositoryImpl(), new IssueRepositoryImpl(), new AnalysisOrchestrator(), EventBus.getInstance());
    }

    // For Testing
    public AnalysisService(AnalysisRepository analysisRepo, IssueRepository issueRepo, 
                           AnalysisOrchestrator orchestrator, EventBus eventBus) {
        this.analysisRepo = analysisRepo;
        this.issueRepo = issueRepo;
        this.orchestrator = orchestrator;
        this.eventBus = eventBus;
    }

    /**
     * 启动异步分析任务
     */
    public String startAnalysis(AnalysisRequestDTO request) {
        // 1. 参数校验
        validateRequest(request);

        String taskId = UUID.randomUUID().toString().substring(0, 8);
        logger.info("Starting analysis task: " + taskId);

        // 2. 发布开始事件
        eventBus.publish(new AnalysisEvent(AnalysisEvent.Type.STARTED, taskId, null, "Analysis started"));

        // 3. 在后台线程执行 (模拟异步)
        Thread worker = new Thread(() -> {
            try {
                executeAnalysis(taskId, request);
            } catch (Exception e) {
                logger.severe("Task " + taskId + " failed: " + e.getMessage());
                eventBus.publish(new AnalysisEvent(AnalysisEvent.Type.FAILED, taskId, null, e.getMessage()));
            }
        }, "analysis-worker-" + taskId);
        
        worker.setDaemon(true);
        worker.start();

        return taskId;
    }

    /**
     * 执行分析逻辑
     */
    private void executeAnalysis(String taskId, AnalysisRequestDTO request) {
        try {
            // 1. 设置监听器以捕获阶段进度
            orchestrator.setListener(new AnalysisOrchestrator.Listener() {
                @Override public void onStageStart(String stage) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("stage", stage);
                    eventBus.publish(new AnalysisEvent(AnalysisEvent.Type.STAGE_COMPLETED, taskId, p, "Stage " + stage + " started"));
                }
                @Override public void onStageComplete(String stage, long elapsedMs) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("stage", stage);
                    p.put("elapsed", elapsedMs);
                    eventBus.publish(new AnalysisEvent(AnalysisEvent.Type.STAGE_COMPLETED, taskId, p, "Stage " + stage + " completed"));
                }
                @Override public void onStageFailed(String stage, String error) {
                    eventBus.publish(new AnalysisEvent(AnalysisEvent.Type.FAILED, taskId, null, "Stage " + stage + " failed: " + error));
                }
                @Override public void onPipelineComplete(UnifiedReport report) {
                    // 保存报告
                    saveReport(taskId, request, report);
                    eventBus.publish(new AnalysisEvent(AnalysisEvent.Type.COMPLETED, taskId, null, "Analysis completed"));
                }
                @Override public void onPipelineFailed(String error) {
                    // Error is handled by the outer try-catch
                }
            });

            // 2. 运行编排器
            orchestrator.run(
                request.getSourceRoot(),
                "/tmp/codeguardian-output/" + taskId, // 临时输出目录
                request.isEnableRag(),
                false // 非增量
            );

        } catch (PipelineException e) {
            throw new ApiException(500, "Pipeline execution failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ApiException(500, "Unexpected error during analysis: " + e.getMessage(), e);
        }
    }

    /**
     * 保存报告到数据库
     */
    private void saveReport(String taskId, AnalysisRequestDTO request, UnifiedReport report) {
        logger.info("Saving report for task: " + taskId);
        
        // 1. 保存 AnalysisResult
        AnalysisResultDTO resultDTO = new AnalysisResultDTO();
        resultDTO.setTaskId(taskId);
        resultDTO.setProjectName(request.getProjectName());
        resultDTO.setAnalysisTimeMs(0); // Placeholder
        analysisRepo.save(resultDTO);

        // 2. 保存 Issues (批量)
        if (report.getIssues() != null) {
            // Convert UnifiedIssue to IssueDTO
            List<IssueDTO> issueDTOs = Collections.emptyList(); // Mapping logic omitted for brevity
            issueRepo.batchSave(taskId, issueDTOs);
        }
    }

    private void validateRequest(AnalysisRequestDTO request) {
        if (request == null || request.getSourceRoot() == null || request.getSourceRoot().isEmpty()) {
            throw new ApiException(400, "sourceRoot is required");
        }
    }
}
