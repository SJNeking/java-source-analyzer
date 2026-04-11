package cn.dolphinmind.glossary.java.analyze.performance;

import cn.dolphinmind.glossary.java.analyze.config.RulesConfig;
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
 * Performance regression tests to ensure analysis speed doesn't degrade.
 *
 * These tests:
 * 1. Measure analysis time on a fixed codebase
 * 2. Ensure performance stays within acceptable bounds
 * 3. Detect performance regressions early
 */
public class PerformanceRegressionTest {

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
    private static List<Map<String, Object>> testClasses;

    @Before
    public void setUp() throws Exception {
        if (testClasses == null) {
            testClasses = new ArrayList<>();
            // Parse a subset of real Java files for testing
            Path srcDir = PROJECT_ROOT.resolve("src/main/java/cn/dolphinmind/glossary/java/analyze");
            Files.walk(srcDir, 3)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("test"))
                    .limit(20)
                    .forEach(p -> {
                        try {
                            testClasses.addAll(parseJavaFile(p));
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
        String className = javaFile.getFileName().toString().replace(".java", "");

        List<String> imports = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ") && !trimmed.startsWith("import static")) {
                imports.add(trimmed.replace("import ", "").replace(";", ""));
            }
        }

        Map<String, Object> classAsset = new LinkedHashMap<>();
        classAsset.put("address", "com.test." + className);
        classAsset.put("kind", "CLASS");
        classAsset.put("source_file", javaFile.toString());
        classAsset.put("import_dependencies", imports);
        classAsset.put("lines_of_code", lines.length);
        classAsset.put("comment_lines", 0);
        classAsset.put("fields_matrix", Collections.emptyList());
        classAsset.put("hierarchy", new LinkedHashMap<>());
        classAsset.put("is_abstract", false);
        classAsset.put("is_interface", false);
        classAsset.put("is_enum", false);

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

    @Test
    public void ruleEngineShouldCompleteInReasonableTime() {
        RuleEngine engine = new RuleEngine();
        java.util.function.Consumer<cn.dolphinmind.glossary.java.analyze.quality.QualityRule> reg = rule -> {
            engine.registerRule(rule);
        };

        // Register all rules
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

        long startTime = System.currentTimeMillis();
        List<QualityIssue> issues = engine.run(testClasses);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;

        // Should complete within 5 seconds for 20 classes
        assertTrue("Rule engine should complete within 5 seconds (took " + duration + "ms)",
            duration < 5000);
        assertNotNull("Should produce issues", issues);
    }

    @Test
    public void metricsCalculationShouldBeFast() {
        cn.dolphinmind.glossary.java.analyze.metrics.CodeMetricsCalculator calculator =
            new cn.dolphinmind.glossary.java.analyze.metrics.CodeMetricsCalculator();

        long startTime = System.currentTimeMillis();
        cn.dolphinmind.glossary.java.analyze.metrics.CodeMetricsCalculator.ProjectMetrics metrics =
            calculator.calculateProjectMetrics(testClasses, null, null);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;

        // Should complete within 1 second for 20 classes
        assertTrue("Metrics calculation should complete within 1 second (took " + duration + "ms)",
            duration < 1000);
        assertNotNull("Should produce metrics", metrics);
    }

    @Test
    public void entryPointDiscoveryShouldBeFast() throws Exception {
        cn.dolphinmind.glossary.java.analyze.core.EntryPointDiscovery discovery =
            new cn.dolphinmind.glossary.java.analyze.core.EntryPointDiscovery();

        long startTime = System.currentTimeMillis();
        List<cn.dolphinmind.glossary.java.analyze.core.EntryPointDiscovery.EntryPoint> eps =
            discovery.discover(PROJECT_ROOT);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;

        // Should complete within 10 seconds for full project
        assertTrue("Entry point discovery should complete within 10 seconds (took " + duration + "ms)",
            duration < 10000);
        assertNotNull("Should find entry points", eps);
    }

    @Test
    public void shouldHandleLargeClassesEfficiently() {
        // Create a large class with many methods
        Map<String, Object> largeClass = new LinkedHashMap<>();
        largeClass.put("address", "com.test.LargeClass");
        largeClass.put("kind", "CLASS");
        largeClass.put("source_file", "LargeClass.java");
        largeClass.put("import_dependencies", Collections.emptyList());
        largeClass.put("lines_of_code", 10000);
        largeClass.put("comment_lines", 100);
        largeClass.put("fields_matrix", Collections.emptyList());
        largeClass.put("hierarchy", new LinkedHashMap<>());
        largeClass.put("is_abstract", false);
        largeClass.put("is_interface", false);
        largeClass.put("is_enum", false);

        List<Map<String, Object>> methods = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            StringBuilder body = new StringBuilder();
            for (int j = 0; j < 20; j++) {
                body.append("int var").append(j).append(" = ").append(j).append(";\n");
            }
            Map<String, Object> method = new LinkedHashMap<>();
            method.put("name", "method" + i);
            method.put("body_code", body.toString());
            method.put("return_type_path", "void");
            method.put("modifiers", Collections.singletonList("public"));
            method.put("line_start", i * 30);
            method.put("source_file", "LargeClass.java");
            methods.add(method);
        }
        largeClass.put("methods_full", methods);

        RuleEngine engine = new RuleEngine();
        engine.registerRule(new AllRules.EmptyCatchBlock());
        engine.registerRule(new AllRules.StringLiteralEquality());
        engine.registerRule(new AllRules.NullDereference());

        long startTime = System.currentTimeMillis();
        List<QualityIssue> issues = engine.run(Collections.singletonList(largeClass));
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;

        // Should complete within 3 seconds for 500 methods
        assertTrue("Should handle 500 methods within 3 seconds (took " + duration + "ms)",
            duration < 3000);
        assertNotNull("Should produce issues", issues);
    }
}
