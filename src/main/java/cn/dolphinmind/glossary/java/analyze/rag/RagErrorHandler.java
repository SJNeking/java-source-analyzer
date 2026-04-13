package cn.dolphinmind.glossary.java.analyze.rag;

import cn.dolphinmind.glossary.java.analyze.rag.RagPerformanceSLA.DegradationStrategy;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RAG Pipeline Error Handler
 * 
 * 统一错误处理策略:
 * 1. LLM超时/失败 → 降级到静态分析
 * 2. JSON解析失败 → 记录原始响应并返回空列表
 * 3. Embedding生成失败 → 使用零向量或跳过
 * 4. 网络异常 → 重试+指数退避
 */
public class RagErrorHandler {
    
    private static final Logger logger = Logger.getLogger(RagErrorHandler.class.getName());
    
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    
    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        LLM_TIMEOUT,
        LLM_INVALID_RESPONSE,
        EMBEDDING_FAILURE,
        NETWORK_ERROR,
        VECTOR_STORE_ERROR,
        UNKNOWN
    }
    
    /**
     * 处理RAG管道异常
     * 
     * @param errorType 错误类型
     * @param exception 原始异常
     * @param context 上下文信息
     * @return 降级策略
     */
    public static DegradationAction handleError(ErrorType errorType, Exception exception, Map<String, Object> context) {
        logger.log(Level.WARNING, "RAG pipeline error: " + errorType, exception);
        
        switch (errorType) {
            case LLM_TIMEOUT:
                return handleLlmTimeout(exception, context);
            
            case LLM_INVALID_RESPONSE:
                return handleInvalidResponse(exception, context);
            
            case EMBEDDING_FAILURE:
                return handleEmbeddingFailure(exception, context);
            
            case NETWORK_ERROR:
                return handleNetworkError(exception, context);
            
            case VECTOR_STORE_ERROR:
                return handleVectorStoreError(exception, context);
            
            default:
                return new DegradationAction(DegradationStrategy.USE_STATIC_ONLY, 
                    "Unknown error, fallback to static analysis");
        }
    }
    
    /**
     * LLM超时处理
     */
    private static DegradationAction handleLlmTimeout(Exception e, Map<String, Object> context) {
        int retryCount = getRetryCount(context);
        
        if (retryCount < RagPerformanceSLA.LLM_RETRY_COUNT) {
            // 重试: 减少上下文
            logger.info("LLM timeout, retrying with reduced context (attempt " + (retryCount + 1) + ")");
            return new DegradationAction(DegradationStrategy.REDUCE_CONTEXT, 
                "Retrying with reduced context", true);
        } else {
            // 放弃: 降级到静态分析
            logger.warning("LLM timeout after " + retryCount + " retries, using static analysis only");
            return new DegradationAction(DegradationStrategy.USE_STATIC_ONLY, 
                "LLM timeout, using static analysis results only");
        }
    }
    
    /**
     * LLM返回非法JSON处理
     */
    private static DegradationAction handleInvalidResponse(Exception e, Map<String, Object> context) {
        String responsePreview = (String) context.getOrDefault("response_preview", "");
        logger.severe("LLM returned invalid JSON. Preview: " + responsePreview.substring(0, Math.min(200, responsePreview.length())));
        
        // 记录原始响应供调试
        logRawResponse(responsePreview);
        
        // 降级: 返回空结果（不阻塞静态分析）
        return new DegradationAction(DegradationStrategy.RETURN_EMPTY, 
            "LLM response parsing failed, skipping AI review");
    }
    
    /**
     * Embedding生成失败处理
     */
    private static DegradationAction handleEmbeddingFailure(Exception e, Map<String, Object> context) {
        logger.warning("Embedding generation failed: " + e.getMessage());
        
        // 尝试使用零向量
        if (context.containsKey("text_length")) {
            logger.info("Using zero vector as fallback");
            return new DegradationAction(DegradationStrategy.REDUCE_CONTEXT, 
                "Using zero vector for embedding", false);
        }
        
        // 完全失败: 跳过该切片
        return new DegradationAction(DegradationStrategy.RETURN_EMPTY, 
            "Embedding failed, skipping this slice");
    }
    
    /**
     * 网络错误处理 (带指数退避重试)
     */
    private static DegradationAction handleNetworkError(Exception e, Map<String, Object> context) {
        int retryCount = getRetryCount(context);
        
        if (retryCount < MAX_RETRIES) {
            long backoffMs = INITIAL_BACKOFF_MS * (long) Math.pow(2, retryCount);
            logger.info("Network error, retrying in " + backoffMs + "ms (attempt " + (retryCount + 1) + ")");
            
            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
            return new DegradationAction(DegradationStrategy.REDUCE_CONTEXT, 
                "Retrying after network error", true);
        } else {
            logger.severe("Network error after " + MAX_RETRIES + " retries");
            return new DegradationAction(DegradationStrategy.USE_STATIC_ONLY, 
                "Network error, fallback to static analysis");
        }
    }
    
    /**
     * 向量存储错误处理
     */
    private static DegradationAction handleVectorStoreError(Exception e, Map<String, Object> context) {
        logger.warning("Vector store error: " + e.getMessage());
        
        // 降级到内存存储
        logger.info("Falling back to in-memory vector store");
        return new DegradationAction(DegradationStrategy.REDUCE_CONTEXT, 
            "Using in-memory vector store", false);
    }
    
    /**
     * 记录原始响应 (用于调试)
     */
    private static void logRawResponse(String response) {
        if (response == null || response.isEmpty()) return;
        
        logger.fine("=== Raw LLM Response ===");
        logger.fine(response);
        logger.fine("========================");
    }
    
    /**
     * 从上下文中获取重试次数
     */
    private static int getRetryCount(Map<String, Object> context) {
        Object count = context.get("retry_count");
        return count instanceof Number ? ((Number) count).intValue() : 0;
    }
    
    /**
     * 降级动作
     */
    public static class DegradationAction {
        private final DegradationStrategy strategy;
        private final String message;
        private final boolean shouldRetry;
        
        public DegradationAction(DegradationStrategy strategy, String message) {
            this(strategy, message, false);
        }
        
        public DegradationAction(DegradationStrategy strategy, String message, boolean shouldRetry) {
            this.strategy = strategy;
            this.message = message;
            this.shouldRetry = shouldRetry;
        }
        
        public DegradationStrategy getStrategy() { return strategy; }
        public String getMessage() { return message; }
        public boolean shouldRetry() { return shouldRetry; }
    }
}
