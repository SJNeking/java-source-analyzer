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
 * OpenAI Embedding 实现
 * 
 * 使用 text-embedding-3-small (1536 维) 或 text-embedding-3-large (3072 维)
 * 通过 OpenAI-compatible API 调用 (支持兼容端点如 Azure, DeepSeek 等)
 */
public class OpenAIEmbeddingService implements EmbeddingService {

    private static final Logger logger = Logger.getLogger(OpenAIEmbeddingService.class.getName());
    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/embeddings";
    private static final String DEFAULT_MODEL = "text-embedding-3-small";

    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final Gson gson;

    public OpenAIEmbeddingService(String apiKey) {
        this(apiKey, DEFAULT_ENDPOINT, DEFAULT_MODEL);
    }

    public OpenAIEmbeddingService(String apiKey, String endpoint, String model) {
        this.apiKey = apiKey;
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.model = model;
        this.gson = new GsonBuilder().create();
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) return new float[getDimension()];

        try {
            String requestBody = gson.toJson(createRequestBody(text));
            String responseBody = sendRequest(requestBody);
            return parseEmbeddingResponse(responseBody);
        } catch (Exception e) {
            logger.warning("OpenAI embedding failed: " + e.getMessage());
            return new float[getDimension()]; // 返回零向量
        }
    }

    @Override
    public float[][] embedBatch(String[] texts) {
        float[][] embeddings = new float[texts.length][];
        // OpenAI API 支持批量, 这里简单循环调用
        // 生产环境应使用真正的批量请求
        for (int i = 0; i < texts.length; i++) {
            embeddings[i] = embed(texts[i]);
        }
        return embeddings;
    }

    @Override
    public int getDimension() {
        if (model.contains("large")) return 3072;
        if (model.contains("ada")) return 1536;
        return 1536; // text-embedding-3-small
    }

    @Override
    public String getModelName() {
        return model;
    }

    private JsonObject createRequestBody(String text) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("input", text);
        body.addProperty("encoding_format", "float");
        return body;
    }

    private String sendRequest(String requestBody) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status >= 400) {
            throw new IOException("API request failed: HTTP " + status);
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
        JsonArray dataArray = json.getAsJsonArray("data");
        if (dataArray == null || dataArray.size() == 0) {
            throw new RuntimeException("No embedding data in response");
        }
        JsonArray embeddingArray = dataArray.get(0).getAsJsonObject().getAsJsonArray("embedding");
        float[] embedding = new float[embeddingArray.size()];
        for (int i = 0; i < embeddingArray.size(); i++) {
            embedding[i] = embeddingArray.get(i).getAsFloat();
        }
        return embedding;
    }
}
