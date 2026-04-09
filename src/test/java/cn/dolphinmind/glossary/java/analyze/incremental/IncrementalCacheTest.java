package cn.dolphinmind.glossary.java.analyze.incremental;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

import static org.junit.Assert.*;

/**
 * Tests for IncrementalCache.
 */
public class IncrementalCacheTest {

    private static final String TEST_VERSION = "1.0-test";

    @Test
    public void computeFileHash_shouldReturnValidHash() throws Exception {
        Path testFile = Paths.get("src/test/resources/test-file.txt");
        // Create test file if not exists
        java.nio.file.Files.createDirectories(testFile.getParent());
        java.nio.file.Files.write(testFile, "test content".getBytes());

        String hash = IncrementalCache.computeFileHash(testFile);
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 produces 64 hex chars
    }

    @Test
    public void computeFileHash_shouldBeDeterministic() throws Exception {
        Path testFile = Paths.get("src/test/resources/test-file.txt");
        java.nio.file.Files.write(testFile, "deterministic content".getBytes());

        String hash1 = IncrementalCache.computeFileHash(testFile);
        String hash2 = IncrementalCache.computeFileHash(testFile);
        assertEquals("Hash should be deterministic", hash1, hash2);
    }

    @Test
    public void computeFileHash_shouldChangeWithContent() throws Exception {
        Path testFile = Paths.get("src/test/resources/test-file.txt");

        java.nio.file.Files.write(testFile, "content version 1".getBytes());
        String hash1 = IncrementalCache.computeFileHash(testFile);

        java.nio.file.Files.write(testFile, "content version 2".getBytes());
        String hash2 = IncrementalCache.computeFileHash(testFile);

        assertNotEquals("Hash should change with content", hash1, hash2);
    }

    @Test
    public void cacheData_shouldIncludeAnalyzerVersion() {
        IncrementalCache.CacheData cache = new IncrementalCache.CacheData();
        cache.setAnalyzerVersion(TEST_VERSION);

        assertEquals(TEST_VERSION, cache.getAnalyzerVersion());
    }

    @Test
    public void load_shouldReturnFreshCacheWhenVersionMismatch() throws Exception {
        Path cacheDir = Paths.get("src/test/resources/test-cache");
        java.nio.file.Files.createDirectories(cacheDir);

        // Write old version cache
        String cacheContent = "{\n" +
            "  \"scanDate\": \"2024-01-01T00:00:00\",\n" +
            "  \"analyzerVersion\": \"0.9-old\",\n" +
            "  \"files\": {\"test.java\": {\"hash\": \"abc123\", \"classCount\": 1}},\n" +
            "  \"totalClasses\": 1\n" +
            "}";
        java.nio.file.Files.write(cacheDir.resolve("scan-cache.json"), cacheContent.getBytes());

        // Load with new version - should invalidate
        IncrementalCache.CacheData cache = IncrementalCache.load(cacheDir, "1.0-new");

        assertNotNull(cache);
        assertEquals("1.0-new", cache.getAnalyzerVersion());
        assertTrue("Old cache should be invalidated", cache.getFiles().isEmpty());
    }

    @Test
    public void load_shouldReturnCachedDataWhenVersionMatches() throws Exception {
        Path cacheDir = Paths.get("src/test/resources/test-cache");
        java.nio.file.Files.createDirectories(cacheDir);

        String cacheContent = "{\n" +
            "  \"scanDate\": \"2024-01-01T00:00:00\",\n" +
            "  \"analyzerVersion\": \"1.0-matching\",\n" +
            "  \"files\": {\"test.java\": {\"hash\": \"abc123\", \"classCount\": 1}},\n" +
            "  \"totalClasses\": 1\n" +
            "}";
        java.nio.file.Files.write(cacheDir.resolve("scan-cache.json"), cacheContent.getBytes());

        IncrementalCache.CacheData cache = IncrementalCache.load(cacheDir, "1.0-matching");

        assertNotNull(cache);
        assertEquals(1, cache.getTotalClasses());
        assertEquals(1, cache.getFiles().size());
    }

    @Test
    public void save_shouldPersistCache() throws Exception {
        Path cacheDir = Paths.get("src/test/resources/test-cache-save");
        java.nio.file.Files.createDirectories(cacheDir);

        IncrementalCache.CacheData cache = new IncrementalCache.CacheData();
        cache.setScanDate("2024-01-01T00:00:00");
        cache.setAnalyzerVersion(TEST_VERSION);
        cache.setTotalClasses(5);

        IncrementalCache.FileEntry entry = new IncrementalCache.FileEntry("hash123");
        entry.setClassCount(2);
        cache.getFiles().put("Test.java", entry);

        IncrementalCache.save(cache, cacheDir);

        // Verify file exists
        assertTrue(java.nio.file.Files.exists(cacheDir.resolve("scan-cache.json")));

        // Verify content
        IncrementalCache.CacheData loaded = IncrementalCache.load(cacheDir, TEST_VERSION);
        assertEquals(5, loaded.getTotalClasses());
        assertEquals(1, loaded.getFiles().size());
        assertEquals("hash123", loaded.getFiles().get("Test.java").getHash());
    }

    @Test
    public void findChangedFiles_shouldDetectChanges() throws Exception {
        Path cacheDir = Paths.get("src/test/resources/test-cache-changes");
        java.nio.file.Files.createDirectories(cacheDir);
        Path testFile = Paths.get("src/test/resources/test-change.java");
        java.nio.file.Files.createDirectories(testFile.getParent());
        java.nio.file.Files.write(testFile, "initial content".getBytes());

        // Create cache with initial hash
        IncrementalCache.CacheData cache = new IncrementalCache.CacheData();
        cache.setAnalyzerVersion(TEST_VERSION);
        String initialHash = IncrementalCache.computeFileHash(testFile);
        IncrementalCache.FileEntry entry = new IncrementalCache.FileEntry(initialHash);
        cache.getFiles().put("test-change.java", entry);

        // No changes - should return empty list
        java.util.List<java.nio.file.Path> changed = IncrementalCache.findChangedFiles(
            testFile.getParent(), cache, java.util.Arrays.asList(testFile));
        assertEquals(0, changed.size());

        // Change file - should detect change
        java.nio.file.Files.write(testFile, "changed content".getBytes());
        changed = IncrementalCache.findChangedFiles(testFile.getParent(), cache, java.util.Arrays.asList(testFile));
        assertEquals(1, changed.size());
    }
}
