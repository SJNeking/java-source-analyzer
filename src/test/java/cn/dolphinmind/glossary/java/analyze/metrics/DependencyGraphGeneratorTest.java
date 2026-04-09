package cn.dolphinmind.glossary.java.analyze.metrics;

import cn.dolphinmind.glossary.java.analyze.core.CallChainTracer;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for DependencyGraphGenerator.
 */
public class DependencyGraphGeneratorTest {

    private final DependencyGraphGenerator generator = new DependencyGraphGenerator();

    @Test
    public void generatePackageGraph_shouldReturnValidGraph() {
        List<Map<String, Object>> classAssets = createTestClassAssets();
        DependencyGraphGenerator.PackageDependencyGraph graph = generator.generatePackageGraph(classAssets);

        assertNotNull(graph);
        Map<String, Object> map = graph.toMap();
        assertTrue(map.containsKey("packages"));
        assertTrue(map.containsKey("edges"));
        assertTrue(map.containsKey("weights"));
    }

    @Test
    public void generatePackageGraph_shouldDetectDependencies() {
        List<Map<String, Object>> classAssets = createTestClassAssets();
        DependencyGraphGenerator.PackageDependencyGraph graph = generator.generatePackageGraph(classAssets);

        Map<String, Object> map = graph.toMap();
        @SuppressWarnings("unchecked")
        List<Map.Entry<String, String>> edges = (List<Map.Entry<String, String>>) map.get("edges");

        // Should have detected some dependencies
        assertNotNull(edges);
        assertTrue("Should detect some dependencies", edges.size() > 0);
    }

    @Test
    public void generateModuleMatrix_shouldReturnValidMatrix() {
        CallChainTracer.CallGraph callGraph = createTestCallGraph();
        DependencyGraphGenerator.ModuleDependencyMatrix matrix = generator.generateModuleMatrix(callGraph);

        assertNotNull(matrix);
        Map<String, Object> map = matrix.toMap();
        assertTrue(map.containsKey("modules"));
        assertTrue(map.containsKey("matrix"));
        assertTrue(map.containsKey("total_dependencies"));
    }

    @Test
    public void moduleDependencyMatrix_toMap_shouldReturnCompleteMap() {
        CallChainTracer.CallGraph callGraph = createTestCallGraph();
        DependencyGraphGenerator.ModuleDependencyMatrix matrix = generator.generateModuleMatrix(callGraph);

        Map<String, Object> map = matrix.toMap();
        assertTrue(map.containsKey("modules"));
        assertTrue(map.containsKey("matrix"));
        assertTrue(map.containsKey("total_dependencies"));
    }

    @Test
    public void toGraphvizDot_shouldReturnValidDot() {
        CallChainTracer.CallGraph callGraph = createTestCallGraph();
        DependencyGraphGenerator.ModuleDependencyMatrix matrix = generator.generateModuleMatrix(callGraph);

        String dot = generator.toGraphvizDot(matrix);
        assertNotNull(dot);
        assertTrue("Dot should start with digraph", dot.contains("digraph"));
    }

    @Test
    public void generatePackageGraph_shouldHandleEmptyAssets() {
        List<Map<String, Object>> emptyAssets = Collections.emptyList();
        DependencyGraphGenerator.PackageDependencyGraph graph = generator.generatePackageGraph(emptyAssets);

        assertNotNull(graph);
        Map<String, Object> map = graph.toMap();
        assertTrue(((List<?>) map.get("packages")).isEmpty());
        assertTrue(((List<?>) map.get("edges")).isEmpty());
    }

    private List<Map<String, Object>> createTestClassAssets() {
        List<Map<String, Object>> assets = new ArrayList<>();

        Map<String, Object> userService = new LinkedHashMap<>();
        userService.put("address", "com.example.service.UserService");
        userService.put("kind", "CLASS");
        userService.put("source_file", "com/example/service/UserService.java");
        userService.put("import_dependencies", Arrays.asList(
            "com.example.repository.UserRepository",
            "com.example.model.User",
            "org.springframework.stereotype.Service"
        ));
        assets.add(userService);

        Map<String, Object> userRepository = new LinkedHashMap<>();
        userRepository.put("address", "com.example.repository.UserRepository");
        userRepository.put("kind", "INTERFACE");
        userRepository.put("source_file", "com/example/repository/UserRepository.java");
        userRepository.put("import_dependencies", Arrays.asList(
            "com.example.model.User",
            "org.springframework.data.jpa.repository.JpaRepository"
        ));
        assets.add(userRepository);

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("address", "com.example.model.User");
        user.put("kind", "CLASS");
        user.put("source_file", "com/example/model/User.java");
        user.put("import_dependencies", Collections.emptyList());
        assets.add(user);

        return assets;
    }

    private CallChainTracer.CallGraph createTestCallGraph() {
        CallChainTracer.CallGraph graph = new CallChainTracer.CallGraph();

        // Add some calls
        graph.addCall("com.example.service.UserService#findUser",
            "com.example.repository.UserRepository#findById", true);
        graph.addCall("com.example.service.UserService#saveUser",
            "com.example.repository.UserRepository#save", true);
        graph.addCall("com.example.controller.UserController#get",
            "com.example.service.UserService#findUser", true);

        return graph;
    }
}
