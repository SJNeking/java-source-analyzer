package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for RESOURCE category rules.
 */
public class ResourceManagementRulesTest extends AbstractRuleTest {

    @Test
    public void streamNotClosed_shouldDetect() {
        ResourceManagementRules.StreamNotClosed rule = new ResourceManagementRules.StreamNotClosed();
        Map<String, Object> method = createMethod("readFile", "FileInputStream fis = new FileInputStream(\"file.txt\");\n" +
            "fis.read();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void scannerNotClosed_shouldDetect() {
        ResourceManagementRules.ScannerNotClosed rule = new ResourceManagementRules.ScannerNotClosed();
        Map<String, Object> method = createMethod("read", "Scanner sc = new Scanner(System.in);\n" +
            "String line = sc.nextLine();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void threadPoolNotShutdown_shouldDetect() {
        ResourceManagementRules.ThreadPoolNotShutdown rule = new ResourceManagementRules.ThreadPoolNotShutdown();
        Map<String, Object> method = createMethod("execute", "ExecutorService exec = Executors.newFixedThreadPool(4);\n" +
            "exec.submit(() -> doWork());\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void jdbcConnectionNotClosed_shouldDetect() {
        ResourceManagementRules.JdbcConnectionNotClosed rule = new ResourceManagementRules.JdbcConnectionNotClosed();
        Map<String, Object> method = createMethod("query", "Connection conn = DriverManager.getConnection(url);\n" +
            "Statement stmt = conn.createStatement();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void zipFileNotClosed_shouldDetect() {
        ResourceManagementRules.ZipFileNotClosed rule = new ResourceManagementRules.ZipFileNotClosed();
        Map<String, Object> method = createMethod("extract", "ZipFile zf = new ZipFile(\"archive.zip\");\n" +
            "zf.entries();\n");
        assertIssues(rule, method, 1);
    }
}
