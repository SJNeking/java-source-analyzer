package cn.dolphinmind.glossary.java.analyze.quality;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for QualityIssue improvements:
 * - Precise location (start/end line/column)
 * - Code snippet
 * - Builder pattern
 * - equals/hashCode for dedup
 * - toMap includes new fields
 */
public class QualityIssueTest {

    @Test
    public void legacyConstructor_shouldSetDefaults() {
        QualityIssue issue = new QualityIssue(
            "RSPEC-108", "Empty catch block", Severity.MAJOR, "BUG",
            "Test.java", "com.example.Test", "testMethod", 10,
            "Empty catch block", "catch (...) {}"
        );

        assertEquals("RSPEC-108", issue.getRuleKey());
        assertEquals(10, issue.getStartLine());
        assertEquals(10, issue.getEndLine());
        assertEquals(1, issue.getStartColumn());
        assertEquals(0, issue.getEndColumn());
        assertNull(issue.getCodeSnippet());
        assertFalse(issue.hasPreciseLocation());
    }

    @Test
    public void fullConstructor_shouldSetPreciseLocation() {
        QualityIssue issue = new QualityIssue(
            "RSPEC-108", "Empty catch block", Severity.MAJOR, "BUG",
            "Test.java", "com.example.Test", "testMethod",
            10, 12, 5, 3,
            "Empty catch block", "catch (...) {}", "catch (Exception e) {}"
        );

        assertEquals(10, issue.getStartLine());
        assertEquals(12, issue.getEndLine());
        assertEquals(5, issue.getStartColumn());
        assertEquals(3, issue.getEndColumn());
        assertEquals("catch (Exception e) {}", issue.getCodeSnippet());
        assertTrue(issue.hasPreciseLocation());
    }

    @Test
    public void builder_shouldCreateIssueWithAllFields() {
        QualityIssue issue = new QualityIssue.Builder()
            .ruleKey("RSPEC-108")
            .ruleName("Empty catch blocks should be avoided")
            .severity(Severity.MAJOR)
            .category("BUG")
            .filePath("Test.java")
            .className("com.example.Test")
            .methodName("testMethod")
            .location(10, 12, 5, 3)
            .message("Empty catch block")
            .evidence("catch (...) {}")
            .codeSnippet("catch (Exception e) {}")
            .build();

        assertEquals("RSPEC-108", issue.getRuleKey());
        assertEquals("Empty catch blocks should be avoided", issue.getRuleName());
        assertEquals(Severity.MAJOR, issue.getSeverity());
        assertEquals(10, issue.getStartLine());
        assertEquals(12, issue.getEndLine());
        assertEquals(5, issue.getStartColumn());
        assertEquals(3, issue.getEndColumn());
        assertEquals("catch (Exception e) {}", issue.getCodeSnippet());
    }

    @Test
    public void equals_shouldIdentifyDuplicates() {
        QualityIssue issue1 = new QualityIssue.Builder()
            .ruleKey("RSPEC-108").severity(Severity.MAJOR).category("BUG")
            .filePath("Test.java").className("Test").methodName("m")
            .line(10).message("test").evidence("test").build();

        QualityIssue issue2 = new QualityIssue.Builder()
            .ruleKey("RSPEC-108").severity(Severity.CRITICAL).category("BUG")
            .filePath("Test.java").className("Test").methodName("m")
            .line(10).message("different message").evidence("different").build();

        // Same ruleKey + file + line + method = equal (even with different message/severity)
        assertEquals(issue1, issue2);
        assertEquals(issue1.hashCode(), issue2.hashCode());
    }

    @Test
    public void equals_shouldDifferForDifferentLines() {
        QualityIssue issue1 = new QualityIssue.Builder()
            .ruleKey("RSPEC-108").severity(Severity.MAJOR).category("BUG")
            .filePath("Test.java").className("Test").methodName("m")
            .line(10).message("test").evidence("test").build();

        QualityIssue issue2 = new QualityIssue.Builder()
            .ruleKey("RSPEC-108").severity(Severity.MAJOR).category("BUG")
            .filePath("Test.java").className("Test").methodName("m")
            .line(20).message("test").evidence("test").build();

        assertNotEquals(issue1, issue2);
    }

    @Test
    public void toMap_shouldIncludeNewFields() {
        QualityIssue issue = new QualityIssue.Builder()
            .ruleKey("RSPEC-108").severity(Severity.MAJOR).category("BUG")
            .filePath("Test.java").className("Test").methodName("m")
            .location(10, 12, 5, 3)
            .message("test").evidence("test").codeSnippet("code").build();

        Map<String, Object> map = issue.toMap();

        assertEquals(10, map.get("start_line"));
        assertEquals(12, map.get("end_line"));
        assertEquals(5, map.get("start_column"));
        assertEquals(3, map.get("end_column"));
        assertEquals("code", map.get("code_snippet"));
        assertEquals("test", map.get("message"));
    }

    @Test
    public void toString_shouldBeReadable() {
        QualityIssue issue = new QualityIssue.Builder()
            .ruleKey("RSPEC-108").severity(Severity.MAJOR).category("BUG")
            .filePath("Test.java").className("Test").methodName("m")
            .line(10).message("Empty catch").evidence("test").build();

        String str = issue.toString();
        assertTrue(str.contains("RSPEC-108"));
        assertTrue(str.contains("Test.java"));
        assertTrue(str.contains("10"));
        assertTrue(str.contains("Empty catch"));
    }
}
