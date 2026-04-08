package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Database Rules - P5 Priority
 *
 * Detects database anti-patterns:
 * - SELECT * usage
 * - Hardcoded connection URLs
 * - Missing transaction commits
 * - Inefficient queries (N+1)
 */
public final class DatabaseRules {
    private DatabaseRules() {}

    // =====================================================================
    // SQL Injection & Security
    // =====================================================================

    /**
     * RSPEC-2077: SQL Injection (already covered in AllRules, adding variations)
     */
    public static class HardcodedDatabaseCredentials extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2078-DB"; }
        public String getName() { return "Database credentials should not be hardcoded"; }
        public String getCategory() { return "DATABASE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (Pattern.compile("jdbc:[^\\s\"]+:[^\\s\"]+/[^\\s\"]+:[^\\s\"]+").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.CRITICAL, getCategory(), fp, cn, name, line,
                    "Hardcoded database credentials", "jdbc url with credentials"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Query Patterns
    // =====================================================================

    /**
     * RSPEC-2079: SELECT * should not be used
     * Detects SELECT * usage in SQL strings.
     */
    public static class SelectStarUsage extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2079"; }
        public String getName() { return "SELECT * should not be used"; }
        public String getCategory() { return "DATABASE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (Pattern.compile("SELECT\\s+\\*|select\\s+\\*").matcher(body).find()) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "SELECT * used instead of explicit columns", "SELECT *"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-2080: SQL query in loop
     * Detects SQL execution inside loops.
     */
    public static class SqlInLoop extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2080"; }
        public String getName() { return "SQL queries should not be executed in loops"; }
        public String getCategory() { return "DATABASE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean hasLoop = body.contains("for (") || body.contains("while (");
            boolean hasSql = body.contains("executeQuery(") || body.contains("executeUpdate(") ||
                body.contains(".find(") || body.contains(".findOne(");

            if (hasLoop && hasSql) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "SQL query executed in loop", "query inside loop"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Resource Management
    // =====================================================================

    /**
     * RSPEC-2081: ResultSet not closed
     * Detects ResultSet usage without closing.
     */
    public static class ResultSetNotClosed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2081"; }
        public String getName() { return "ResultSet should be closed"; }
        public String getCategory() { return "DATABASE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean hasResultSet = body.contains("ResultSet") && body.contains("executeQuery");
            boolean hasClose = body.contains(".close()") || body.contains("try (");

            if (hasResultSet && !hasClose) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "ResultSet not closed", "resource leak"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-2082: Statement not closed
     * Detects Statement usage without closing.
     */
    public static class StatementNotClosed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2082"; }
        public String getName() { return "Statement should be closed"; }
        public String getCategory() { return "DATABASE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean hasStatement = body.contains("Statement") && (body.contains("createStatement") || body.contains("prepareStatement"));
            boolean hasClose = body.contains(".close()") || body.contains("try (");

            if (hasStatement && !hasClose) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Statement not closed", "resource leak"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Connection Management
    // =====================================================================

    /**
     * RSPEC-2083: Connection not closed
     * Detects Connection usage without closing.
     */
    public static class ConnectionNotClosed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2083"; }
        public String getName() { return "Database connection should be closed"; }
        public String getCategory() { return "DATABASE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            boolean hasConnection = body.contains("Connection") && (body.contains("DriverManager.getConnection") || body.contains(".getConnection()"));
            boolean hasClose = body.contains(".close()") || body.contains("try (");

            if (hasConnection && !hasClose) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Connection not closed", "connection leak"));
            }
            return issues;
        }
    }

    // =====================================================================
    // ORM/Hibernate Specific
    // =====================================================================

    /**
     * RSPEC-2084: EntityManager not closed
     * Detects EntityManager usage without closing.
     */
    public static class EntityManagerNotClosed extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2084"; }
        public String getName() { return "EntityManager should be closed"; }
        public String getCategory() { return "DATABASE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("EntityManager") && body.contains("createEntityManager") && !body.contains(".close()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "EntityManager not closed", "resource leak"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-2085: Transaction not committed
     * Detects transaction started but not committed.
     */
    public static class TransactionNotCommitted extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-2085"; }
        public String getName() { return "Transaction should be committed"; }
        public String getCategory() { return "DATABASE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("transaction.begin()") || body.contains("beginTransaction()")) {
                if (!body.contains("commit()") && !body.contains("TransactionManager")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Transaction started but not committed", "missing commit"));
                }
            }
            return issues;
        }
    }
}
