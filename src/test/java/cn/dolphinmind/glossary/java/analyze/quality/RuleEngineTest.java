package cn.dolphinmind.glossary.java.analyze.quality;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for RuleEngine improvements:
 * - Performance tracking
 * - Deduplication
 * - Rule filtering
 * - Summary statistics
 */
public class RuleEngineTest {

    private RuleEngine engine;

    @Before
    public void setUp() {
        engine = new RuleEngine();
    }

    @Test
    public void registerRule_shouldTrackHits() {
        // A rule that always returns 2 issues
        QualityRule mockRule = new QualityRule() {
            public String getRuleKey() { return "TEST-001"; }
            public String getName() { return "Test Rule"; }
            public String getCategory() { return "BUG"; }
            public List<QualityIssue> check(Map<String, Object> classAsset) {
                List<QualityIssue> issues = new ArrayList<>();
                issues.add(new QualityIssue.Builder()
                    .ruleKey("TEST-001").severity(Severity.MAJOR).category("BUG")
                    .filePath("Test.java").className("Test").methodName("m")
                    .line(10).message("test").evidence("test").build());
                issues.add(new QualityIssue.Builder()
                    .ruleKey("TEST-001").severity(Severity.MAJOR).category("BUG")
                    .filePath("Test.java").className("Test").methodName("m")
                    .line(20).message("test").evidence("test").build());
                return issues;
            }
        };

        engine.registerRule(mockRule);
        List<Map<String, Object>> assets = new ArrayList<>();
        assets.add(createClassAsset("Test", new ArrayList<>()));

        List<QualityIssue> issues = engine.run(assets);

        assertEquals(2, issues.size());
        assertEquals(2, engine.getRuleHitCounts().get("TEST-001").intValue());
    }

    @Test
    public void runParallel_shouldExecuteCorrectly() {
        QualityRule mockRule = new QualityRule() {
            public String getRuleKey() { return "TEST-002"; }
            public String getName() { return "Test Rule"; }
            public String getCategory() { return "BUG"; }
            public List<QualityIssue> check(Map<String, Object> classAsset) {
                List<QualityIssue> issues = new ArrayList<>();
                String className = (String) classAsset.getOrDefault("address", "");
                issues.add(new QualityIssue.Builder()
                    .ruleKey("TEST-002").severity(Severity.MAJOR).category("BUG")
                    .filePath(className + ".java").className(className).methodName("m")
                    .line(10).message("test").evidence("test").build());
                return issues;
            }
        };

        engine.registerRule(mockRule);
        List<Map<String, Object>> assets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            assets.add(createClassAsset("TestClass" + i, new ArrayList<>()));
        }

        List<QualityIssue> issues = engine.runParallel(assets);
        assertEquals(10, issues.size());
    }

    @Test
    public void deduplication_shouldPreventDuplicates() {
        // A rule that returns the same issue twice for the same class
        QualityRule mockRule = new QualityRule() {
            public String getRuleKey() { return "TEST-003"; }
            public String getName() { return "Test Rule"; }
            public String getCategory() { return "BUG"; }
            public List<QualityIssue> check(Map<String, Object> classAsset) {
                List<QualityIssue> issues = new ArrayList<>();
                // Same ruleKey + file + line = duplicate
                issues.add(new QualityIssue.Builder()
                    .ruleKey("TEST-003").severity(Severity.MAJOR).category("BUG")
                    .filePath("Test.java").className("Test").methodName("m")
                    .line(10).message("test").evidence("test").build());
                issues.add(new QualityIssue.Builder()
                    .ruleKey("TEST-003").severity(Severity.MAJOR).category("BUG")
                    .filePath("Test.java").className("Test").methodName("m")
                    .line(10).message("test").evidence("test").build());
                return issues;
            }
        };

        engine.registerRule(mockRule);
        List<Map<String, Object>> assets = new ArrayList<>();
        assets.add(createClassAsset("Test", new ArrayList<>()));

        List<QualityIssue> issues = engine.run(assets);
        // Should only have 1 issue (deduplicated)
        assertEquals(1, issues.size());
    }

    @Test
    public void getSummary_shouldIncludePerformanceStats() {
        QualityRule mockRule = new QualityRule() {
            public String getRuleKey() { return "TEST-004"; }
            public String getName() { return "Test Rule"; }
            public String getCategory() { return "BUG"; }
            public List<QualityIssue> check(Map<String, Object> classAsset) {
                List<QualityIssue> issues = new ArrayList<>();
                issues.add(new QualityIssue.Builder()
                    .ruleKey("TEST-004").severity(Severity.CRITICAL).category("BUG")
                    .filePath("Test.java").className("Test").methodName("m")
                    .line(10).message("test").evidence("test").build());
                return issues;
            }
        };

        engine.registerRule(mockRule);
        List<Map<String, Object>> assets = new ArrayList<>();
        assets.add(createClassAsset("Test", new ArrayList<>()));

        List<QualityIssue> issues = engine.run(assets);
        Map<String, Object> summary = engine.getSummary(issues);

        assertEquals(1, summary.get("total_issues"));
        assertTrue(summary.containsKey("performance"));
        @SuppressWarnings("unchecked")
        Map<String, Object> perf = (Map<String, Object>) summary.get("performance");
        assertTrue(perf.containsKey("total_analysis_time_ms"));
        assertTrue(perf.containsKey("slowest_rules_ms"));
    }

    @Test
    public void ruleExecutionTimes_shouldBeTracked() {
        QualityRule mockRule = new QualityRule() {
            public String getRuleKey() { return "TEST-005"; }
            public String getName() { return "Test Rule"; }
            public String getCategory() { return "BUG"; }
            public List<QualityIssue> check(Map<String, Object> classAsset) {
                return Collections.emptyList();
            }
        };

        engine.registerRule(mockRule);
        List<Map<String, Object>> assets = new ArrayList<>();
        assets.add(createClassAsset("Test", new ArrayList<>()));

        engine.run(assets);

        assertTrue(engine.getRuleExecutionTimesMs().containsKey("TEST-005"));
        assertTrue(engine.getRuleExecutionTimesMs().get("TEST-005") >= 0);
    }

    private Map<String, Object> createClassAsset(String className, List<Map<String, Object>> methods) {
        Map<String, Object> clazz = new LinkedHashMap<>();
        clazz.put("address", "com.example." + className);
        clazz.put("kind", "CLASS");
        clazz.put("source_file", "com/example/" + className + ".java");
        clazz.put("methods_full", methods);
        clazz.put("fields_matrix", Collections.emptyList());
        clazz.put("hierarchy", new LinkedHashMap<>());
        clazz.put("annotations", Collections.emptyList());
        clazz.put("tags", new LinkedHashMap<>());
        return clazz;
    }
}
