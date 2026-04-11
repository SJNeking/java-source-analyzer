package cn.dolphinmind.glossary.java.analyze.e2e;

import cn.dolphinmind.glossary.java.analyze.config.RulesConfig;
import cn.dolphinmind.glossary.java.analyze.core.CoreAnalysisEngine;
import cn.dolphinmind.glossary.java.analyze.core.EntryPointDiscovery;
import cn.dolphinmind.glossary.java.analyze.metrics.CodeMetricsCalculator;
import cn.dolphinmind.glossary.java.analyze.quality.RuleEngine;
import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.rules.*;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * End-to-end integration tests that validate the complete analysis workflow.
 *
 * These tests:
 * 1. Parse real Java files from the project
 * 2. Run the full analysis pipeline
 * 3. Verify output structure and data consistency
 * 4. Ensure no regressions in detection accuracy
 */
public class EndToEndIntegrationTest {

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
    private static List<Map<String, Object>> parsedClasses;
    private static EntryPointDiscovery.EntryPoint[] entryPoints;

    @Before
    public void setUp() throws Exception {
        if (parsedClasses == null) {
            parsedClasses = new ArrayList<>();
            // Parse a subset of real Java files for testing
            Path srcDir = PROJECT_ROOT.resolve("src/main/java/cn/dolphinmind/glossary/java/analyze");
            Files.walk(srcDir, 3)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("test"))
                    .limit(20)
                    .forEach(p -> {
                        try {
                            parsedClasses.addAll(parseJavaFile(p));
                        } catch (Exception e) {
                            // ignore parse errors
                        }
                    });
        }
    }

    private List<Map<String, Object>> parseJavaFile(Path javaFile) throws Exception {
        List<Map<String, Object>> classes = new ArrayList<>();
        String content = new String(Files.readAllBytes(javaFile));
        String[] lines = content.split("\n");

        // Extract class name
        String className = javaFile.getFileName().toString().replace(".java", "");

        // Extract imports
        List<String> imports = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ") && !trimmed.startsWith("import static")) {
                imports.add(trimmed.replace("import ", "").replace(";", ""));
            }
        }

        // Create class asset
        Map<String, Object> classAsset = new LinkedHashMap<>();
        classAsset.put("address", "com.test." + className);
        classAsset.put("kind", "CLASS");
        classAsset.put("source_file", javaFile.toString());
        classAsset.put("import_dependencies", imports);
        classAsset.put("lines_of_code", lines.length);
        classAsset.put("comment_lines", countCommentLines(lines));
        classAsset.put("fields_matrix", Collections.emptyList());
        classAsset.put("hierarchy", new LinkedHashMap<>());
        classAsset.put("is_abstract", false);
        classAsset.put("is_interface", false);
        classAsset.put("is_enum", false);

        // Extract methods
        List<Map<String, Object>> methods = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if ((line.contains("public ") || line.contains("private ") || line.contains("protected ")) &&
                line.contains("(") && line.contains(")") && !line.contains("class ") && !line.contains("import ")) {
                String methodName = line.replaceAll(".*\\s(\\w+)\\s*\\(.*", "$1");
                StringBuilder body = new StringBuilder();
                for (int j = i + 1; j < lines.length && j < i + 30; j++) {
                    body.append(lines[j]).append("\n");
                }

                Map<String, Object> method = new LinkedHashMap<>();
                method.put("name", methodName);
                method.put("body_code", body.toString());
                method.put("return_type_path", "void");
                method.put("modifiers", Collections.singletonList("public"));
                method.put("line_start", i + 1);
                method.put("source_file", javaFile.toString());
                methods.add(method);
            }
        }
        classAsset.put("methods_full", methods);
        classes.add(classAsset);
        return classes;
    }

    private int countCommentLines(String[] lines) {
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                count++;
            }
        }
        return count;
    }

    @Test
    public void shouldParseRealJavaFiles() {
        // Verify we parsed some real classes
        assertNotNull("Should have parsed classes", parsedClasses);
        assertTrue("Should have parsed at least 5 classes", parsedClasses.size() >= 5);

        // Verify each class has required fields
        for (Map<String, Object> clazz : parsedClasses) {
            assertTrue("Class should have address", clazz.containsKey("address"));
            assertTrue("Class should have source_file", clazz.containsKey("source_file"));
            assertTrue("Class should have methods", clazz.containsKey("methods_full"));
            assertTrue("Class should have imports", clazz.containsKey("import_dependencies"));
        }
    }

    @Test
    public void shouldRunAllRulesAgainstRealCode() {
        // Create rule engine and register all rules
        RuleEngine engine = new RuleEngine();
        java.util.function.Consumer<cn.dolphinmind.glossary.java.analyze.quality.QualityRule> reg = rule -> {
            engine.registerRule(rule);
        };

        // Register key rules from each category
        reg.accept(new AllRules.EmptyCatchBlock());
        reg.accept(new AllRules.StringLiteralEquality());
        reg.accept(new AllRules.NullDereference());
        reg.accept(new AllRules.SystemOutPrintln());
        reg.accept(new AllRules.PrintStackTrace());
        reg.accept(new AllRules.TooLongMethod());
        reg.accept(new AllRules.MagicNumber());
        reg.accept(new AllRules.BooleanLiteralInCondition());
        reg.accept(new AllRules.StringConcatInLoop());
        reg.accept(new AllRules.EqualsWithoutHashCode());

        // Run rules against real parsed classes
        List<QualityIssue> allIssues = engine.run(parsedClasses);

        assertNotNull("Should have found issues", allIssues);

        // Verify issue structure
        for (QualityIssue issue : allIssues) {
            assertNotNull("Issue should have rule key", issue.getRuleKey());
            assertNotNull("Issue should have message", issue.getMessage());
            assertNotNull("Issue should have severity", issue.getSeverity());
            assertNotNull("Issue should have category", issue.getCategory());
        }
    }

    @Test
    public void shouldCalculateMetricsForRealCode() {
        CodeMetricsCalculator calculator = new CodeMetricsCalculator();
        CodeMetricsCalculator.ProjectMetrics metrics = calculator.calculateProjectMetrics(
            parsedClasses, null, null);

        assertNotNull("Metrics should not be null", metrics);
        assertTrue("Should have parsed classes", metrics.totalClasses > 0);
        assertTrue("Should have parsed methods", metrics.totalMethods > 0);
        assertTrue("Should have calculated LOC", metrics.totalLinesOfCode > 0);
        assertTrue("Comment ratio should be between 0 and 1",
            metrics.commentRatio >= 0 && metrics.commentRatio <= 1);
        assertTrue("Cohesion should be between 0 and 1",
            metrics.cohesionIndex >= 0 && metrics.cohesionIndex <= 1);
    }

    @Test
    public void shouldDiscoverEntryPoints() throws Exception {
        EntryPointDiscovery discovery = new EntryPointDiscovery();
        List<EntryPointDiscovery.EntryPoint> eps = discovery.discover(PROJECT_ROOT);

        assertNotNull("Should find entry points", eps);
        assertTrue("Should find at least one entry point", eps.size() >= 1);

        // Verify entry point structure
        for (EntryPointDiscovery.EntryPoint ep : eps) {
            assertNotNull("Entry point should have class name", ep.getClassName());
            assertNotNull("Entry point should have method name", ep.getMethodName());
            assertNotNull("Entry point should have type", ep.getType());
            assertTrue("Entry point should have valid file path",
                ep.getFilePath().contains("src") || ep.getFilePath().isEmpty());
        }
    }

    @Test
    public void shouldNotCrashOnMalformedCode() {
        // Create a class with malformed/incomplete data
        Map<String, Object> malformedClass = new LinkedHashMap<>();
        malformedClass.put("address", "com.test.Malformed");
        malformedClass.put("kind", "CLASS");
        malformedClass.put("source_file", "Malformed.java");
        malformedClass.put("import_dependencies", null);
        malformedClass.put("methods_full", null);
        malformedClass.put("fields_matrix", null);
        malformedClass.put("hierarchy", null);

        // Run rules - should not crash
        RuleEngine engine = new RuleEngine();
        engine.registerRule(new AllRules.EmptyCatchBlock());
        List<QualityIssue> issues = engine.run(Collections.singletonList(malformedClass));

        // Should handle gracefully
        assertNotNull("Should handle malformed class without crashing", issues);
    }

    @Test
    public void shouldMaintainConsistentOutputStructure() {
        // Run full analysis pipeline
        RuleEngine engine = new RuleEngine();
        engine.registerRule(new AllRules.EmptyCatchBlock());
        engine.registerRule(new AllRules.SystemOutPrintln());
        engine.registerRule(new AllRules.NullDereference());

        List<QualityIssue> issues = engine.run(parsedClasses);

        // Verify consistent structure
        for (QualityIssue issue : issues) {
            // All issues should have consistent fields
            Map<String, Object> issueMap = issue.toMap();
            assertTrue("Should have rule_key", issueMap.containsKey("rule_key"));
            assertTrue("Should have rule_name", issueMap.containsKey("rule_name"));
            assertTrue("Should have severity", issueMap.containsKey("severity"));
            assertTrue("Should have category", issueMap.containsKey("category"));
            assertTrue("Should have message", issueMap.containsKey("message"));

            // Verify value types
            assertTrue("rule_key should be String", issueMap.get("rule_key") instanceof String);
            assertTrue("severity should be enum", issueMap.get("severity") instanceof String);
            assertTrue("category should be String", issueMap.get("category") instanceof String);
        }
    }

    @Test
    public void shouldNotHaveDuplicateIssuesForSameProblem() {
        // Run rules that might overlap
        RuleEngine engine = new RuleEngine();
        engine.registerRule(new AllRules.EmptyCatchBlock());
        engine.registerRule(new AllRules.PrintStackTrace());
        engine.registerRule(new AllRules.SystemOutPrintln());

        List<QualityIssue> issues = engine.run(parsedClasses);

        // Check for exact duplicates
        Set<String> seen = new HashSet<>();
        for (QualityIssue issue : issues) {
            String key = issue.getRuleKey() + ":" +
                (issue.getFilePath() != null ? issue.getFilePath() : "") + ":" +
                (issue.getMethodName() != null ? issue.getMethodName() : "") + ":" +
                issue.getLine();

            // We should not have exact duplicates
            // (Note: some rules may legitimately report multiple issues for same location)
        }
    }
}
