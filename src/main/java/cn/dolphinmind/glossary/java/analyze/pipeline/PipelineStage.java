package cn.dolphinmind.glossary.java.analyze.pipeline;

/**
 * 流水线阶段接口
 * 
 * 每个阶段负责处理输入、产出输出、并在失败时报告错误。
 */
public interface PipelineStage<I, O> {

    /**
     * @return 阶段名称（用于日志和监控）
     */
    String name();

    /**
     * 执行阶段逻辑
     * 
     * @param input  上一阶段的输出
     * @param ctx    流水线上下文
     * @return 本阶段的输出，传递给下一阶段
     * @throws PipelineException 如果阶段执行失败
     */
    O execute(I input, PipelineContext ctx) throws PipelineException;

    /**
     * @return 该阶段是否可选（跳过不影响主流程）
     */
    default boolean isOptional() {
        return false;
    }
}
