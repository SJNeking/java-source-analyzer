package cn.dolphinmind.glossary.java.analyze.realtime;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Progress event emitted during analysis for real-time streaming.
 */
public class AnalysisProgressEvent {
    public enum EventType {
        SCAN_START,       // Starting file scan
        FILE_SCANNED,     // A file has been scanned
        FILE_SKIPPED,     // A file was skipped (cached)
        MODULE_COMPLETE,  // A module is done scanning
        QUALITY_START,    // Starting quality analysis
        QUALITY_COMPLETE, // Quality analysis done
        RELATIONS_START,  // Starting relation discovery
        RELATIONS_COMPLETE,
        ASSETS_START,     // Starting non-Java asset scan
        ASSETS_COMPLETE,
        ANALYSIS_COMPLETE,// Full analysis done
        ERROR           // Error occurred
    }

    private final EventType type;
    private final String message;
    private final int progress; // 0-100
    private final Map<String, Object> data;
    private final long timestamp;

    public AnalysisProgressEvent(EventType type, String message, int progress, Map<String, Object> data) {
        this.type = type;
        this.message = message;
        this.progress = progress;
        this.data = data != null ? data : new LinkedHashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    public AnalysisProgressEvent(EventType type, String message, int progress) {
        this(type, message, progress, null);
    }

    // Factory methods
    public static AnalysisProgressEvent scanStart(int totalFiles) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalFiles", totalFiles);
        return new AnalysisProgressEvent(EventType.SCAN_START, "开始扫描 " + totalFiles + " 个文件", 0, data);
    }

    public static AnalysisProgressEvent fileScanned(String filePath, int scanned, int total) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("filePath", filePath);
        data.put("scanned", scanned);
        data.put("total", total);
        int progress = total > 0 ? (int) ((scanned * 100.0) / total) : 0;
        return new AnalysisProgressEvent(EventType.FILE_SCANNED, "已扫描: " + filePath, progress, data);
    }

    public static AnalysisProgressEvent fileSkipped(String filePath, int skipped) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("filePath", filePath);
        data.put("skipped", skipped);
        return new AnalysisProgressEvent(EventType.FILE_SKIPPED, "跳过缓存: " + filePath, 0, data);
    }

    public static AnalysisProgressEvent moduleComplete(String moduleName, int classCount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("module", moduleName);
        data.put("classCount", classCount);
        return new AnalysisProgressEvent(EventType.MODULE_COMPLETE, "模块完成: " + moduleName, 0, data);
    }

    public static AnalysisProgressEvent qualityStart() {
        return new AnalysisProgressEvent(EventType.QUALITY_START, "开始质量分析", 60);
    }

    public static AnalysisProgressEvent qualityComplete(int issueCount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("issueCount", issueCount);
        return new AnalysisProgressEvent(EventType.QUALITY_COMPLETE, "质量分析完成: " + issueCount + " 个问题", 70, data);
    }

    public static AnalysisProgressEvent relationsStart() {
        return new AnalysisProgressEvent(EventType.RELATIONS_START, "开始跨文件关系分析", 75);
    }

    public static AnalysisProgressEvent relationsComplete(int relationCount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("relationCount", relationCount);
        return new AnalysisProgressEvent(EventType.RELATIONS_COMPLETE, "关系分析完成: " + relationCount + " 条关系", 85, data);
    }

    public static AnalysisProgressEvent assetsStart() {
        return new AnalysisProgressEvent(EventType.ASSETS_START, "开始扫描非Java文件", 50);
    }

    public static AnalysisProgressEvent assetsComplete() {
        return new AnalysisProgressEvent(EventType.ASSETS_COMPLETE, "非Java文件扫描完成", 55);
    }

    public static AnalysisProgressEvent analysisComplete(int totalClasses, int totalFiles) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalClasses", totalClasses);
        data.put("totalFiles", totalFiles);
        return new AnalysisProgressEvent(EventType.ANALYSIS_COMPLETE, "分析完成! " + totalClasses + " 个类", 100, data);
    }

    public static AnalysisProgressEvent error(String message) {
        return new AnalysisProgressEvent(EventType.ERROR, "错误: " + message, 0);
    }

    // Getters
    public EventType getType() { return type; }
    public String getMessage() { return message; }
    public int getProgress() { return progress; }
    public Map<String, Object> getData() { return data; }
    public long getTimestamp() { return timestamp; }

    /**
     * Convert to JSON-compatible Map for WebSocket transmission.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type.name());
        map.put("message", message);
        map.put("progress", progress);
        map.put("data", data);
        map.put("timestamp", timestamp);
        return map;
    }
}
