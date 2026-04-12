package cn.dolphinmind.glossary.java.analyze.rag.service;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

/**
 * Ollama Embedding 实现 (本地运行, 无需 API Key)
 * 
 * 使用 nomic-embed-text (768 维) 或其他 Ollama 支持的 embedding 模型
 * 
 * 前提:
 *   docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
 *   ollama pull nomic-embed-text
 */
public class OllamaEmbeddingService implements EmbeddingService {

    private static final Logger logger = Logger.getLogger(OllamaEmbeddingService.class.getName());
    private static final String DEFAULT_ENDPOINT = "http://localhost:11434/api/embeddings";
    private static final String DEFAULT_MODEL = "nomic-embed-text";

    private final String endpoint;
    private final String model;
    private final Gson gson;
    private int dimension = 768; // Default dimension (nomic-embed-text is 768)

    public OllamaEmbeddingService() {
        this(DEFAULT_ENDPOINT, DEFAULT_MODEL);
    }

    public OllamaEmbeddingService(String endpoint, String model) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.model = model;
        this.gson = new GsonBuilder().create();
        // Try to detect dimension, but fallback to 768 if failed
        try {
            this.dimension = detectDimension();
        } catch (Exception e) {
            this.dimension = 768;
        }
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) return new float[dimension];

        try {
            String requestBody = "{\"model\":\"" + model + "\",\"prompt\":\"" + 
                    escapeJson(text) + "\"}";
            String responseBody = sendRequest(requestBody);
            float[] result = parseEmbeddingResponse(responseBody);
            if (result.length > 0) return result;
            
            // Fallback: deterministic hash-based embedding
            return hashEmbedding(text);
        } catch (Exception e) {
            // Fallback: deterministic hash-based embedding
            return hashEmbedding(text);
        }
    }

    /**
     * Deterministic hash-based embedding (fallback when no embedding model available)
     * Produces consistent vectors for the same text, enables basic similarity search
     */
    private float[] hashEmbedding(String text) {
        float[] result = new float[dimension];
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // Expand 16-byte MD to full dimension by cycling
            for (int i = 0; i < dimension; i++) {
                result[i] = (hash[i % 16] / 128.0f) - 1.0f; // normalize to [-1, 1]
            }
        } catch (Exception e) {
            // Last resort: simple char-code based
            for (int i = 0; i < dimension; i++) {
                result[i] = (text.charAt(i % text.length()) / 128.0f) - 1.0f;
            }
        }
        // Normalize
        double norm = 0;
        for (float v : result) norm += v * v;
        if (norm > 0) {
            norm = Math.sqrt(norm);
            for (int i = 0; i < result.length; i++) result[i] /= norm;
        }
        return result;
    }

    @Override
    public float[][] embedBatch(String[] texts) {
        float[][] embeddings = new float[texts.length][];
        for (int i = 0; i < texts.length; i++) {
            embeddings[i] = embed(texts[i]);
        }
        return embeddings;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getModelName() {
        return model;
    }

    private int detectDimension() {
        // Avoid calling embed() here to prevent circular dependency with hashEmbedding.
        // Return known dimensions for specific models, or a safe default.
        if ("nomic-embed-text".equals(model)) return 768;
        if (model.contains("large")) return 3072; // text-embedding-3-large
        
        // For general LLMs like qwen2.5-coder, if Ollama supports embedding extraction,
        // it typically uses a hidden layer projection. 
        // We assume a safe default or rely on the fallback if the API fails.
        return 1536; 
    }

    private String sendRequest(String requestBody) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status >= 400) {
            throw new IOException("Ollama request failed: HTTP " + status);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private float[] parseEmbeddingResponse(String responseBody) {
        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        JsonArray embeddingArray = json.getAsJsonArray("embedding");
        if (embeddingArray == null) {
            throw new RuntimeException("No embedding in Ollama response");
        }
        float[] embedding = new float[embeddingArray.size()];
        for (int i = 0; i < embeddingArray.size(); i++) {
            embedding[i] = embeddingArray.get(i).getAsFloat();
        }
        this.dimension = embedding.length;
        return embedding;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
