package cn.dolphinmind.glossary.java.analyze.storage;

import java.util.logging.Logger;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.net.Socket;

/**
 * 统一存储服务
 * 
 * 协调 PostgreSQL + Redis + MinIO 三个存储后端
 * 
 * 功能:
 * 1. 语义指纹缓存 (Redis) — 跳审机制
 * 2. 分析结果存储 (PostgreSQL + MinIO) — 统一报告持久化
 * 3. 代码快照 (MinIO) — 审查时点的代码快照
 * 4. 审查报告 URL (MinIO) — 前端站点托管
 */
public class StorageService {
    private static final Logger logger = Logger.getLogger(StorageService.class.getName());

    // Redis 键前缀
    private static final String REDIS_FP_PREFIX = "fp:";
    private static final String REDIS_ANALYSIS_PREFIX = "analysis:";
    private static final String REDIS_GATE_PREFIX = "gate:";
    private static final String REDIS_LOCK_PREFIX = "lock:analysis:";
    private static final String REDIS_RATE_LIMIT_PREFIX = "ratelimit:ai:";

    // MinIO bucket
    private static final String BUCKET = "codeguardian";

    private final RedisClient redis;
    private final MinioClient minio;
    private final String projectDir;  // 本地项目根

    public StorageService(RedisClient redis, MinioClient minio, String projectDir) {
        this.redis = redis;
        this.minio = minio;
        this.projectDir = projectDir;
    }

    // ============================================================
    // 1. 语义指纹缓存 (跳审)
    // ============================================================

    /**
     * 检查指纹是否命中缓存
     * @return 缓存的分析结果 ID, 未命中返回 -1
     */
    public long checkFingerprint(String fingerprint) {
        String key = REDIS_FP_PREFIX + fingerprint;
        String cached = redis.get(key);
        if (cached == null) return -1;

        // 命中: 增加计数
        redis.incr(key);
        return parseResultId(cached);
    }

    /**
     * 设置指纹缓存
     * @param ttlDays 过期天数
     */
    public void setFingerprint(String fingerprint, long resultId, int ttlDays) {
        String key = REDIS_FP_PREFIX + fingerprint;
        redis.setex(key, ttlDays * 86400, "{\"result_id\":" + resultId + ",\"hit_count\":0}");
    }

    /**
     * 获取指纹统计
     */
    public Map<String, Object> getFingerprintStats() {
        // 扫描所有指纹键 (生产环境应使用 SCAN 而非 KEYS)
        List<String> keys = redis.keys(REDIS_FP_PREFIX + "*");
        int total = keys.size();
        int hits = 0;
        int totalHits = 0;

        for (String key : keys) {
            String val = redis.get(key);
            if (val != null && val.contains("\"hit_count\":") && !val.contains("\"hit_count\":0")) {
                hits++;
                totalHits++; // 简化统计
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_fingerprints", total);
        stats.put("hit_fingerprints", hits);
        stats.put("total_hits", totalHits);
        return stats;
    }

    // ============================================================
    // 2. 分析结果存储
    // ============================================================

    /**
     * 保存分析结果到 MinIO + 本地缓存
     */
    public String saveAnalysisResult(String project, String commit, String branch,
                                     Map<String, Object> report, boolean isUnified) throws IOException {
        String date = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String objectPath = "analysis/" + project + "/" + commit + "/";

        if (isUnified) {
            objectPath += "unified-report.json";
        } else {
            objectPath += "static-results.json";
        }

        // 上传到 MinIO
        minio.uploadJson(BUCKET, objectPath, report);

        // 也保存到本地 data 目录 (兼容现有前端)
        String localPath = projectDir + "/html/data/" + (isUnified ? "unified-report.json" : "static-results.json");
        Files.write(Paths.get(localPath), 
                new com.google.gson.GsonBuilder().setPrettyPrinting().create()
                        .toJson(report).getBytes(StandardCharsets.UTF_8));

        // 缓存分析状态到 Redis
        String analysisKey = REDIS_ANALYSIS_PREFIX + project + ":" + commit;
        redis.setex(analysisKey, 90 * 86400, 
                "{\"status\":\"completed\",\"issues\":" + report.getOrDefault("totalIssues", 0) + "}");

        return minio.getObjectUrl(BUCKET, objectPath);
    }

    // ============================================================
    // 3. 代码快照
    // ============================================================

    /**
     * 上传代码快照到 MinIO
     */
    public boolean uploadCodeSnapshot(String project, String commit, File zipFile) {
        String objectPath = "snapshots/" + project + "/" + commit + "/source-code.zip";
        return minio.uploadFile(BUCKET, objectPath, zipFile);
    }

    /**
     * 获取快照下载 URL
     */
    public String getSnapshotUrl(String project, String commit) {
        return minio.getObjectUrl(BUCKET, "snapshots/" + project + "/" + commit + "/source-code.zip");
    }

    // ============================================================
    // 4. 审查报告前端站点
    // ============================================================

    /**
     * 部署审查报告前端站点到 MinIO
     */
    public boolean deployReviewSite(String project, String commit, String siteDir) throws IOException {
        String basePath = "reviews/" + project + "/" + commit + "/";
        
        // 遍历目录上传所有文件
        Path sitePath = Paths.get(siteDir);
        if (!Files.isDirectory(sitePath)) return false;

        int uploaded = 0;
        try (java.util.stream.Stream<Path> walk = Files.walk(sitePath)) {
            for (Path file : walk.filter(Files::isRegularFile).toArray(Path[]::new)) {
                String relative = sitePath.relativize(file).toString().replace('\\', '/');
                String objectPath = basePath + relative;
                String contentType = getContentType(relative);
                
                byte[] data = Files.readAllBytes(file);
                minio.uploadBytes(BUCKET, objectPath, data, contentType);
                uploaded++;
            }
        }

        // 缓存报告状态
        String reportKey = REDIS_ANALYSIS_PREFIX + project + ":review:" + commit;
        redis.setex(reportKey, 90 * 86400, 
                "{\"status\":\"deployed\",\"url\":\"" + minio.getObjectUrl(BUCKET, basePath + "views/index.html") + "\"}");

        return uploaded > 0;
    }

    /**
     * 获取审查报告 URL
     */
    public String getReviewUrl(String project, String commit) {
        return minio.getObjectUrl(BUCKET, "reviews/" + project + "/" + commit + "/views/index.html");
    }

    // ============================================================
    // 5. 质量门禁
    // ============================================================

    /**
     * 更新质量门禁状态
     */
    public void updateQualityGate(String project, String branch, boolean passed, int critical, int major) {
        String key = REDIS_GATE_PREFIX + project + ":" + branch;
        String value = "{\"passed\":" + passed + 
                ",\"critical\":" + critical + 
                ",\"major\":" + major + 
                ",\"updated_at\":\"" + new Date() + "\"}";
        redis.set(key, value);
    }

    /**
     * 查询质量门禁状态
     */
    public String getQualityGate(String project, String branch) {
        return redis.get(REDIS_GATE_PREFIX + project + ":" + branch);
    }

    // ============================================================
    // 6. 分布式锁
    // ============================================================

    /**
     * 获取分析锁 (防止并发分析冲突)
     * @param ttlSeconds 锁超时时间
     * @return 是否成功获取
     */
    public boolean acquireAnalysisLock(String project, String workerId, int ttlSeconds) {
        String key = REDIS_LOCK_PREFIX + project;
        String value = workerId + ":" + System.currentTimeMillis();
        return redis.setnx(key, value, ttlSeconds);
    }

    /**
     * 释放分析锁
     */
    public void releaseAnalysisLock(String project, String workerId) {
        String key = REDIS_LOCK_PREFIX + project;
        String val = redis.get(key);
        if (val != null && val.startsWith(workerId + ":")) {
            redis.del(key);
        }
    }

    // ============================================================
    // 7. AI 速率限制
    // ============================================================

    /**
     * 检查 AI API 速率限制
     * @return true = 可以继续请求, false = 需要等待
     */
    public boolean checkAiRateLimit(String model, String project, int maxPerMinute) {
        String key = REDIS_RATE_LIMIT_PREFIX + model + ":" + project;
        long count = redis.incr(key);
        if (count == 1) {
            redis.expire(key, 60);  // 60 秒窗口
        }
        return count <= maxPerMinute;
    }

    // ============================================================
    // 内部方法
    // ============================================================

    private long parseResultId(String json) {
        try {
            int idx = json.indexOf("\"result_id\":");
            if (idx >= 0) {
                int start = idx + 12;
                int end = json.indexOf(",", start);
                if (end < 0) end = json.indexOf("}", start);
                if (end > start) return Long.parseLong(json.substring(start, end).trim());
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        if (fileName.endsWith(".map")) return "application/json";
        return "application/octet-stream";
    }

    // ============================================================
    // Redis Client (简易实现, 基于 HTTP 或 socket)
    // ============================================================

    /**
     * 简易 Redis 客户端 (通过 TCP socket 协议)
     * 生产环境建议使用 Lettuce/Jedis
     */
    /**
     * @deprecated Use Jedis or Lettuce for production.
     *             This is a minimal RESP implementation for CLI-only use.
     */
    @Deprecated
    public static class RedisClient {
        private final String host;
        private final int port;
        private final String password;

        public RedisClient(String host, int port, String password) {
            this.host = host;
            this.port = port;
            this.password = password;
        }

        public String get(String key) {
            return execute("GET", key);
        }

        public void set(String key, String value) {
            execute("SET", key, value);
        }

        public void setex(String key, int ttlSeconds, String value) {
            execute("SETEX", key, String.valueOf(ttlSeconds), value);
        }

        public boolean setnx(String key, String value, int ttlSeconds) {
            String result = execute("SET", key, value, "NX", "EX", String.valueOf(ttlSeconds));
            return result != null && result.equalsIgnoreCase("OK");
        }

        public long incr(String key) {
            String result = execute("INCR", key);
            try { return Long.parseLong(result); } catch (Exception e) { return 0; }
        }

        public boolean expire(String key, int seconds) {
            execute("EXPIRE", key, String.valueOf(seconds));
            return true;
        }

        public long del(String key) {
            String result = execute("DEL", key);
            try { return Long.parseLong(result); } catch (Exception e) { return 0; }
        }

        public List<String> keys(String pattern) {
            String result = execute("KEYS", pattern);
            List<String> keys = new ArrayList<>();
            if (result != null) {
                String[] parts = result.split("\\s+");
                for (String p : parts) if (!p.isEmpty()) keys.add(p);
            }
            return keys;
        }

        private String execute(String... args) {
            // 简易 RESP 协议实现
            try (Socket socket = new Socket(host, port);
                 OutputStream os = socket.getOutputStream();
                 InputStream is = socket.getInputStream()) {

                // 发送命令
                StringBuilder sb = new StringBuilder();
                sb.append("*").append(args.length).append("\r\n");
                for (String arg : args) {
                    byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
                    sb.append("$").append(bytes.length).append("\r\n");
                    sb.append(arg).append("\r\n");
                }
                os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();

                // 读取响应 (简化版, 仅处理 simple string / integer / bulk string)
                byte[] buffer = new byte[4096];
                int n = is.read(buffer);
                if (n <= 0) return null;
                
                String response = new String(buffer, 0, n, StandardCharsets.UTF_8).trim();
                if (response.startsWith("+")) return response.substring(1);
                if (response.startsWith(":")) return response.substring(1);
                if (response.startsWith("$")) {
                    int len = Integer.parseInt(response.substring(1, response.indexOf("\r\n")));
                    if (len < 0) return null;
                    int dataStart = response.indexOf("\r\n") + 2;
                    return response.substring(dataStart, dataStart + len);
                }
                if (response.startsWith("*")) return response; // 数组, 简化返回
                
                return response;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return null;
            }
        }
    }
}
