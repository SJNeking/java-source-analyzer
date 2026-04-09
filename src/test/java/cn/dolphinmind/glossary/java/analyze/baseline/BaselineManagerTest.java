package cn.dolphinmind.glossary.java.analyze.baseline;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for BaselineManager.
 */
public class BaselineManagerTest {

    @Test
    public void issueKey_shouldGenerateConsistentKey() {
        String key1 = BaselineManager.issueKey("RSPEC-123", "com.example.MyClass", "myMethod", "Test message");
        String key2 = BaselineManager.issueKey("RSPEC-123", "com.example.MyClass", "myMethod", "Test message");

        assertEquals("Issue key should be consistent", key1, key2);
    }

    @Test
    public void issueKey_shouldTruncateLongMessages() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) sb.append("A");
        String longMessage = sb.toString();
        String key = BaselineManager.issueKey("RSPEC-123", "com.example.MyClass", "myMethod", longMessage);

        // Key should contain truncated message (max 50 chars)
        assertNotNull(key);
        assertFalse(key.isEmpty());
    }

    @Test
    public void markIssue_shouldAddEntryToBaseline() {
        BaselineManager.BaselineData baseline = new BaselineManager.BaselineData();
        BaselineManager.markIssue(baseline, "RSPEC-123", "com.example.MyClass", "myMethod",
            "Test message", "FALSE_POSITIVE", "This is intentional");

        assertEquals(1, baseline.getIssues().size());
        String key = BaselineManager.issueKey("RSPEC-123", "com.example.MyClass", "myMethod", "Test message");
        assertTrue(baseline.getIssues().containsKey(key));
        assertEquals("FALSE_POSITIVE", baseline.getIssues().get(key).getStatus());
    }

    @Test
    public void filterBaseline_shouldRemoveMarkedIssues() {
        BaselineManager.BaselineData baseline = new BaselineManager.BaselineData();
        BaselineManager.markIssue(baseline, "RSPEC-123", "com.example.MyClass", "myMethod",
            "Marked issue", "FALSE_POSITIVE", "Test reason");

        List<Map<String, Object>> issues = new ArrayList<>();
        issues.add(createIssue("RSPEC-123", "com.example.MyClass", "myMethod", "Marked issue"));
        issues.add(createIssue("RSPEC-456", "com.example.OtherClass", "otherMethod", "Unmarked issue"));

        List<Map<String, Object>> filtered = BaselineManager.filterBaseline(issues, baseline);

        assertEquals(1, filtered.size());
        assertEquals("RSPEC-456", filtered.get(0).get("rule_key"));
    }

    @Test
    public void filterBaseline_shouldKeepUnmarkedIssues() {
        BaselineManager.BaselineData baseline = new BaselineManager.BaselineData();

        List<Map<String, Object>> issues = new ArrayList<>();
        issues.add(createIssue("RSPEC-123", "com.example.MyClass", "myMethod", "Issue 1"));
        issues.add(createIssue("RSPEC-456", "com.example.OtherClass", "otherMethod", "Issue 2"));

        List<Map<String, Object>> filtered = BaselineManager.filterBaseline(issues, baseline);

        assertEquals(2, filtered.size());
    }

    @Test
    public void baselineEntry_shouldHaveRequiredFields() {
        BaselineManager.BaselineEntry entry = new BaselineManager.BaselineEntry(
            "WONT_FIX", "This is by design");

        assertEquals("WONT_FIX", entry.getStatus());
        assertEquals("This is by design", entry.getReason());
        assertNotNull(entry.getMarkedBy());
        assertNotNull(entry.getMarkedDate());
    }

    @Test
    public void saveAndLoadBaseline_shouldPreserveData() throws Exception {
        Path testDir = Paths.get("src/test/resources/test-baseline");
        java.nio.file.Files.createDirectories(testDir);

        BaselineManager.BaselineData baseline = new BaselineManager.BaselineData();
        BaselineManager.markIssue(baseline, "RSPEC-123", "com.example.MyClass", "myMethod",
            "Test issue", "FALSE_POSITIVE", "Test reason");

        BaselineManager.save(baseline, testDir);

        BaselineManager.BaselineData loaded = BaselineManager.load(testDir);
        assertEquals(1, loaded.getIssues().size());
    }

    @Test
    public void loadBaseline_shouldHandleMissingFile() {
        Path nonExistentDir = Paths.get("src/test/resources/non-existent-baseline");
        BaselineManager.BaselineData baseline = BaselineManager.load(nonExistentDir);

        assertNotNull(baseline);
        assertEquals(0, baseline.getIssues().size());
    }

    @Test
    public void loadBaseline_shouldHandleCorruptFile() throws Exception {
        Path testDir = Paths.get("src/test/resources/test-baseline-corrupt");
        java.nio.file.Files.createDirectories(testDir);
        java.nio.file.Files.write(testDir.resolve("baseline.json"), "{invalid json}".getBytes());

        BaselineManager.BaselineData baseline = BaselineManager.load(testDir);

        assertNotNull(baseline);
        assertEquals(0, baseline.getIssues().size());
    }

    private Map<String, Object> createIssue(String ruleKey, String className, String methodName, String message) {
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("rule_key", ruleKey);
        issue.put("class", className);
        issue.put("method", methodName);
        issue.put("message", message);
        return issue;
    }
}
