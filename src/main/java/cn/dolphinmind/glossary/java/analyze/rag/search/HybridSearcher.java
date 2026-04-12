package cn.dolphinmind.glossary.java.analyze.rag.search;

import cn.dolphinmind.glossary.java.analyze.rag.model.RagSlice;

import java.util.*;
import java.util.logging.Logger;

/**
 * 混合检索 (RRF - Reciprocal Rank Fusion)
 * 
 * 结合向量检索和 BM25 关键词检索的结果, 使用 RRF 算法重排
 * 
 * RRF 公式: score = 1/(k + rank_vector) + 1/(k + rank_bm25)
 * 其中 k 是常数 (通常取 60)
 */
public class HybridSearcher {

    private static final Logger logger = Logger.getLogger(HybridSearcher.class.getName());
    private static final double RRF_K = 60.0;  // RRF 常数

    private final VectorSearcher vectorSearcher;
    private final Bm25Searcher bm25Searcher;

    public HybridSearcher(VectorSearcher vectorSearcher, Bm25Searcher bm25Searcher) {
        this.vectorSearcher = vectorSearcher;
        this.bm25Searcher = bm25Searcher;
    }

    /**
     * 混合检索
     * @param queryVector 查询向量
     * @param queryText 查询文本 (用于 BM25)
     * @param topK 返回数量
     * @return 按混合分数降序排列的结果
     */
    public List<RagSlice> search(float[] queryVector, String queryText, int topK) {
        long startTime = System.currentTimeMillis();

        // 并行执行向量检索和 BM25 检索
        List<RagSlice> vectorResults = vectorSearcher.search(queryVector, topK * 2);
        List<RagSlice> bm25Results = bm25Searcher.search(queryText, topK * 2);

        logger.info("Vector search: " + vectorResults.size() + " results, " +
                "BM25 search: " + bm25Results.size() + " results");

        // RRF 重排
        List<RagSlice> merged = rrfRerank(vectorResults, bm25Results, topK);

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("Hybrid search completed in " + elapsed + "ms, " + merged.size() + " results");

        return merged;
    }

    /**
     * RRF (Reciprocal Rank Fusion) 重排算法
     */
    private List<RagSlice> rrfRerank(List<RagSlice> vectorResults, 
                                       List<RagSlice> bm25Results, 
                                       int topK) {
        // 建立 ID -> Slice 映射
        Map<String, RagSlice> sliceMap = new LinkedHashMap<>();
        Map<String, Double> vectorRankMap = new HashMap<>();
        Map<String, Double> bm25RankMap = new HashMap<>();

        // 向量检索排名
        for (int i = 0; i < vectorResults.size(); i++) {
            RagSlice slice = vectorResults.get(i);
            String key = sliceKey(slice);
            sliceMap.put(key, slice);
            vectorRankMap.put(key, (double) (i + 1));
        }

        // BM25 排名
        for (int i = 0; i < bm25Results.size(); i++) {
            RagSlice slice = bm25Results.get(i);
            String key = sliceKey(slice);
            sliceMap.putIfAbsent(key, slice);
            bm25RankMap.put(key, (double) (i + 1));
        }

        // 计算 RRF 分数
        List<RagSliceWithScore> scored = new ArrayList<>();
        for (Map.Entry<String, RagSlice> entry : sliceMap.entrySet()) {
            String key = entry.getKey();
            RagSlice slice = entry.getValue();

            double vecRank = vectorRankMap.getOrDefault(key, Double.MAX_VALUE);
            double bm25Rank = bm25RankMap.getOrDefault(key, Double.MAX_VALUE);

            double rrfVector = 1.0 / (RRF_K + vecRank);
            double rrfBm25 = 1.0 / (RRF_K + bm25Rank);
            double hybridScore = rrfVector + rrfBm25;

            slice.setHybridScore(hybridScore);
            scored.add(new RagSliceWithScore(slice, hybridScore));
        }

        // 排序并取 Top-K
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        List<RagSlice> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            results.add(scored.get(i).slice);
        }

        return results;
    }

    private String sliceKey(RagSlice slice) {
        return slice.getFilePath() + ":" + slice.getStartLine() + 
                (slice.getMethodName() != null ? ":" + slice.getMethodName() : "");
    }

    private static class RagSliceWithScore {
        final RagSlice slice;
        final double score;
        RagSliceWithScore(RagSlice s, double sc) {
            this.slice = s;
            this.score = sc;
        }
    }
}
