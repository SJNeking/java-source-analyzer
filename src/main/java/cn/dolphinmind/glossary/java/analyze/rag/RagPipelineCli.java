package cn.dolphinmind.glossary.java.analyze.rag;

import cn.dolphinmind.glossary.java.analyze.config.AppConfig;
import cn.dolphinmind.glossary.java.analyze.rag.llm.LlmClient;
import cn.dolphinmind.glossary.java.analyze.rag.llm.OpenAIChatClient;
import cn.dolphinmind.glossary.java.analyze.rag.model.RagSlice;
import cn.dolphinmind.glossary.java.analyze.rag.search.Bm25Searcher;
import cn.dolphinmind.glossary.java.analyze.rag.service.EmbeddingService;
import cn.dolphinmind.glossary.java.analyze.rag.service.OllamaEmbeddingService;
import cn.dolphinmind.glossary.java.analyze.rag.service.OpenAIEmbeddingService;
import cn.dolphinmind.glossary.java.analyze.rag.store.PgVectorStore;
import cn.dolphinmind.glossary.java.analyze.rag.store.InMemoryVectorStore;
import cn.dolphinmind.glossary.java.analyze.rag.store.VectorStore;
import cn.dolphinmind.glossary.java.analyze.slicing.CodeSlicer;
import cn.dolphinmind.glossary.java.analyze.unified.UnifiedIssue;
import cn.dolphinmind.glossary.java.analyze.security.SecurityUtils;
import cn.dolphinmind.glossary.java.analyze.monitoring.HealthChecker;
import cn.dolphinmind.glossary.java.analyze.monitoring.MetricsCollector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * RAG Pipeline CLI
 * 
 * 用法:
 *   java -cp analyzer.jar cn.dolphinmind.glossary.java.analyze.rag.RagPipelineCli \
 *     --sourceRoot /path/to/java/project \
 *     --analysisResult analysis-result.json \
 *     --output ai-results.json \
 *     --embedding-provider ollama \
 *     --llm-provider openai \
 *     --query "审查代码质量"
 */
public class RagPipelineCli {

    private static final Logger logger = Logger.getLogger(RagPipelineCli.class.getName());

    public static void main(String[] args) {
        Map<String, String> params = parseArgs(args);

        if (params.containsKey("--help")) {
            printUsage();
            return;
        }

        if (params.containsKey("--health")) {
            runHealthCheck(params);
            return;
        }

        if (params.containsKey("--metrics")) {
            runMetrics();
            return;
        }

        String sourceRoot = params.get("--sourceRoot");
        String analysisResultPath = params.get("--analysisResult");
        String outputPath = params.get("--output");
        String query = params.getOrDefault("--query", "review code quality");
        String embeddingProvider = params.getOrDefault("--embedding-provider", "ollama");
        String llmProvider = params.getOrDefault("--llm-provider", "openai");

        if (sourceRoot == null || analysisResultPath == null) {
            System.err.println("Error: --sourceRoot and --analysisResult are required");
            printUsage();
            System.exit(1);
        }

        try {
            // Security: Validate file paths
            // For CLI usage, validate extension but allow absolute paths
            Path analysisResultFile = Paths.get(analysisResultPath).normalize().toAbsolutePath();
            SecurityUtils.validateExtension(analysisResultFile.toString(), "json");
            
            // Verify file exists
            if (!Files.exists(analysisResultFile)) {
                throw new IllegalArgumentException("File not found: " + analysisResultPath);
            }
            
            Path sourceRootPath = Paths.get(sourceRoot).normalize().toAbsolutePath();
            if (!Files.exists(sourceRootPath)) {
                throw new IllegalArgumentException("Source root not found: " + sourceRoot);
            }
            
            if (outputPath != null) {
                Path outputFile = Paths.get(outputPath).normalize().toAbsolutePath();
                SecurityUtils.validateExtension(outputFile.toString(), "json");
                
                // Create parent directory if needed
                if (outputFile.getParent() != null && !Files.exists(outputFile.getParent())) {
                    Files.createDirectories(outputFile.getParent());
                }
            }

            System.out.println("=== RAG Pipeline ===");
            System.out.println("Source: " + SecurityUtils.sanitizeLogInput(sourceRootPath.toString()));
            System.out.println("Analysis: " + SecurityUtils.sanitizeLogInput(analysisResultPath));
            System.out.println("Embedding: " + embeddingProvider);
            System.out.println("LLM: " + llmProvider);
            System.out.println();

            // Step 1: Load analysis result & generate slices
            System.out.println("Step 1: Loading analysis result...");
            String jsonContent = new String(Files.readAllBytes(Paths.get(analysisResultPath)));
            @SuppressWarnings("unchecked")
            Map<String, Object> analysisResult = new Gson().fromJson(jsonContent, Map.class);

            CodeSlicer slicer = new CodeSlicer();
            List<RagSlice> slices = convertToRagSlices(slicer, sourceRootPath, analysisResult);
            System.out.println("  Generated " + slices.size() + " code slices");

            // Step 2: Initialize RAG Pipeline
            System.out.println("Step 2: Initializing RAG Pipeline...");
            RagPipeline pipeline = createPipeline(embeddingProvider, llmProvider, params);
            pipeline.initialize();

            // Step 3: Index slices
            System.out.println("Step 3: Indexing slices...");
            pipeline.indexSlices(slices);

            // Step 4: Run review
            System.out.println("Step 4: Running review with query: \"" + query + "\"");
            List<UnifiedIssue> issues = pipeline.review(query, null);
            System.out.println("  Found " + issues.size() + " AI issues");

            // Step 5: Output results
            System.out.println("Step 5: Writing results...");
            if (outputPath != null) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("version", "1.0.0");
                result.put("model", params.getOrDefault("--llm-model", "gpt-4"));
                result.put("issues", issuesToMaps(issues));

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Files.write(Paths.get(outputPath), gson.toJson(result).getBytes());
                System.out.println("  Written to: " + outputPath);
            }

            // Print summary
            System.out.println();
            System.out.println("=== Review Summary ===");
            for (UnifiedIssue issue : issues) {
                System.out.println("  [" + issue.getSeverity() + "] " + issue.getMessage());
                System.out.println("    File: " + issue.getFilePath() + ":" + issue.getLine());
                if (issue.getConfidence() != null) {
                    System.out.println("    Confidence: " + String.format("%.0f%%", issue.getConfidence() * 100));
                }
            }

            pipeline.close();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<RagSlice> convertToRagSlices(CodeSlicer slicer, Path sourceRoot,
                                                      Map<String, Object> analysisResult) throws Exception {
        // Security: Validate sourceRoot is within allowed boundaries (optional, but good practice)
        // For now, we assume the user running the CLI has permissions.
        
        List<RagSlice> ragSlices = new ArrayList<>();

        // Use CodeSlicer to generate slices from analysis result
        Map<String, Object> ragContext = slicer.buildRagContext(sourceRoot, analysisResult);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sliceMaps = (List<Map<String, Object>>) ragContext.get("slices");

        if (sliceMaps != null) {
            for (Map<String, Object> map : sliceMaps) {
                RagSlice slice = new RagSlice();
                slice.setFilePath((String) map.get("filePath"));
                slice.setClassName((String) map.get("className"));
                slice.setMethodName((String) map.get("methodName"));
                slice.setStartLine(map.get("startLine") instanceof Number ? ((Number) map.get("startLine")).intValue() : 0);
                slice.setEndLine(map.get("endLine") instanceof Number ? ((Number) map.get("endLine")).intValue() : 0);
                slice.setCode((String) map.get("code"));
                slice.setTokenCount(map.get("tokenCount") instanceof Number ? ((Number) map.get("tokenCount")).intValue() : 0);
                String typeStr = (String) map.get("type");
                if (typeStr != null) {
                    try { slice.setType(RagSlice.SliceType.valueOf(typeStr)); }
                    catch (IllegalArgumentException e) { slice.setType(RagSlice.SliceType.ISSUE_AREA); }
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) map.get("context");
                if (metadata != null) slice.setMetadata(metadata);
                ragSlices.add(slice);
            }
        }

        return ragSlices;
    }

    private static RagPipeline createPipeline(String embeddingProvider, String llmProvider,
                                                Map<String, String> params) {
        AppConfig config = AppConfig.getInstance();

        // Embedding Service
        EmbeddingService embeddingService;
        if ("ollama".equals(embeddingProvider)) {
            embeddingService = new OllamaEmbeddingService(
                    params.getOrDefault("--embedding-endpoint", config.getString("ollama.endpoint", "http://localhost:11434/api/embeddings")),
                    params.getOrDefault("--embedding-model", config.getString("ollama.embed-model", "nomic-embed-text")));
        } else {
            String apiKey = params.getOrDefault("--api-key", config.getString("openai.api-key", ""));
            if (apiKey.isEmpty()) {
                System.err.println("Warning: No API key provided. Using zero embeddings.");
            }
            embeddingService = new OpenAIEmbeddingService(
                    apiKey,
                    params.getOrDefault("--embedding-endpoint", config.getString("openai.endpoint", "https://api.openai.com/v1/embeddings")),
                    params.getOrDefault("--embedding-model", config.getString("openai.embed-model", "text-embedding-3-small")));
        }

        // Vector Store (auto检测 PGVector 是否可用)
        VectorStore vectorStore;
        String jdbcUrl = params.getOrDefault("--database-url", config.getDatabaseUrl());
        String dbUser = params.getOrDefault("--database-user", config.getDatabaseUser());
        String dbPassword = params.getOrDefault("--database-password", config.getDatabasePassword());

        if (isPgVectorAvailable(jdbcUrl, dbUser, dbPassword)) {
            vectorStore = new PgVectorStore(jdbcUrl, dbUser, dbPassword, embeddingService.getDimension());
            System.out.println("  Using PGVector store");
        } else {
            vectorStore = new InMemoryVectorStore();
            System.out.println("  Using in-memory vector store (PGVector not available)");
        }

        // BM25 Searcher (also auto-detect)
        Bm25Searcher bm25Searcher;
        if (vectorStore instanceof PgVectorStore) {
            bm25Searcher = new Bm25Searcher(jdbcUrl, dbUser, dbPassword);
        } else {
            // For in-memory, BM25 is not applicable; use a no-op
            bm25Searcher = new Bm25Searcher(null, null, null);
        }

        // LLM Client
        LlmClient llmClient;
        String llmApiKey = params.getOrDefault("--api-key", config.getString("openai.api-key", ""));
        String llmModel = params.getOrDefault("--llm-model", config.getString("ollama.chat-model", "qwen2.5-coder:32b"));
        String llmEndpoint;

        // Auto-detect Ollama
        if ("ollama".equals(llmProvider) || llmApiKey.isEmpty()) {
            llmEndpoint = params.getOrDefault("--llm-endpoint", config.getString("ollama.endpoint", "http://localhost:11434"));
            // Ollama uses /v1/chat/completions for OpenAI-compatible endpoint
            if (!llmEndpoint.contains("/v1/")) {
                llmEndpoint = llmEndpoint.endsWith("/") ? llmEndpoint + "v1/chat/completions" : llmEndpoint + "/v1/chat/completions";
            }
            System.out.println("  Using Ollama LLM: " + llmModel + " at " + llmEndpoint);
        } else {
            llmEndpoint = params.getOrDefault("--llm-endpoint", config.getString("openai.endpoint", "https://api.openai.com/v1/chat/completions"));
            System.out.println("  Using OpenAI LLM: " + llmModel);
        }

        double temperature = 0.3;
        try { temperature = Double.parseDouble(params.getOrDefault("--temperature", "0.3")); }
        catch (NumberFormatException ignored) {}

        llmClient = new OpenAIChatClient(llmApiKey.isEmpty() ? "ollama" : llmApiKey, llmEndpoint, llmModel, temperature);

        int topK = 10;
        try { topK = Integer.parseInt(params.getOrDefault("--top-k", "10")); }
        catch (NumberFormatException ignored) {}

        return new RagPipeline(embeddingService, vectorStore, bm25Searcher, llmClient, topK);
    }

    /**
     * 检测 PGVector 是否可用
     */
    private static boolean isPgVectorAvailable(String jdbcUrl, String user, String password) {
        try {
            java.sql.Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, user, password);
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_extension WHERE extname = 'vector'");
            boolean available = rs.next();
            stmt.close();
            conn.close();
            return available;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> issuesToMaps(List<UnifiedIssue> issues) {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (UnifiedIssue issue : issues) {
            maps.add(issue.toMap());
        }
        return maps;
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i];
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    String value = args[++i];
                    // Security: Sanitize log inputs
                    params.put(SecurityUtils.sanitizeLogInput(key), SecurityUtils.sanitizeLogInput(value));
                } else {
                    params.put(SecurityUtils.sanitizeLogInput(key), "");
                }
            }
        }
        return params;
    }

    private static void printUsage() {
        System.out.println("Usage: java RagPipelineCli [options]");
        System.out.println();
        System.out.println("Required:");
        System.out.println("  --sourceRoot <path>           Java source root directory");
        System.out.println("  --analysisResult <file>       Static analysis result JSON");
        System.out.println();
        System.out.println("Optional:");
        System.out.println("  --health                      Run health checks and exit");
        System.out.println("  --metrics                     Print metrics in Prometheus format and exit");
        System.out.println("  --output <file>               Output AI results JSON");
        System.out.println("  --query <text>                Review query (default: 'review code quality')");
        System.out.println("  --embedding-provider <name>   ollama | openai (default: ollama)");
        System.out.println("  --llm-provider <name>         openai (default: openai)");
        System.out.println("  --llm-model <model>           LLM model name (default: gpt-4)");
        System.out.println("  --embedding-model <model>     Embedding model (default: nomic-embed-text)");
        System.out.println("  --api-key <key>               API key for OpenAI/Qwen/etc");
        System.out.println("  --database-url <url>          PostgreSQL JDBC URL");
        System.out.println("  --database-user <user>        Database username");
        System.out.println("  --database-password <pass>    Database password");
        System.out.println("  --help                        Show this help");
    }

    private static void runHealthCheck(Map<String, String> params) {
        AppConfig config = AppConfig.getInstance();
        String dbUrl = params.getOrDefault("--database-url", config.getDatabaseUrl());
        String dbUser = params.getOrDefault("--database-user", config.getDatabaseUser());
        String dbPass = params.getOrDefault("--database-password", config.getDatabasePassword());
        String ollama = params.getOrDefault("--ollama-endpoint", config.getString("ollama.endpoint", "http://localhost:11434"));
        String minio = params.getOrDefault("--minio-endpoint", config.getString("minio.endpoint", "http://localhost:19000"));

        HealthChecker checker = new HealthChecker(dbUrl, dbUser, dbPass, ollama, minio);
        Map<String, Object> health = checker.checkAll();
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(health));
        
        if ("DOWN".equals(health.get("status"))) {
            System.exit(1);
        }
    }

    private static void runMetrics() {
        System.out.println(MetricsCollector.getInstance().exportPrometheus());
    }
}
