package cn.dolphinmind.glossary.java.analyze.pipeline;

import cn.dolphinmind.glossary.java.analyze.rag.RagPipelineCli;
import cn.dolphinmind.glossary.java.analyze.unified.ResultMerger;
import cn.dolphinmind.glossary.java.analyze.unified.UnifiedReport;
import cn.dolphinmind.glossary.java.analyze.unified.UnifiedIssue;
import cn.dolphinmind.glossary.java.analyze.SourceUniversePro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * 分析编排器
 * 
 * 将静态分析、RAG 审查、结果合并串联为一条完整的生产线。
 * 支持断点续跑、进度报告、以及优雅取消。
 * 
 * 使用示例：
 * <pre>
 * AnalysisOrchestrator orchestrator = new AnalysisOrchestrator();
 * UnifiedReport report = orchestrator.run(
 *     "/path/to/project",   // sourceRoot
 *     "/path/to/output",    // outputDir
 *     true,                  // enable RAG
 *     false                  // incremental
 * );
 * </pre>
 */
public class AnalysisOrchestrator {

    private static final Logger logger = Logger.getLogger(AnalysisOrchestrator.class.getName());
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private volatile Listener listener = null;

    /** 监听器接口：供外部订阅流水线事件 */
    public interface Listener {
        void onStageStart(String stageName);
        void onStageComplete(String stageName, long elapsedMs);
        void onStageFailed(String stageName, String error);
        void onPipelineComplete(UnifiedReport report);
        void onPipelineFailed(String error);
    }

    public void setListener(Listener l) { this.listener = l; }

    /**
     * 运行完整分析流水线
     */
    public UnifiedReport run(String sourceRoot, String outputDir,
                             boolean enableRag, boolean incremental) throws PipelineException {
        
        PipelineContext ctx = new PipelineContext(sourceRoot, outputDir,
                extractProjectName(sourceRoot), enableRag, incremental);

        try {
            // Stage 1: 静态分析
            Map<String, Object> staticResult = runStaticAnalysis(ctx);
            
            // Stage 2: RAG 审查 (可选)
            Map<String, Object> aiResult = null;
            if (enableRag) {
                aiResult = runRagReview(ctx, staticResult);
            }

            // Stage 3: 结果合并
            UnifiedReport report = runMerge(ctx, staticResult, aiResult);

            // Stage 4: 输出报告
            writeReport(ctx, report);

            if (listener != null) listener.onPipelineComplete(report);
            return report;

        } catch (PipelineException e) {
            if (listener != null) listener.onPipelineFailed(e.getMessage());
            throw e;
        }
    }

    /**
     * Stage 1: 静态分析
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> runStaticAnalysis(PipelineContext ctx) throws PipelineException {
        String stage = "Static Analysis";
        ctx.recordStageStart(stage);
        if (listener != null) listener.onStageStart(stage);

        try {
            logger.info("Running static analysis on: " + ctx.getSourceRoot());
            
            String jsonPath = ctx.getOutputDir() + "/static-results.json";
            
            // Build args for SourceUniversePro.main()
            String[] args = new String[]{
                "--sourceRoot", ctx.getSourceRoot(),
                "--outputDir", ctx.getOutputDir(),
                "--outputFile", "static-results.json",
                "--format", "json"
            };
            SourceUniversePro.main(args);

            // Read result
            String json = new String(Files.readAllBytes(Paths.get(jsonPath)));
            Map<String, Object> result = gson.fromJson(json, Map.class);

            ctx.put("staticResultPath", jsonPath);
            ctx.put("staticResult", result);

            long elapsed = ctx.recordStageEnd(stage);
            if (listener != null) listener.onStageComplete(stage, elapsed);

            logger.info("Static analysis completed in " + elapsed + "ms");
            return result;

        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            ctx.recordStageEnd(stage);
            String msg = "Static analysis failed: " + e.getMessage();
            logger.warning(msg);
            if (listener != null) listener.onStageFailed(stage, msg);
            throw new PipelineException(stage, msg);
        }
    }

    /**
     * Stage 2: RAG 审查
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> runRagReview(PipelineContext ctx,
                                              Map<String, Object> staticResult) throws PipelineException {
        String stage = "RAG Review";
        ctx.recordStageStart(stage);
        if (listener != null) listener.onStageStart(stage);

        try {
            logger.info("Running RAG review...");

            String aiPath = ctx.getOutputDir() + "/ai-results.json";
            
            // Build args for RagPipelineCli.main()
            List<String> argList = new ArrayList<>();
            argList.add("--sourceRoot"); argList.add(ctx.getSourceRoot());
            argList.add("--analysisResult"); argList.add((String) ctx.get("staticResultPath"));
            argList.add("--output"); argList.add(aiPath);
            argList.add("--embedding-provider"); argList.add("ollama");
            argList.add("--query"); argList.add("审查代码质量和安全问题");
            
            RagPipelineCli.main(argList.toArray(new String[0]));

            // Read AI result
            if (Files.exists(Paths.get(aiPath))) {
                String json = new String(Files.readAllBytes(Paths.get(aiPath)));
                Map<String, Object> result = gson.fromJson(json, Map.class);
                ctx.put("aiResultPath", aiPath);
                ctx.put("aiResult", result);
            }

            long elapsed = ctx.recordStageEnd(stage);
            if (listener != null) listener.onStageComplete(stage, elapsed);

            logger.info("RAG review completed in " + elapsed + "ms");
            return ctx.get("aiResult");

        } catch (Exception e) {
            ctx.recordStageEnd(stage);
            String msg = "RAG review failed: " + e.getMessage();
            logger.warning(msg);
            if (listener != null) listener.onStageFailed(stage, msg);
            // RAG is optional, don't fail the whole pipeline
            return null;
        }
    }

    /**
     * Stage 3: 结果合并
     */
    private UnifiedReport runMerge(PipelineContext ctx,
                                    Map<String, Object> staticResult,
                                    Map<String, Object> aiResult) throws PipelineException {
        String stage = "Result Merge";
        ctx.recordStageStart(stage);
        if (listener != null) listener.onStageStart(stage);

        try {
            logger.info("Merging static and AI results...");

            String reportPath = ctx.getOutputDir() + "/unified-report.json";
            
            // Build args for ResultMerger.main()
            List<String> argList = new ArrayList<>();
            argList.add("--static"); argList.add((String) ctx.get("staticResultPath"));
            argList.add("--output"); argList.add(reportPath);
            argList.add("--sourceRoot"); argList.add(ctx.getSourceRoot());
            
            if (ctx.containsKey("aiResultPath")) {
                argList.add("--ai"); argList.add((String) ctx.get("aiResultPath"));
            }
            
            ResultMerger.main(argList.toArray(new String[0]));

            // Read unified report
            String json = new String(Files.readAllBytes(Paths.get(reportPath)));
            UnifiedReport report = gson.fromJson(json, UnifiedReport.class);
            ctx.put("unifiedReport", report);

            long elapsed = ctx.recordStageEnd(stage);
            if (listener != null) listener.onStageComplete(stage, elapsed);

            logger.info("Result merge completed in " + elapsed + "ms");
            return report;

        } catch (Exception e) {
            ctx.recordStageEnd(stage);
            String msg = "Result merge failed: " + e.getMessage();
            logger.warning(msg);
            if (listener != null) listener.onStageFailed(stage, msg);
            throw new PipelineException(stage, msg);
        }
    }

    /**
     * Stage 4: 输出报告
     */
    private void writeReport(PipelineContext ctx, UnifiedReport report) throws PipelineException {
        String stage = "Report Output";
        ctx.recordStageStart(stage);
        if (listener != null) listener.onStageStart(stage);

        try {
            String outputPath = ctx.getOutputDir() + "/unified-report.json";
            String json = gson.toJson(report.toMap());
            Files.write(Paths.get(outputPath), json.getBytes());
            ctx.put("reportPath", outputPath);

            long elapsed = ctx.recordStageEnd(stage);
            if (listener != null) listener.onStageComplete(stage, elapsed);

            logger.info("Report written to: " + outputPath);

        } catch (Exception e) {
            String msg = "Failed to write report: " + e.getMessage();
            logger.warning(msg);
            throw new PipelineException(stage, msg);
        }
    }

    private String extractProjectName(String sourceRoot) {
        if (sourceRoot == null) return "unknown";
        Path p = Paths.get(sourceRoot);
        return p.getFileName() != null ? p.getFileName().toString() : "unknown";
    }
}
