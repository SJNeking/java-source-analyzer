package cn.dolphinmind.glossary.java.analyze.metrics;

import cn.dolphinmind.glossary.java.analyze.core.CallChainTracer;
import cn.dolphinmind.glossary.java.analyze.core.PackageStructureMapper;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for CodeMetricsCalculator.
 */
public class CodeMetricsCalculatorTest {

    private final CodeMetricsCalculator calculator = new CodeMetricsCalculator();

    @Test
    public void calculateProjectMetrics_shouldReturnValidMetrics() {
        List<Map<String, Object>> classAssets = createTestClassAssets();
        CodeMetricsCalculator.ProjectMetrics metrics = calculator.calculateProjectMetrics(
            classAssets, null, null);

        assertNotNull(metrics);
        assertEquals(2, metrics.totalClasses);
        assertTrue(metrics.totalMethods > 0);
        assertTrue(metrics.totalFields > 0);
    }

    @Test
    public void calculateProjectMetrics_shouldCalculateCohesion() {
        List<Map<String, Object>> classAssets = createTestClassAssets();
        CodeMetricsCalculator.ProjectMetrics metrics = calculator.calculateProjectMetrics(
            classAssets, null, null);

        // Cohesion should be between 0 and 1
        assertTrue("Cohesion should be >= 0", metrics.cohesionIndex >= 0);
        assertTrue("Cohesion should be <= 1", metrics.cohesionIndex <= 1);
    }

    @Test
    public void calculateProjectMetrics_shouldHandleEmptyAssets() {
        List<Map<String, Object>> emptyAssets = Collections.emptyList();
        CodeMetricsCalculator.ProjectMetrics metrics = calculator.calculateProjectMetrics(
            emptyAssets, null, null);

        assertNotNull(metrics);
        assertEquals(0, metrics.totalClasses);
        assertEquals(0.0, metrics.cohesionIndex, 0.01);
    }

    @Test
    public void classMetrics_shouldContainAllFields() {
        List<Map<String, Object>> classAssets = createTestClassAssets();
        CodeMetricsCalculator.ProjectMetrics metrics = calculator.calculateProjectMetrics(
            classAssets, null, null);

        Map<String, Object> map = metrics.toMap();
        assertTrue(map.containsKey("total_classes"));
        assertTrue(map.containsKey("total_methods"));
        assertTrue(map.containsKey("total_fields"));
        assertTrue(map.containsKey("total_loc"));
        assertTrue(map.containsKey("comment_lines"));
        assertTrue(map.containsKey("avg_complexity"));
        assertTrue(map.containsKey("cohesion_index"));
    }

    @Test
    public void classMetrics_toMap_shouldReturnCompleteMap() {
        List<Map<String, Object>> classAssets = createTestClassAssets();
        CodeMetricsCalculator.ProjectMetrics metrics = calculator.calculateProjectMetrics(
            classAssets, null, null);

        Map<String, Object> map = metrics.toMap();
        // Verify all expected keys are present
        String[] expectedKeys = {"total_classes", "total_methods", "total_fields", "total_loc",
            "comment_lines", "comment_ratio", "avg_method_length", "avg_complexity",
            "max_complexity", "avg_inheritance_depth", "max_inheritance_depth",
            "avg_coupling", "max_coupling", "cohesion_index", "abstract_classes",
            "interfaces", "enum_classes"};

        for (String key : expectedKeys) {
            assertTrue("Missing key: " + key, map.containsKey(key));
        }
    }

    private List<Map<String, Object>> createTestClassAssets() {
        List<Map<String, Object>> assets = new ArrayList<>();

        // Class 1: UserService
        Map<String, Object> userService = new LinkedHashMap<>();
        userService.put("address", "com.example.UserService");
        userService.put("kind", "CLASS");
        userService.put("source_file", "com/example/UserService.java");
        userService.put("layer", "SERVICE");
        userService.put("lines_of_code", 150);
        userService.put("comment_lines", 20);
        userService.put("cyclomatic_complexity", 5.0);
        userService.put("inheritance_depth", 1);

        List<Map<String, Object>> methods1 = new ArrayList<>();
        methods1.add(createMethod("findUser", "return userRepository.findById(id);", "User"));
        methods1.add(createMethod("saveUser", "userRepository.save(user);", "User"));
        userService.put("methods_full", methods1);

        List<Map<String, Object>> fields1 = new ArrayList<>();
        fields1.add(createField("userRepository", "UserRepository"));
        fields1.add(createField("emailService", "EmailService"));
        userService.put("fields_matrix", fields1);
        userService.put("is_abstract", false);
        userService.put("is_interface", false);
        userService.put("is_enum", false);

        assets.add(userService);

        // Class 2: User
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("address", "com.example.User");
        user.put("kind", "CLASS");
        user.put("source_file", "com/example/User.java");
        user.put("layer", "ENTITY");
        user.put("lines_of_code", 50);
        user.put("comment_lines", 5);
        user.put("cyclomatic_complexity", 1.0);
        user.put("inheritance_depth", 0);

        List<Map<String, Object>> methods2 = new ArrayList<>();
        methods2.add(createMethod("getName", "return name;", "String"));
        methods2.add(createMethod("setName", "this.name = name;", "void"));
        user.put("methods_full", methods2);

        List<Map<String, Object>> fields2 = new ArrayList<>();
        fields2.add(createField("name", "String"));
        fields2.add(createField("email", "String"));
        user.put("fields_matrix", fields2);
        user.put("is_abstract", false);
        user.put("is_interface", false);
        user.put("is_enum", false);

        assets.add(user);

        return assets;
    }

    private Map<String, Object> createMethod(String name, String body, String returnType) {
        Map<String, Object> method = new LinkedHashMap<>();
        method.put("name", name);
        method.put("body_code", body);
        method.put("return_type_path", returnType);
        return method;
    }

    private Map<String, Object> createField(String name, String type) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", name);
        field.put("type_path", type);
        return field;
    }
}
