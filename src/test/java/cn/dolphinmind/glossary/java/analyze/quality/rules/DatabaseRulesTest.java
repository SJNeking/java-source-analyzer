package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for DATABASE category rules.
 */
public class DatabaseRulesTest extends AbstractRuleTest {

    @Test
    public void selectStarUsage_shouldDetect() {
        DatabaseRules.SelectStarUsage rule = new DatabaseRules.SelectStarUsage();
        Map<String, Object> method = createMethod("getAll", "ResultSet rs = stmt.executeQuery(\"SELECT * FROM users\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void sqlInLoop_shouldDetect() {
        DatabaseRules.SqlInLoop rule = new DatabaseRules.SqlInLoop();
        Map<String, Object> method = createMethod("process", "for (String id : ids) {\n" +
            "    stmt.executeQuery(\"SELECT * FROM users WHERE id = \" + id);\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void resultSetNotClosed_shouldDetect() {
        DatabaseRules.ResultSetNotClosed rule = new DatabaseRules.ResultSetNotClosed();
        Map<String, Object> method = createMethod("query", "ResultSet rs = stmt.executeQuery(\"SELECT ...\");\n" +
            "while (rs.next()) {}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void statementNotClosed_shouldDetect() {
        DatabaseRules.StatementNotClosed rule = new DatabaseRules.StatementNotClosed();
        Map<String, Object> method = createMethod("query", "Statement stmt = conn.createStatement();\n" +
            "stmt.executeQuery(\"SELECT ...\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void connectionNotClosed_shouldDetect() {
        DatabaseRules.ConnectionNotClosed rule = new DatabaseRules.ConnectionNotClosed();
        Map<String, Object> method = createMethod("query", "Connection conn = DriverManager.getConnection(url);\n" +
            "Statement stmt = conn.createStatement();\n");
        assertIssues(rule, method, 1);
    }
}
