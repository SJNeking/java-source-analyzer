package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for CODE_ORGANIZATION category rules.
 */
public class CodeOrganizationRulesTest extends AbstractRuleTest {

    @Test
    public void staticImportMisuse_shouldDetect() {
        CodeOrganizationRules.StaticImportMisuse rule = new CodeOrganizationRules.StaticImportMisuse();
        Map<String, Object> method = createMethod("process", "import static com.example.Util.*;\n" +
            "import static com.example.Helper.*;\n" +
            "import static com.example.Config.*;\n" +
            "import static com.example.Constants.*;\n" +
            "import static com.example.Validation.*;\n" +
            "import static com.example.Formatter.*;\n" +
            "import static com.example.Parser.*;\n" +
            "import static com.example.Converter.*;\n" +
            "import static com.example.Mapper.*;\n" +
            "import static com.example.Transformer.*;\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void tooManyImports_shouldDetect() {
        CodeOrganizationRules.TooManyImports rule = new CodeOrganizationRules.TooManyImports();
        Map<String, Object> method = createMethod("process", buildManyImports(35));
        assertIssues(rule, method, 1);
    }

    @Test
    public void tooManyImports_shouldNotDetectWithFewImports() {
        CodeOrganizationRules.TooManyImports rule = new CodeOrganizationRules.TooManyImports();
        Map<String, Object> method = createMethod("process", buildManyImports(10));
        assertIssues(rule, method, 0);
    }

    @Test
    public void duplicateImport_shouldDetect() {
        CodeOrganizationRules.DuplicateImport rule = new CodeOrganizationRules.DuplicateImport();
        Map<String, Object> method = createMethod("process", "import java.util.List;\n" +
            "import java.util.Map;\n" +
            "import java.util.List;\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void nestedClassTooDeep_shouldDetect() {
        CodeOrganizationRules.NestedClassTooDeep rule = new CodeOrganizationRules.NestedClassTooDeep();
        Map<String, Object> method = createMethod("inner", "class A {\n" +
            "    class B {\n" +
            "        class C {\n" +
            "            class D {}\n" +
            "        }\n" +
            "    }\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    private String buildManyImports(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("import com.example.package").append(i).append(".Class").append(i).append(";\n");
        }
        return sb.toString();
    }
}
