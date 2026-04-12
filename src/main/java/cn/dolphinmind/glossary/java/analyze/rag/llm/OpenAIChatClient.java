package cn.dolphinmind.glossary.java.analyze.rag.llm;

import cn.dolphinmind.glossary.java.analyze.rag.model.RagSlice;
import cn.dolphinmind.glossary.java.analyze.rag.prompt.ReviewPromptBuilder;
import cn.dolphinmind.glossary.java.analyze.unified.UnifiedIssue;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import com.google.gson.*;

/**
 * OpenAI Chat 实现 (兼容 OpenAI-compatible APIs: Qwen, DeepSeek, etc.)
 */
public class OpenAIChatClient implements LlmClient {

    private static final Logger logger = Logger.getLogger(OpenAIChatClient.class.getName());
    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final double temperature;
    private final Gson gson;

    public OpenAIChatClient(String apiKey, String model) {
        this(apiKey, DEFAULT_ENDPOINT, model, 0.3);
    }

    public OpenAIChatClient(String apiKey, String endpoint, String model, double temperature) {
        this.apiKey = apiKey;
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.model = model;
        this.temperature = temperature;
        this.gson = new Gson();
    }

    @Override
    public String chat(String prompt) {
        try {
            JsonObject requestBody = createChatBody(prompt, false);
            String responseBody = sendRequest(requestBody);
            return parseChatResponse(responseBody);
        } catch (Exception e) {
            logger.warning("OpenAI chat failed: " + e.getMessage());
            return "";
        }
    }

    @Override
    public List<UnifiedIssue> reviewCode(List<RagSlice> context, String prompt) {
        try {
            // 构建结构化审查提示
            String systemPrompt = ReviewPromptBuilder.buildSystemPrompt();
            String userPrompt = ReviewPromptBuilder.buildUserPrompt(context, prompt);
            String fullPrompt = systemPrompt + "\n\n" + userPrompt;

            JsonObject requestBody = createChatBody(fullPrompt, true);
            String responseBody = sendRequest(requestBody);
            return parseReviewResponse(responseBody);
        } catch (Exception e) {
            logger.warning("Code review failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public String getModelName() {
        return model;
    }

    private JsonObject createChatBody(String prompt, boolean jsonMode) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", temperature);

        JsonArray messages = new JsonArray();
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "You are an expert code review assistant. Respond with valid JSON only.");
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);

        body.add("messages", messages);

        // response_format is not supported by all providers (Ollama may reject it)
        // We rely on system prompt to enforce JSON output instead

        return body;
    }

    private String sendRequest(JsonObject requestBody) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(gson.toJson(requestBody).getBytes(StandardCharsets.UTF_8));
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

    private String parseChatResponse(String responseBody) {
        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        return json.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }

    @SuppressWarnings("unchecked")
    private List<UnifiedIssue> parseReviewResponse(String responseBody) {
        String content = parseChatResponse(responseBody);

        // 提取 JSON (LLM 可能返回 Markdown 包裹)
        int start = content.indexOf("{");
        int end = content.lastIndexOf("}");
        if (start >= 0 && end > start) {
            content = content.substring(start, end + 1);
        }

        try {
            JsonElement parsed = gson.fromJson(content, JsonElement.class);
            JsonArray issuesArray = null;

            // Handle different response formats
            if (parsed.isJsonArray()) {
                // LLM returned a raw array of issues
                issuesArray = parsed.getAsJsonArray();
            } else if (parsed.isJsonObject()) {
                JsonObject obj = parsed.getAsJsonObject();
                if (obj.has("issues")) {
                    issuesArray = obj.getAsJsonArray("issues");
                } else {
                    // Single issue object
                    issuesArray = new JsonArray();
                    issuesArray.add(obj);
                }
            }

            if (issuesArray == null) return Collections.emptyList();

            List<UnifiedIssue> issues = new ArrayList<>();
            for (int i = 0; i < issuesArray.size(); i++) {
                JsonElement el = issuesArray.get(i);
                if (!el.isJsonObject()) continue;
                JsonObject issueJson = el.getAsJsonObject();
                UnifiedIssue issue = UnifiedIssue.builder()
                        .source(cn.dolphinmind.glossary.java.analyze.unified.IssueSource.AI)
                        .ruleKey(getStr(issueJson, "ruleKey", "AI_REVIEW"))
                        .ruleName(getStr(issueJson, "ruleName", "AI Review"))
                        .severity(getStr(issueJson, "severity", "MINOR"))
                        .category(getStr(issueJson, "category", "DESIGN"))
                        .filePath(getStr(issueJson, "filePath", ""))
                        .className(getStr(issueJson, "className", ""))
                        .methodName(getStr(issueJson, "methodName"))
                        .line(getInt(issueJson, "line", 0))
                        .message(getStr(issueJson, "message", ""))
                        .confidence(getDouble(issueJson, "confidence"))
                        .aiSuggestion(getStr(issueJson, "aiSuggestion"))
                        .aiFixedCode(getStr(issueJson, "aiFixedCode"))
                        .aiReasoning(getStr(issueJson, "aiReasoning"))
                        .build();
                issues.add(issue);
            }
            return issues;
        } catch (Exception e) {
            logger.warning("Failed to parse review response as JSON: " + e.getMessage());
            logger.warning("Response preview: " + content.substring(0, Math.min(300, content.length())));
            return Collections.emptyList();
        }
    }

    private String getStr(JsonObject json, String key) {
        return getStr(json, key, null);
    }
    private String getStr(JsonObject json, String key, String def) {
        JsonElement el = json.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : def;
    }
    private int getInt(JsonObject json, String key, int def) {
        JsonElement el = json.get(key);
        return el != null ? el.getAsInt() : def;
    }
    private Double getDouble(JsonObject json, String key) {
        JsonElement el = json.get(key);
        return el != null ? el.getAsDouble() : null;
    }
}
