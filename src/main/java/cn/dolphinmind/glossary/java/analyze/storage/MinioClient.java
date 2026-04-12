package cn.dolphinmind.glossary.java.analyze.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * MinIO 客户端 (轻量 HTTP API, 无需 MinIO SDK)
 * 
 * 通过 S3-compatible HTTP API 与 MinIO 交互
 */
public class MinioClient {

    private static final Logger logger = Logger.getLogger(MinioClient.class.getName());

    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private final Gson gson;

    public MinioClient(String endpoint, String accessKey, String secretKey) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * 上传 JSON 对象到 MinIO
     */
    public boolean uploadJson(String bucket, String objectPath, Map<String, Object> data) {
        try {
            String json = gson.toJson(data);
            return uploadString(bucket, objectPath, json, "application/json");
        } catch (Exception e) {
            logger.warning("MinIO upload failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 上传字符串到 MinIO
     */
    public boolean uploadString(String bucket, String objectPath, String content, String contentType) {
        try {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            return uploadBytes(bucket, objectPath, bytes, contentType);
        } catch (Exception e) {
            logger.warning("MinIO upload string failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 上传字节数组到 MinIO
     */
    public boolean uploadBytes(String bucket, String objectPath, byte[] data, String contentType) {
        try {
            URL url = new URL(endpoint + "/" + bucket + "/" + objectPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", contentType != null ? contentType : "application/octet-stream");
            conn.setRequestProperty("Content-Length", String.valueOf(data.length));
            conn.setRequestProperty("Authorization", buildAuthHeader("PUT", "/" + bucket + "/" + objectPath, data.length));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data);
            }

            int status = conn.getResponseCode();
            conn.disconnect();
            return status >= 200 && status < 300;
        } catch (Exception e) {
            logger.warning("MinIO upload bytes failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 下载字符串从 MinIO
     */
    public String downloadString(String bucket, String objectPath) throws IOException {
        URL url = new URL(endpoint + "/" + bucket + "/" + objectPath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", buildAuthHeader("GET", "/" + bucket + "/" + objectPath, 0));

        int status = conn.getResponseCode();
        if (status >= 400) {
            conn.disconnect();
            throw new IOException("MinIO download failed: HTTP " + status);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            conn.disconnect();
            return sb.toString();
        }
    }

    /**
     * 上传文件到 MinIO
     */
    public boolean uploadFile(String bucket, String objectPath, File file) {
        try {
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
            String contentType = getContentType(file.getName());
            return uploadBytes(bucket, objectPath, data, contentType);
        } catch (Exception e) {
            logger.warning("MinIO upload file failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取对象的 URL (用于公开访问)
     */
    public String getObjectUrl(String bucket, String objectPath) {
        return endpoint + "/" + bucket + "/" + objectPath;
    }

    /**
     * 构建 review 站点的完整路径
     */
    public String buildReviewPath(String project, String commit, String subPath) {
        return "reviews/" + project + "/" + commit + "/" + (subPath != null ? subPath : "");
    }

    private String buildAuthHeader(String method, String path, long contentLength) {
        // TODO: Implement AWS Signature V4 for production
        return "";
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".zip")) return "application/zip";
        return "application/octet-stream";
    }
}
