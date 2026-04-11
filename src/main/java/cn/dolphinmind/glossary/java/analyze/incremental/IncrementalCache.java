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
        /** Cached class assets for this file (full data for merge) */
        private List<Map<String, Object>> cachedAssets;

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
        public List<Map<String, Object>> getCachedAssets() { return cachedAssets; }
        public void setCachedAssets(List<Map<String, Object>> cachedAssets) { this.cachedAssets = cachedAssets; }
    }

    public static class CacheData {
        private String scanDate;
        private String analyzerVersion;
        private final Map<String, FileEntry> files = new LinkedHashMap<>();
        private int totalClasses;
        private int totalMethods;
        private int totalFields;

        public String getScanDate() { return scanDate; }
        public void setScanDate(String scanDate) { this.scanDate = scanDate; }
        public String getAnalyzerVersion() { return analyzerVersion; }
        public void setAnalyzerVersion(String analyzerVersion) { this.analyzerVersion = analyzerVersion; }
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
     * If cache version doesn't match current analyzer version, returns fresh cache.
     */
    public static CacheData load(Path cacheDir, String currentAnalyzerVersion) {
        Path cacheFile = cacheDir.resolve("scan-cache.json");
        if (Files.exists(cacheFile)) {
            try {
                String content = new String(Files.readAllBytes(cacheFile), StandardCharsets.UTF_8);
                CacheData cached = GSON.fromJson(content, CacheData.class);
                
                // Check analyzer version - invalidate cache if version mismatch
                if (cached.getAnalyzerVersion() == null || !cached.getAnalyzerVersion().equals(currentAnalyzerVersion)) {
                    System.out.println("ℹ️  缓存版本不匹配 (cached: " + cached.getAnalyzerVersion() + 
                            ", current: " + currentAnalyzerVersion + ")，强制失效");
                    CacheData fresh = new CacheData();
                    fresh.setAnalyzerVersion(currentAnalyzerVersion);
                    return fresh;
                }
                
                return cached;
            } catch (Exception e) {
                CacheData fresh = new CacheData();
                fresh.setAnalyzerVersion(currentAnalyzerVersion);
                return fresh;
            }
        }
        CacheData data = new CacheData();
        data.setScanDate(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
        data.setAnalyzerVersion(currentAnalyzerVersion);
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

    /**
     * Merge cached assets with newly scanned assets.
     * For changed files: use new assets; for unchanged files: use cached assets.
     *
     * @param cache The current cache data
     * @param newAssets Newly scanned class assets (only from changed files)
     * @param changedFilePaths Set of relative paths that were changed (need to replace cached assets)
     * @return Merged list of all class assets (cached + new)
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> mergeCachedAssets(
            CacheData cache,
            List<Map<String, Object>> newAssets,
            Set<String> changedFilePaths) {

        List<Map<String, Object>> merged = new ArrayList<>();

        // 1. Add cached assets for unchanged files
        for (Map.Entry<String, FileEntry> entry : cache.getFiles().entrySet()) {
            String relativePath = entry.getKey();
            FileEntry fileEntry = entry.getValue();

            // If this file wasn't changed, use its cached assets
            if (!changedFilePaths.contains(relativePath) && fileEntry.getCachedAssets() != null) {
                merged.addAll(fileEntry.getCachedAssets());
            }
        }

        // 2. Add all new assets (from changed files)
        merged.addAll(newAssets);

        return merged;
    }

    /**
     * Update cache entry with newly scanned assets.
     * Stores the full asset list in the cache for future incremental merges.
     *
     * @param cache The current cache data
     * @param relativePath Relative path of the file being cached
     * @param fileHash SHA-256 hash of the file content
     * @param assets List of class assets extracted from this file
     */
    public static void updateCacheEntry(
            CacheData cache,
            String relativePath,
            String fileHash,
            List<Map<String, Object>> assets) {

        FileEntry entry = cache.getFiles().computeIfAbsent(relativePath, k -> new FileEntry());
        entry.setHash(fileHash);
        entry.setCachedAssets(new ArrayList<>(assets));

        // Update counts from assets
        int classCount = assets.size();
        int methodCount = 0;
        int fieldCount = 0;

        for (Map<String, Object> asset : assets) {
            Object methods = asset.get("methods");
            if (methods instanceof List) methodCount += ((List<?>) methods).size();

            Object fields = asset.get("fields_matrix");
            if (fields instanceof List) fieldCount += ((List<?>) fields).size();
        }

        entry.setClassCount(classCount);
        entry.setMethodCount(methodCount);
        entry.setFieldCount(fieldCount);
    }

    /**
     * Remove cache entries for deleted files.
     * Call this when a file no longer exists in the project.
     *
     * @param cache The current cache data
     * @param deletedFilePaths Set of relative paths that have been deleted
     * @return Number of entries removed
     */
    public static int removeDeletedEntries(CacheData cache, Set<String> deletedFilePaths) {
        int removed = 0;
        for (String path : deletedFilePaths) {
            if (cache.getFiles().remove(path) != null) {
                removed++;
            }
        }
        return removed;
    }

    /**
     * Get total asset count from cache (including cached assets).
     */
    public static int getTotalAssetCount(CacheData cache) {
        int total = 0;
        for (FileEntry entry : cache.getFiles().values()) {
            if (entry.getCachedAssets() != null) {
                total += entry.getCachedAssets().size();
            } else {
                total += entry.getClassCount(); // fallback to count-only
            }
        }
        return total;
    }
}
