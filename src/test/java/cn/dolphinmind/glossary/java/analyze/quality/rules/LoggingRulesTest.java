package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for LOGGING category rules.
 */
public class LoggingRulesTest extends AbstractRuleTest {

    @Test
    public void usingSystemOut_shouldDetect() {
        LoggingRules.UsingSystemOut rule = new LoggingRules.UsingSystemOut();
        Map<String, Object> method = createMethod("debug", "System.out.println(\"Debug info\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void stringConcatInLog_shouldDetect() {
        LoggingRules.StringConcatInLog rule = new LoggingRules.StringConcatInLog();
        Map<String, Object> method = createMethod("log", "log.info(\"User: \" + username + \" logged in\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void loggingSensitiveData_shouldDetect() {
        LoggingRules.LoggingSensitiveData rule = new LoggingRules.LoggingSensitiveData();
        Map<String, Object> method = createMethod("log", "log.debug(\"Password: \" + password);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void wrongLogLevel_shouldDetect() {
        LoggingRules.WrongLogLevel rule = new LoggingRules.WrongLogLevel();
        Map<String, Object> method = createMethod("handle", "try {\n" +
            "    doWork();\n" +
            "} catch (Exception e) {\n" +
            "    log.info(\"Error: \" + e.getMessage());\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void loggerNotStaticFinal_shouldDetect() {
        LoggingRules.LoggerNotStaticFinal rule = new LoggingRules.LoggerNotStaticFinal();
        Map<String, Object> method = createMethod("process", "Logger logger = LoggerFactory.getLogger(getClass());\n" +
            "logger.info(\"Hello\");\n");
        assertIssues(rule, method, 1);
    }
}
