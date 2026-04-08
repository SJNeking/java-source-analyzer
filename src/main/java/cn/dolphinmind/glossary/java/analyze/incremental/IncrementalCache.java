package cn.dolphinmind.glossary.java.analyze.incremental;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Incremental analysis: cache scan results per file based on content hash.
 *
 * Only re-scan files that have changed since last scan.
 * Returns a list of files that need scanning (changed or new).
 *
 * Cache format (JSON):
 * {
 *   "scan_date": "2026-04-09T12:00:00",
 *   "files": {
 *     "com/example/MyClass.java": {
 *       "hash": "abc123...",
 *       "class_count": 2,
 *       "method_count": 15
 *     }
 *   },
 *   "total_classes": 100,
 *   "total_methods": 500
 * }
 */
public class IncrementalCache {

    public static class FileEntry {
        private String hash;
        private int classCount;
        private int methodCount;
        private int fieldCount;

        public FileEntry() {}
        public FileEntry(String hash) { this.hash = hash; }

        public String getHash() { return hash; }
        public void setHash(String hash) { this.hash = hash; }
        public int getClassCount() { return classCount; }
        public void setClassCount(int classCount) { this.classCount = classCount; }
        public int getMethodCount() { return methodCount; }
        public void setMethodCount(int methodCount) { this.methodCount = methodCount; }
        public int getFieldCount() { return fieldCount; }
        public void setFieldCount(int fieldCount) { this.fieldCount = fieldCount; }
    }

    public static class CacheData {
        private String scanDate;
        private final Map<String, FileEntry> files = new LinkedHashMap<>();
        private int totalClasses;
        private int totalMethods;
        private int totalFields;

        public String getScanDate() { return scanDate; }
        public void setScanDate(String scanDate) { this.scanDate = scanDate; }
        public Map<String, FileEntry> getFiles() { return files; }
        public int getTotalClasses() { return totalClasses; }
        public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }
        public int getTotalMethods() { return totalMethods; }
        public void setTotalMethods(int totalMethods) { this.totalMethods = totalMethods; }
        public int getTotalFields() { return totalFields; }
        public void setTotalFields(int totalFields) { this.totalFields = totalFields; }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Load existing cache or create new one.
     */
    public static CacheData load(Path cacheDir) {
        Path cacheFile = cacheDir.resolve("scan-cache.json");
        if (Files.exists(cacheFile)) {
            try {
                String content = new String(Files.readAllBytes(cacheFile), StandardCharsets.UTF_8);
                return GSON.fromJson(content, CacheData.class);
            } catch (Exception e) {
                return new CacheData();
            }
        }
        CacheData data = new CacheData();
        data.setScanDate(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
        return data;
    }

    /**
     * Save cache to disk.
     */
    public static void save(CacheData cache, Path cacheDir) throws IOException {
        Files.createDirectories(cacheDir);
        Path cacheFile = cacheDir.resolve("scan-cache.json");
        String json = GSON.toJson(cache);
        Files.write(cacheFile, json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Compute SHA-256 hash of file content.
     */
    public static String computeFileHash(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(file);
        byte[] hashBytes = digest.digest(fileBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Find files that have changed or are new since last scan.
     * Returns list of (relativePath, absolutePath) pairs.
     */
    public static List<Path> findChangedFiles(Path projectRoot, CacheData cache, List<Path> javaFiles) throws Exception {
        List<Path> changedFiles = new ArrayList<>();

        for (Path file : javaFiles) {
            String relativePath = projectRoot.relativize(file).toString();
            String currentHash = computeFileHash(file);

            FileEntry cached = cache.getFiles().get(relativePath);
            if (cached == null || !cached.getHash().equals(currentHash)) {
                changedFiles.add(file);
            }
        }

        return changedFiles;
    }

    /**
     * Get summary of incremental scan.
     */
    public static String getIncrementalSummary(CacheData oldCache, int totalFiles, int changedFiles) {
        int cachedFiles = oldCache.getFiles().size();
        int skipped = totalFiles - changedFiles;
        return String.format("增量扫描: %d 总文件, %d 变更, %d 跳过缓存", totalFiles, changedFiles, skipped);
    }
}
