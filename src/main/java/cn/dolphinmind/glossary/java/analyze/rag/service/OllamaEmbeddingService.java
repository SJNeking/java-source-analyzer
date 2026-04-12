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
    private int dimension; // 延迟检测

    public OllamaEmbeddingService() {
        this(DEFAULT_ENDPOINT, DEFAULT_MODEL);
    }

    public OllamaEmbeddingService(String endpoint, String model) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.model = model;
        this.gson = new GsonBuilder().create();
        this.dimension = detectDimension();
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) return new float[dimension];

        try {
            String requestBody = "{\"model\":\"" + model + "\",\"prompt\":\"" + 
                    escapeJson(text) + "\"}";
            String responseBody = sendRequest(requestBody);
            return parseEmbeddingResponse(responseBody);
        } catch (Exception e) {
            logger.warning("Ollama embedding failed: " + e.getMessage());
            return new float[dimension];
        }
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
        try {
            float[] test = embed("test");
            return test.length;
        } catch (Exception e) {
            // 默认维度 (nomic-embed-text = 768)
            return 768;
        }
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
