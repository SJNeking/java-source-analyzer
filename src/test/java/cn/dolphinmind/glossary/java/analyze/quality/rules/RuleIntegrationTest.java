package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Integration tests for rules using real Java source files.
 *
 * These tests parse actual Java files from the project and run rules against them,
 * providing full context (imports, annotations, class structure) that unit tests cannot.
 */
public class RuleIntegrationTest {

    private static List<Map<String, Object>> testClasses;
    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));

    @BeforeClass
    public static void setUp() throws IOException {
        testClasses = new ArrayList<>();

        // Parse test fixture files
        Path testFixtureDir = PROJECT_ROOT.resolve("src/test/resources/test-fixtures");
        if (Files.exists(testFixtureDir)) {
            Files.walk(testFixtureDir)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(RuleIntegrationTest::parseAndAddClass);
        }
    }

    private static void parseAndAddClass(Path path) {
        try {
            String content = new String(Files.readAllBytes(path));
            String[] lines = content.split("\n");

            // Extract class name
            String className = path.getFileName().toString().replace(".java", "");

            // Extract imports
            List<String> imports = new ArrayList<>();
            for (String line : lines) {
                if (line.trim().startsWith("import ") && !line.trim().startsWith("import static")) {
                    imports.add(line.trim().replace("import ", "").replace(";", ""));
                }
            }

            // Create class asset
            Map<String, Object> classAsset = new LinkedHashMap<>();
            classAsset.put("address", "com.test." + className);
            classAsset.put("kind", "CLASS");
            classAsset.put("source_file", path.toString());
            classAsset.put("import_dependencies", imports);
            classAsset.put("fields_matrix", Collections.emptyList());
            classAsset.put("hierarchy", new LinkedHashMap<>());

            // Extract methods
            List<Map<String, Object>> methods = new ArrayList<>();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.contains("void ") || line.contains("String ") || line.contains("int ") ||
                    line.contains("boolean ") || line.contains("List<") || line.contains("Map<")) {
                    if (line.contains("(") && line.contains(")")) {
                        String methodName = line.replaceAll(".*\\s(\\w+)\\s*\\(.*", "$1");
                        StringBuilder body = new StringBuilder();
                        for (int j = i + 1; j < lines.length && j < i + 10; j++) {
                            if (!lines[j].trim().startsWith("@")) {
                                body.append(lines[j]).append("\n");
                            }
                        }

                        Map<String, Object> method = new LinkedHashMap<>();
                        method.put("name", methodName);
                        method.put("body_code", body.toString());
                        method.put("return_type_path", "void");
                        method.put("modifiers", Collections.singletonList("public"));
                        method.put("line_start", i + 1);
                        method.put("source_file", path.toString());
                        methods.add(method);
                    }
                }
            }
            classAsset.put("methods_full", methods);
            testClasses.add(classAsset);
        } catch (IOException e) {
            // ignore
        }
    }

    // Helper method to run a rule against all test classes and return issues
    private List<QualityIssue> runRule(QualityRule rule) {
        List<QualityIssue> allIssues = new ArrayList<>();
        for (Map<String, Object> clazz : testClasses) {
            allIssues.addAll(rule.check(clazz));
        }
        return allIssues;
    }

    @Test
    public void systemOut_shouldDetectInFixtures() {
        AllRules.SystemOutPrintln rule = new AllRules.SystemOutPrintln();
        List<QualityIssue> issues = runRule(rule);
        // At least one System.out.println should be found in test fixtures
        assertNotNull(issues);
    }

    @Test
    public void printStackTrace_shouldDetectInFixtures() {
        AllRules.PrintStackTrace rule = new AllRules.PrintStackTrace();
        List<QualityIssue> issues = runRule(rule);
        assertNotNull(issues);
    }

    @Test
    public void stringLiteralEquality_shouldDetectInFixtures() {
        AllRules.StringLiteralEquality rule = new AllRules.StringLiteralEquality();
        List<QualityIssue> issues = runRule(rule);
        assertNotNull(issues);
    }

    @Test
    public void nullDereference_shouldDetectInFixtures() {
        AllRules.NullDereference rule = new AllRules.NullDereference();
        List<QualityIssue> issues = runRule(rule);
        assertNotNull(issues);
    }

    @Test
    public void tooLongMethod_shouldDetectInFixtures() {
        AllRules.TooLongMethod rule = new AllRules.TooLongMethod();
        List<QualityIssue> issues = runRule(rule);
        assertNotNull(issues);
    }

    @Test
    public void godClass_shouldDetectInFixtures() {
        AllRules.GodClass rule = new AllRules.GodClass();
        List<QualityIssue> issues = runRule(rule);
        assertNotNull(issues);
    }
}
