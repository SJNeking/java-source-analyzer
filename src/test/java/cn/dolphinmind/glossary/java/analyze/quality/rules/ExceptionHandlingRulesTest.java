package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for EXCEPTION_HANDLING category rules.
 */
public class ExceptionHandlingRulesTest extends AbstractRuleTest {

    @Test
    public void loggingAndThrowing_shouldDetect() {
        ExceptionHandlingRules.LoggingAndThrowing rule = new ExceptionHandlingRules.LoggingAndThrowing();
        Map<String, Object> method = createMethod("process", "try {\n" +
            "    doWork();\n" +
            "} catch (Exception e) {\n" +
            "    log.error(\"Error\", e);\n" +
            "    throw e;\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void returnNullOnException_shouldDetect() {
        ExceptionHandlingRules.ReturnNullOnException rule = new ExceptionHandlingRules.ReturnNullOnException();
        Map<String, Object> method = createMethod("parse", "try {\n" +
            "    return Integer.parseInt(input);\n" +
            "} catch (NumberFormatException e) {\n" +
            "    return null;\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void incompleteCatch_shouldDetect() {
        ExceptionHandlingRules.IncompleteCatch rule = new ExceptionHandlingRules.IncompleteCatch();
        Map<String, Object> method = createMethod("process", "try {\n" +
            "    doWork();\n" +
            "} catch (Exception e) {}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void catchNpe_shouldDetect() {
        ExceptionHandlingRules.CatchNpe rule = new ExceptionHandlingRules.CatchNpe();
        Map<String, Object> method = createMethod("process", "try {\n" +
            "    obj.toString();\n" +
            "} catch (NullPointerException e) {}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void catchIndexOutOfBounds_shouldDetect() {
        ExceptionHandlingRules.CatchIndexOutOfBounds rule = new ExceptionHandlingRules.CatchIndexOutOfBounds();
        Map<String, Object> method = createMethod("process", "try {\n" +
            "    return list.get(i);\n" +
            "} catch (IndexOutOfBoundsException e) {}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void catchingThrowable_shouldDetect() {
        ExceptionHandlingRules.CatchingThrowable rule = new ExceptionHandlingRules.CatchingThrowable();
        Map<String, Object> method = createMethod("process", "try {\n" +
            "    doWork();\n" +
            "} catch (Throwable t) {}\n");
        assertIssues(rule, method, 1);
    }
}
