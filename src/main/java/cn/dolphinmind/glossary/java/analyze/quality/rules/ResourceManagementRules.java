package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Resource Management Rules
 *
 * Detects resource management anti-patterns:
 * - Stream not closed
 * - Network resource not closed
 * - File handle leak
 * - Scanner not closed
 * - Connection pool not closed
 * - Thread pool not shutdown
 * - Timer not cancelled
 * - Process not destroyed
 */
public final class ResourceManagementRules {
    private ResourceManagementRules() {}

    /**
     * RSPEC-12001: Stream not closed
     */
    public static class StreamNotClosed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-12001"; }
        public String getName() { return "Stream should be closed"; }
        public String getCategory() { return "RESOURCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("FileInputStream") || body.contains("FileOutputStream") ||
                 body.contains("BufferedReader") || body.contains("BufferedWriter") ||
                 body.contains("ObjectInputStream") || body.contains("ObjectOutputStream")) &&
                !body.contains(".close()") && !body.contains("try (") && !body.contains("IOUtils.closeQuietly")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Stream opened but not closed", "resource leak"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-12002: Scanner not closed
     */
    public static class ScannerNotClosed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-12002"; }
        public String getName() { return "Scanner should be closed"; }
        public String getCategory() { return "RESOURCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new Scanner(") && !body.contains(".close()") && !body.contains("try (")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Scanner not closed", "resource leak"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-12003: Thread pool not shutdown
     */
    public static class ThreadPoolNotShutdown extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-12003"; }
        public String getName() { return "Thread pool should be shutdown"; }
        public String getCategory() { return "RESOURCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("ExecutorService") || body.contains("newFixedThreadPool") || body.contains("newCachedThreadPool")) &&
                !body.contains(".shutdown()") && !body.contains(".shutdownNow()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Thread pool not shutdown", "thread leak"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-12004: Timer not cancelled
     */
    public static class TimerNotCancelled extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-12004"; }
        public String getName() { return "Timer should be cancelled"; }
        public String getCategory() { return "RESOURCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new Timer(") || body.contains("Timer(") && body.contains("schedule")) {
                if (!body.contains(".cancel()") && !body.contains(".purge()")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Timer not cancelled", "timer leak"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-12005: Process not destroyed
     */
    public static class ProcessNotDestroyed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-12005"; }
        public String getName() { return "Process should be destroyed"; }
        public String getCategory() { return "RESOURCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("Runtime.getRuntime().exec(") || body.contains("ProcessBuilder")) {
                if (!body.contains(".destroy()") && !body.contains(".waitFor()")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Process not destroyed", "zombie process"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-12006: JDBC connection not closed
     */
    public static class JdbcConnectionNotClosed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-12006"; }
        public String getName() { return "JDBC connection should be closed"; }
        public String getCategory() { return "RESOURCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("DriverManager.getConnection(") || body.contains("DataSource.getConnection(")) {
                if (!body.contains(".close()") && !body.contains("try (")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                        "JDBC connection not closed", "connection pool exhaustion"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-12007: ZipFile not closed
     */
    public static class ZipFileNotClosed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-12007"; }
        public String getName() { return "ZipFile should be closed"; }
        public String getCategory() { return "RESOURCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new ZipFile(") || body.contains("ZipInputStream")) {
                if (!body.contains(".close()") && !body.contains("try (")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "ZipFile/ZipInputStream not closed", "resource leak"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-12008: HttpURLConnection not disconnected
     */
    public static class HttpConnectionNotDisconnected extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-12008"; }
        public String getName() { return "HttpURLConnection should be disconnected"; }
        public String getCategory() { return "RESOURCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("HttpURLConnection") && body.contains("openConnection(")) {
                if (!body.contains(".disconnect()") && !body.contains(".close()")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "HttpURLConnection not disconnected", "connection leak"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-12009: Writer not flushed
     */
    public static class WriterNotFlushed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-12009"; }
        public String getName() { return "Writer should be flushed before closing"; }
        public String getCategory() { return "RESOURCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("BufferedWriter") || body.contains("PrintWriter")) {
                if (!body.contains(".flush()") && !body.contains("autoFlush")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Writer not flushed", "data loss"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-12010: RandomAccessFile not closed
     */
    public static class RandomAccessFileNotClosed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-12010"; }
        public String getName() { return "RandomAccessFile should be closed"; }
        public String getCategory() { return "RESOURCE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new RandomAccessFile(") && !body.contains(".close()") && !body.contains("try (")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "RandomAccessFile not closed", "file handle leak"));
            }
            return issues;
        }
    }
}
