package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for CODE_SMELL category rules.
 */
public class CodeSmellRulesTest extends AbstractRuleTest {

    @Test
    public void systemOutPrintln_shouldDetect() {
        AllRules.SystemOutPrintln rule = new AllRules.SystemOutPrintln();
        Map<String, Object> method = createMethod("test", "System.out.println(\"debug\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void printStackTrace_shouldDetect() {
        AllRules.PrintStackTrace rule = new AllRules.PrintStackTrace();
        Map<String, Object> method = createMethod("test", "e.printStackTrace();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void tooLongMethod_shouldDetect() {
        AllRules.TooLongMethod rule = new AllRules.TooLongMethod();
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            body.append("int var").append(i).append(" = ").append(i).append(";\n");
        }
        Map<String, Object> method = createMethod("longMethod", body.toString());
        assertIssues(rule, method, 1);
    }

    @Test
    public void stringLiteralEquality_shouldDetect() {
        AllRules.StringLiteralEquality rule = new AllRules.StringLiteralEquality();
        Map<String, Object> method = createMethod("test", "if (name == \"admin\") {}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void identicalOperand_shouldDetect() {
        AllRules.IdenticalOperand rule = new AllRules.IdenticalOperand();
        Map<String, Object> method = createMethod("test", "if (x == x) {}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void magicNumber_shouldDetect() {
        AllRules.MagicNumber rule = new AllRules.MagicNumber();
        Map<String, Object> method = createMethod("test", "int timeout = 5000;\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void booleanMethodName_shouldDetect() {
        AllRules.BooleanMethodName rule = new AllRules.BooleanMethodName();
        Map<String, Object> method = createMethod("getStatus", "return true;\n", "boolean", null);
        assertIssues(rule, method, 1);
    }

    @Test
    public void methodTooLongName_shouldDetect() {
        AllRules.MethodTooLongName rule = new AllRules.MethodTooLongName();
        Map<String, Object> method = createMethod("thisMethodNameIsWayTooLongAndShouldBeShortened", "return;\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void stringConcatInLoop_shouldDetect() {
        AllRules.StringConcatInLoop rule = new AllRules.StringConcatInLoop();
        Map<String, Object> method = createMethod("test", "for (int i = 0; i < 10; i++) {\n" +
            "    result += str + \" \";\n" +
            "}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void unusedLocalVariable_shouldDetect() {
        AllRules.UnusedLocalVariable rule = new AllRules.UnusedLocalVariable();
        Map<String, Object> method = createMethod("test", "int unused = 10;\nreturn 5;\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void equalsWithoutHashCode_shouldDetect() {
        AllRules.EqualsWithoutHashCode rule = new AllRules.EqualsWithoutHashCode();
        Map<String, Object> clazz = createClass("Test", Collections.emptyList());
        List<Map<String, Object>> methods = new ArrayList<>();
        methods.add(createMethod("equals", "return true;\n", "boolean", null));
        clazz.put("methods_full", methods);
        assertIssuesOnClass(rule, clazz, 1);
    }

    @Test
    public void booleanLiteralInCondition_shouldDetect() {
        AllRules.BooleanLiteralInCondition rule = new AllRules.BooleanLiteralInCondition();
        Map<String, Object> method = createMethod("test", "if (flag == true) {}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void stringEqualsCaseSensitive_shouldDetect() {
        AllRules.StringEqualsCaseSensitive rule = new AllRules.StringEqualsCaseSensitive();
        Map<String, Object> method = createMethod("test", "if (str.toLowerCase().equals(\"test\")) {}\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void optionalGetWithoutCheck_shouldDetect() {
        AllRules.OptionalGetWithoutCheck rule = new AllRules.OptionalGetWithoutCheck();
        Map<String, Object> method = createMethod("test", "return optional.get();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void streamNotConsumed_shouldDetect() {
        AllRules.StreamNotConsumed rule = new AllRules.StreamNotConsumed();
        Map<String, Object> method = createMethod("test", "list.stream().filter(x -> x > 0);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void serializableField_shouldDetect() {
        AllRules.SerializableField rule = new AllRules.SerializableField();
        Map<String, Object> clazz = createClass("Test", Collections.emptyList());
        clazz.put("kind", "CLASS");
        Map<String, List<String>> hierarchy = new LinkedHashMap<>();
        hierarchy.put("implements", Arrays.asList("java.io.Serializable"));
        clazz.put("hierarchy", hierarchy);
        List<Map<String, Object>> fields = new ArrayList<>();
        Map<String, Object> field = createField("nonSerializable", "com.example.NonSerializable");
        fields.add(field);
        clazz.put("fields_matrix", fields);
        assertIssuesOnClass(rule, clazz, 1);
    }

    private Map<String, Object> createField(String name, String type) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", name);
        field.put("type_path", type);
        return field;
    }
}
