package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for INPUT_VALIDATION category rules.
 */
public class InputValidationRulesTest extends AbstractRuleTest {

    @Test
    public void missingNullCheck_shouldDetect() {
        InputValidationRules.MissingNullCheck rule = new InputValidationRules.MissingNullCheck();
        Map<String, Object> method = createMethod("process", "param.toString();\n", "void", Arrays.asList("public"));
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingTypeValidation_shouldDetect() {
        InputValidationRules.MissingTypeValidation rule = new InputValidationRules.MissingTypeValidation();
        Map<String, Object> method = createMethod("upload", "MultipartFile file = request.getFile(\"file\");\nsave(file);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingAllowlist_shouldDetect() {
        InputValidationRules.MissingAllowlist rule = new InputValidationRules.MissingAllowlist();
        Map<String, Object> method = createMethod("redirect", "String url = request.getParameter(\"url\");\nresponse.sendRedirect(url);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingEncodingValidation_shouldDetect() {
        InputValidationRules.MissingEncodingValidation rule = new InputValidationRules.MissingEncodingValidation();
        Map<String, Object> method = createMethod("readFile", "new FileReader(\"file.txt\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingInputSanitization_shouldDetect() {
        InputValidationRules.MissingInputSanitization rule = new InputValidationRules.MissingInputSanitization();
        Map<String, Object> method = createMethod("display", "String input = request.getParameter(\"input\");\nresponse.getWriter().write(input);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void missingBoundaryCheck_shouldDetect() {
        InputValidationRules.MissingBoundaryCheck rule = new InputValidationRules.MissingBoundaryCheck();
        Map<String, Object> method = createMethod("getFirst", "return args[0];\n");
        assertIssues(rule, method, 1);
    }
}
