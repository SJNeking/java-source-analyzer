package cn.dolphinmind.glossary.java.analyze.pipeline;

/**
 * 流水线异常
 * 携带阶段名称和上下文信息，方便定位问题。
 */
public class PipelineException extends Exception {

    private final String stageName;
    private final PipelineContext context;

    public PipelineException(String message) {
        this(message, null, null);
    }

    public PipelineException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public PipelineException(String stageName, String message) {
        super("[" + stageName + "] " + message);
        this.stageName = stageName;
        this.context = null;
    }

    public PipelineException(String message, Throwable cause, PipelineContext ctx) {
        super(message, cause);
        this.stageName = null;
        this.context = ctx;
    }

    public String getStageName() { return stageName; }
    public PipelineContext getContext() { return context; }
}
