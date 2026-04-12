package cn.dolphinmind.glossary.java.analyze.rag;

import cn.dolphinmind.glossary.java.analyze.rag.model.RagSlice;
import cn.dolphinmind.glossary.java.analyze.rag.service.EmbeddingService;
import cn.dolphinmind.glossary.java.analyze.rag.store.VectorStore;
import cn.dolphinmind.glossary.java.analyze.rag.search.Bm25Searcher;
import cn.dolphinmind.glossary.java.analyze.rag.search.HybridSearcher;
import cn.dolphinmind.glossary.java.analyze.rag.search.VectorSearcher;
import cn.dolphinmind.glossary.java.analyze.rag.llm.LlmClient;
import cn.dolphinmind.glossary.java.analyze.rag.prompt.ReviewPromptBuilder;
import cn.dolphinmind.glossary.java.analyze.unified.UnifiedIssue;

import java.util.*;
import java.util.logging.Logger;

/**
 * RAG Pipeline Orchestrator
 * 
 * 编排完整的 RAG 审查流程:
 * 1. 从 CodeSlicer 获取切片
 * 2. 生成 Embedding 并存储到 PGVector
 * 3. 混合检索 (Vector + BM25)
 * 4. 构建 Prompt
 * 5. 调用 LLM 生成审查结果
 * 6. 返回结构化 UnifiedIssue 列表
 */
public class RagPipeline {

    private static final Logger logger = Logger.getLogger(RagPipeline.class.getName());
    private static final int DEFAULT_TOP_K = 10;
    private static final double RRF_K = 60.0;

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final Bm25Searcher bm25Searcher;
    private final LlmClient llmClient;
    private final int topK;

    public RagPipeline(EmbeddingService embeddingService,
                       VectorStore vectorStore,
                       Bm25Searcher bm25Searcher,
                       LlmClient llmClient) {
        this(embeddingService, vectorStore, bm25Searcher, llmClient, DEFAULT_TOP_K);
    }

    public RagPipeline(EmbeddingService embeddingService,
                       VectorStore vectorStore,
                       Bm25Searcher bm25Searcher,
                       LlmClient llmClient,
                       int topK) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.bm25Searcher = bm25Searcher;
        this.llmClient = llmClient;
        this.topK = topK;
    }

    /**
     * 初始化所有组件
     */
    public void initialize() {
        logger.info("Initializing RAG Pipeline...");
        vectorStore.initialize();
        bm25Searcher.initialize();
        logger.info("RAG Pipeline initialized (model: " + llmClient.getModelName() +
                ", embedding: " + embeddingService.getModelName() + ")");
    }

    /**
     * 索引切片 — 生成 Embedding 并存储
     */
    public void indexSlices(List<RagSlice> slices) {
        logger.info("Indexing " + slices.size() + " slices...");
        long startTime = System.currentTimeMillis();

        // 生成 Embedding
        List<String> texts = new ArrayList<>();
        for (RagSlice slice : slices) {
            texts.add(slice.toSearchableText());
        }

        float[][] embeddings = embeddingService.embedBatch(texts.toArray(new String[0]));

        // 设置向量并存储
        for (int i = 0; i < slices.size(); i++) {
            slices.get(i).setEmbedding(embeddings[i]);
            vectorStore.upsert(slices.get(i));
        }

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("Indexed " + slices.size() + " slices in " + elapsed + "ms");
    }

    /**
     * 执行 RAG 审查 — 完整流程
     * 
     * @param query 查询文本 (如 "审查这个方法的代码质量")
     * @param additionalContext 附加上下文 (如静态分析结果摘要)
     * @return LLM 生成的审查问题列表
     */
    public List<UnifiedIssue> review(String query, String additionalContext) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting RAG review for: " + query);

        // Step 1: 生成查询向量
        float[] queryVector = embeddingService.embed(query);

        // Step 2: 混合检索
        HybridSearcher hybridSearcher = new HybridSearcher(
                new VectorSearcher(vectorStore), bm25Searcher);
        List<RagSlice> retrievedSlices = hybridSearcher.search(queryVector, query, topK);

        if (retrievedSlices.isEmpty()) {
            logger.warning("No relevant code slices found for query: " + query);
            return Collections.emptyList();
        }

        logger.info("Retrieved " + retrievedSlices.size() + " relevant slices");

        // Step 3: 构建 Prompt
        String systemPrompt = ReviewPromptBuilder.buildSystemPrompt();
        String userPrompt = ReviewPromptBuilder.buildUserPrompt(retrievedSlices, additionalContext);
        String fullPrompt = systemPrompt + "\n\n" + userPrompt;

        // Step 4: 调用 LLM
        logger.info("Calling LLM (" + llmClient.getModelName() + ")...");
        List<UnifiedIssue> issues = llmClient.reviewCode(retrievedSlices, userPrompt);

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("RAG review completed in " + elapsed + "ms, " + issues.size() + " issues found");

        return issues;
    }

    /**
     * 执行 RAG 审查 — 基于特定问题的上下文
     */
    public List<UnifiedIssue> reviewWithContext(RagSlice targetSlice, String issueDescription) {
        // 构建检索查询
        String query = ReviewPromptBuilder.buildRetrievalQuery(issueDescription);
        if (query.isEmpty()) {
            query = targetSlice.getClassName() + " " + targetSlice.getMethodName();
        }

        // 构建附加上下文
        String context = "Static analysis found: " + issueDescription;

        return review(query, context);
    }

    /**
     * 清理资源
     */
    public void close() {
        vectorStore.close();
        bm25Searcher.close();
    }

    /**
     * 获取组件信息 (用于日志和报告)
     */
    public Map<String, String> getComponentInfo() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("embedding_model", embeddingService.getModelName());
        info.put("embedding_dimension", String.valueOf(embeddingService.getDimension()));
        info.put("llm_model", llmClient.getModelName());
        info.put("top_k", String.valueOf(topK));
        return info;
    }
}
