package cn.dolphinmind.glossary.java.analyze.event;

import java.util.Map;

/**
 * 分析事件基类
 * 
 * 用于在分析流水线的不同阶段之间传递状态和通知。
 */
public class AnalysisEvent {

    public enum Type {
        STARTED,
        STAGE_COMPLETED,
        COMPLETED,
        FAILED
    }

    private final Type type;
    private final String taskId;
    private final long timestamp;
    private final Map<String, Object> payload;
    private final String message;

    public AnalysisEvent(Type type, String taskId, Map<String, Object> payload, String message) {
        this.type = type;
        this.taskId = taskId != null ? taskId : "unknown";
        this.timestamp = System.currentTimeMillis();
        this.payload = payload;
        this.message = message;
    }

    public Type getType() { return type; }
    public String getTaskId() { return taskId; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Object> getPayload() { return payload; }
    public String getMessage() { return message; }
    
    @Override
    public String toString() {
        return String.format("[%s] Task %s: %s", type, taskId, message);
    }
}
