package cn.dolphinmind.glossary.java.analyze.rag.search;

import cn.dolphinmind.glossary.java.analyze.rag.model.RagSlice;
import java.util.*;

/**
 * 向量检索器 (基于 VectorStore)
 */
public class VectorSearcher {

    private final cn.dolphinmind.glossary.java.analyze.rag.store.VectorStore vectorStore;

    public VectorSearcher(cn.dolphinmind.glossary.java.analyze.rag.store.VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 向量相似度检索
     */
    public List<RagSlice> search(float[] queryVector, int topK) {
        return vectorStore.searchByVector(queryVector, topK);
    }
}
