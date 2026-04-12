package cn.dolphinmind.glossary.java.analyze.rag.store;

import cn.dolphinmind.glossary.java.analyze.rag.model.RagSlice;

import java.util.*;
import java.util.logging.Logger;

/**
 * 内存向量存储 (开发/测试用, 无需 PGVector)
 * 
 * 当 PostgreSQL 未安装 pgvector 扩展时使用此实现。
 * 使用暴力余弦相似度搜索, 适合小规模数据集 (< 10000 slices)。
 */
public class InMemoryVectorStore implements VectorStore {

    private static final Logger logger = Logger.getLogger(InMemoryVectorStore.class.getName());

    private final Map<String, RagSlice> store = new LinkedHashMap<>();
    private volatile boolean initialized = false;

    public InMemoryVectorStore() {}

    @Override
    public void initialize() {
        initialized = true;
        logger.info("In-memory vector store initialized");
    }

    @Override
    public void upsert(RagSlice slice) {
        if (!initialized) throw new IllegalStateException("Store not initialized");
        String key = sliceKey(slice);
        store.put(key, slice);
    }

    @Override
    public void upsertBatch(List<RagSlice> slices) {
        for (RagSlice slice : slices) {
            upsert(slice);
        }
        logger.info("Batch upserted " + slices.size() + " slices, total: " + store.size());
    }

    @Override
    public List<RagSlice> searchByVector(float[] queryVector, int topK) {
        if (!initialized || queryVector == null) return Collections.emptyList();

        List<ScoredSlice> scored = new ArrayList<>();
        for (RagSlice slice : store.values()) {
            if (slice.getEmbedding() != null) {
                double similarity = cosineSimilarity(slice.getEmbedding(), queryVector);
                scored.add(new ScoredSlice(slice, similarity));
            }
        }

        // Sort by similarity descending
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // Take top-K
        List<RagSlice> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            results.add(scored.get(i).slice);
        }

        return results;
    }

    @Override
    public List<RagSlice> getByProject(String projectName) {
        List<RagSlice> results = new ArrayList<>();
        for (RagSlice slice : store.values()) {
            Map<String, Object> meta = slice.getMetadata();
            String proj = meta != null ? (String) meta.get("project") : null;
            if (projectName.equals(proj)) {
                results.add(slice);
            }
        }
        return results;
    }

    @Override
    public void deleteByProject(String projectName) {
        store.entrySet().removeIf(entry -> {
            Map<String, Object> meta = entry.getValue().getMetadata();
            String proj = meta != null ? (String) meta.get("project") : null;
            return projectName.equals(proj);
        });
        logger.info("Deleted slices for project: " + projectName + ", remaining: " + store.size());
    }

    @Override
    public void close() {
        store.clear();
        initialized = false;
    }

    /**
     * 返回存储的切片数量
     */
    public int size() {
        return store.size();
    }

    // ========== 内部方法 ==========

    private String sliceKey(RagSlice slice) {
        return slice.getFilePath() + ":" + slice.getStartLine() +
                (slice.getMethodName() != null ? ":" + slice.getMethodName() : "");
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null) return 0;
        double dot = 0, normA = 0, normB = 0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static class ScoredSlice {
        final RagSlice slice;
        final double score;
        ScoredSlice(RagSlice s, double sc) {
            this.slice = s;
            this.score = sc;
        }
    }
}
