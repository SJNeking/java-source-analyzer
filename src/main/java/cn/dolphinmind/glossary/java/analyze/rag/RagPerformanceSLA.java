package cn.dolphinmind.glossary.java.analyze.rag;

/**
 * RAG Pipeline Performance SLA
 * 
 * 定义RAG管道的性能基线和降级策略
 */
public class RagPerformanceSLA {
    
    // ========== 性能基线 ==========
    
    /**
     * Embedding生成超时 (单条文本)
     * Ollama本地模型: 5秒
     * OpenAI API: 3秒
     */
    public static final long EMBEDDING_TIMEOUT_MS = 5000;
    
    /**
     * 向量检索超时
     */
    public static final long VECTOR_SEARCH_TIMEOUT_MS = 2000;
    
    /**
     * LLM推理超时 (根据模型大小调整)
     * 7B模型: 30秒
     * 14B模型: 60秒
     * 32B模型: 120秒
     * 70B+模型: 300秒
     */
    public static final long LLM_INFERENCE_TIMEOUT_MS = 120000;  // 32B默认
    
    /**
     * 完整RAG审查流程超时
     */
    public static final long FULL_PIPELINE_TIMEOUT_MS = 300000;  // 5分钟
    
    // ========== 降级策略 ==========
    
    /**
     * LLM超时后的重试次数
     */
    public static final int LLM_RETRY_COUNT = 2;
    
    /**
     * LLM失败后的降级行为
     * - RETURN_EMPTY: 返回空结果
     * - USE_STATIC_ONLY: 仅使用静态分析结果
     * - REDUCE_CONTEXT: 减少上下文后重试
     */
    public enum DegradationStrategy {
        RETURN_EMPTY,
        USE_STATIC_ONLY,
        REDUCE_CONTEXT
    }
    
    public static final DegradationStrategy DEFAULT_DEGRADATION = DegradationStrategy.USE_STATIC_ONLY;
    
    // ========== Token限制 ==========
    
    /**
     * 单次审查最大Token数 (避免OOM)
     */
    public static final int MAX_TOKENS_PER_REVIEW = 50000;
    
    /**
     * 单个切片最大Token数
     */
    public static final int MAX_TOKENS_PER_SLICE = 5000;
    
    /**
     * 超过Token限制时的处理
     * - TRUNCATE: 截断代码
     * - SPLIT: 拆分为多个切片
     * - SKIP: 跳过该问题
     */
    public enum TokenOverflowStrategy {
        TRUNCATE,
        SPLIT,
        SKIP
    }
    
    public static final TokenOverflowStrategy DEFAULT_OVERFLOW_STRATEGY = TokenOverflowStrategy.TRUNCATE;
    
    // ========== 置信度阈值 ==========
    
    /**
     * 高置信度阈值 (直接接受)
     */
    public static final double HIGH_CONFIDENCE = 0.8;
    
    /**
     * 低置信度阈值 (需要人工确认)
     */
    public static final double LOW_CONFIDENCE = 0.5;
    
    /**
     * 自动过滤阈值 (低于此值的问题不展示)
     */
    public static final double AUTO_FILTER_THRESHOLD = 0.3;
}
