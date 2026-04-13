package cn.dolphinmind.glossary.java.analyze.pipeline;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 流水线上下文
 * 
 * 在流水线各阶段之间共享数据、配置和指标。
 * 线程安全，支持并行阶段。
 */
public class PipelineContext {

    private static final Logger logger = Logger.getLogger(PipelineContext.class.getName());

    private final Map<String, Object> sharedData = new ConcurrentHashMap<>();
    private final Map<String, Long> stageTimings = new ConcurrentHashMap<>();
    private final Map<String, String> stageResults = new ConcurrentHashMap<>();
    private volatile boolean cancelled = false;

    // ========== 配置 (immutable after construction) ==========
    private final String sourceRoot;
    private final String outputDir;
    private final String projectName;
    private final boolean enableRag;
    private final boolean incremental;

    public PipelineContext(String sourceRoot, String outputDir, String projectName,
                           boolean enableRag, boolean incremental) {
        this.sourceRoot = Objects.requireNonNull(sourceRoot, "sourceRoot required");
        this.outputDir = outputDir != null ? outputDir : ".";
        this.projectName = projectName != null ? projectName : "unknown";
        this.enableRag = enableRag;
        this.incremental = incremental;
    }

    // ========== 配置访问 ==========

    public String getSourceRoot() { return sourceRoot; }
    public String getOutputDir() { return outputDir; }
    public String getProjectName() { return projectName; }
    public boolean isEnableRag() { return enableRag; }
    public boolean isIncremental() { return incremental; }

    // ========== 共享数据 ==========

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) sharedData.get(key);
    }

    public <T> T getOrDefault(String key, T defaultValue) {
        @SuppressWarnings("unchecked")
        T val = (T) sharedData.getOrDefault(key, defaultValue);
        return val;
    }

    public void put(String key, Object value) {
        sharedData.put(key, value);
    }

    public boolean containsKey(String key) {
        return sharedData.containsKey(key);
    }

    // ========== 阶段指标 ==========

    public void recordStageStart(String stageName) {
        stageTimings.put(stageName, System.currentTimeMillis());
    }

    public long recordStageEnd(String stageName) {
        Long start = stageTimings.get(stageName);
        if (start == null) return 0;
        long elapsed = System.currentTimeMillis() - start;
        stageResults.put(stageName + ".elapsedMs", String.valueOf(elapsed));
        stageTimings.remove(stageName);
        return elapsed;
    }

    public Map<String, String> getMetrics() {
        return Collections.unmodifiableMap(stageResults);
    }

    // ========== 取消控制 ==========

    public void cancel() { cancelled = true; }
    public boolean isCancelled() { return cancelled; }

    /**
     * 检查取消状态，如果已取消则抛出异常以中断流水线
     */
    public void checkCancelled() throws PipelineException {
        if (cancelled) {
            throw new PipelineException("Pipeline cancelled by user");
        }
    }
}
