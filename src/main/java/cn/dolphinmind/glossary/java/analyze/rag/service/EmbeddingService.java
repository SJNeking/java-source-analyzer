package cn.dolphinmind.glossary.java.analyze.rag.service;

/**
 * Embedding Service 接口
 * 
 * 支持多种 Embedding 模型 (OpenAI, Ollama, 本地模型)
 */
public interface EmbeddingService {

    /**
     * 生成单个文本的向量
     * @return 浮点数组, 维度取决于模型 (384 for nomic, 1536 for text-embedding-3-small)
     */
    float[] embed(String text);

    /**
     * 批量生成向量
     */
    float[][] embedBatch(String[] texts);

    /**
     * 向量维度
     */
    int getDimension();

    /**
     * 模型名称
     */
    String getModelName();
}
