package cn.dolphinmind.glossary.java.analyze.metrics;

import cn.dolphinmind.glossary.java.analyze.core.CallChainTracer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dependency Graph Generator
 *
 *对标 JArchitect 的依赖图功能：
 * - 模块依赖矩阵 (Module Dependency Matrix)
 * - 包依赖图 (Package Dependency Graph)
 * - 类依赖网络 (Class Dependency Network)
 * - 依赖权重统计
 *
 * 输出格式：JSON + Graphviz DOT + 简化的 ASCII 矩阵
 */
public class DependencyGraphGenerator {

    /**
     * Module-level dependency matrix.
     */
    public static class ModuleDependencyMatrix {
        private final Map<String, Map<String, Integer>> matrix = new LinkedHashMap<>();
        private final Set<String> allModules = new LinkedHashSet<>();

        public void addDependency(String sourceModule, String targetModule, int weight) {
            allModules.add(sourceModule);
            allModules.add(targetModule);
            matrix.computeIfAbsent(sourceModule, k -> new LinkedHashMap<>())
                  .merge(targetModule, weight, Integer::sum);
        }

        public Set<String> getAllModules() { return Collections.unmodifiableSet(allModules); }
        public int getDependencyCount(String source, String target) {
            return matrix.getOrDefault(source, Collections.emptyMap()).getOrDefault(target, 0);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("modules", new ArrayList<>(allModules));
            result.put("matrix", matrix);
            result.put("total_dependencies", matrix.values().stream()
                    .flatMap(m -> m.values().stream())
                    .mapToInt(Integer::intValue).sum());
            return result;
        }
    }

    /**
     * Package-level dependency graph.
     */
    public static class PackageDependencyGraph {
        private final Map<String, Set<String>> adjacencyList = new LinkedHashMap<>();
        private final Map<String, Integer> weights = new LinkedHashMap<>();

        public void addDependency(String sourcePackage, String targetPackage) {
            adjacencyList.computeIfAbsent(sourcePackage, k -> new LinkedHashSet<>()).add(targetPackage);
            String key = sourcePackage + "→" + targetPackage;
            weights.merge(key, 1, Integer::sum);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("packages", new ArrayList<>(adjacencyList.keySet()));
            result.put("edges", adjacencyList.entrySet().stream()
                    .flatMap(e -> e.getValue().stream().map(v ->
                            new AbstractMap.SimpleEntry<>(e.getKey(), v)))
                    .collect(Collectors.toList()));
            result.put("weights", weights);
            return result;
        }
    }

    /**
     * Class-level dependency network.
     */
    public static class ClassDependencyNetwork {
        private final Map<String, Set<String>> adjacencyList = new LinkedHashMap<>();
        private final Map<String, String> classToLayer = new LinkedHashMap<>();

        public void addClass(String className, String layer) {
            classToLayer.put(className, layer);
            adjacencyList.putIfAbsent(className, new LinkedHashSet<>());
        }

        public void addDependency(String sourceClass, String targetClass) {
            adjacencyList.computeIfAbsent(sourceClass, k -> new LinkedHashSet<>()).add(targetClass);
            adjacencyList.putIfAbsent(targetClass, new LinkedHashSet<>());
        }

        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("nodes", classToLayer.entrySet().stream()
                    .map(e -> {
                        Map<String, String> node = new LinkedHashMap<>();
                        node.put("id", e.getKey());
                        node.put("layer", e.getValue());
                        return node;
                    }).collect(Collectors.toList()));
            result.put("edges", adjacencyList.entrySet().stream()
                    .flatMap(e -> e.getValue().stream().map(v ->
                            new AbstractMap.SimpleEntry<>(e.getKey(), v)))
                    .collect(Collectors.toList()));
            return result;
        }
    }

    /**
     * Generate module dependency matrix from call graph.
     */
    public ModuleDependencyMatrix generateModuleMatrix(CallChainTracer.CallGraph callGraph) {
        ModuleDependencyMatrix matrix = new ModuleDependencyMatrix();

        for (Map.Entry<String, Set<String>> entry : callGraph.getAdjacencyList().entrySet()) {
            String sourceClass = entry.getKey();
            String sourceModule = extractModule(sourceClass);

            for (String callee : entry.getValue()) {
                String targetModule = extractModule(callee);
                if (!sourceModule.equals(targetModule)) {
                    matrix.addDependency(sourceModule, targetModule, 1);
                }
            }
        }

        return matrix;
    }

    /**
     * Generate package dependency graph from class assets.
     */
    public PackageDependencyGraph generatePackageGraph(List<Map<String, Object>> classAssets) {
        PackageDependencyGraph graph = new PackageDependencyGraph();

        for (Map<String, Object> asset : classAssets) {
            String className = (String) asset.getOrDefault("address", "");
            String packageName = className.contains(".") ? className.substring(0, className.lastIndexOf('.')) : "";

            @SuppressWarnings("unchecked")
            List<String> imports = (List<String>) asset.getOrDefault("import_dependencies", Collections.emptyList());

            for (String imp : imports) {
                String targetPackage = imp.contains(".") ? imp.substring(0, imp.lastIndexOf('.')) : "";
                if (!targetPackage.isEmpty() && !targetPackage.equals(packageName)) {
                    graph.addDependency(packageName, targetPackage);
                }
            }
        }

        return graph;
    }

    /**
     * Generate class dependency network from call graph.
     */
    public ClassDependencyNetwork generateClassNetwork(CallChainTracer.CallGraph callGraph,
                                                        List<Map<String, Object>> classAssets) {
        ClassDependencyNetwork network = new ClassDependencyNetwork();

        // Register all classes
        Map<String, String> classToLayer = new HashMap<>();
        for (Map<String, Object> asset : classAssets) {
            String className = (String) asset.getOrDefault("address", "");
            String layer = (String) asset.getOrDefault("layer", "");
            network.addClass(className, layer);
            classToLayer.put(className, layer);
        }

        // Add dependencies
        for (Map.Entry<String, Set<String>> entry : callGraph.getAdjacencyList().entrySet()) {
            String sourceClass = entry.getKey();
            for (String callee : entry.getValue()) {
                network.addDependency(sourceClass, callee);
            }
        }

        return network;
    }

    /**
     * Generate ASCII dependency matrix for console output.
     */
    public void printAsciiMatrix(ModuleDependencyMatrix matrix) {
        List<String> modules = new ArrayList<>(matrix.getAllModules());
        if (modules.isEmpty()) return;

        // Find max module name length for alignment
        int maxLen = modules.stream().mapToInt(String::length).max().orElse(10);

        System.out.println("\n=== 模块依赖矩阵 ===");
        System.out.println("      " + spaces(maxLen - 4) + "← 被依赖方");

        // Header row
        System.out.print("      ");
        for (String m : modules) {
            System.out.print(String.format("%" + (maxLen + 1) + "s", m.length() > maxLen ? m.substring(0, maxLen) : m));
        }
        System.out.println();

        // Data rows
        for (String source : modules) {
            System.out.print(String.format("%" + maxLen + "s", source.length() > maxLen ? source.substring(0, maxLen) : source) + " → ");
            for (String target : modules) {
                int count = matrix.getDependencyCount(source, target);
                String cell = count > 0 ? String.valueOf(count) : ".";
                System.out.print(String.format("%" + (maxLen + 1) + "s", cell));
            }
            System.out.println();
        }

        // Print actual dependencies (non-zero)
        System.out.println("\n实际依赖关系:");
        for (String source : modules) {
            for (String target : modules) {
                int count = matrix.getDependencyCount(source, target);
                if (count > 0) {
                    System.out.println("  " + source + " → " + target + ": " + count + " 次调用");
                }
            }
        }
    }

    /**
     * Export dependency graph as Graphviz DOT format.
     */
    public String toGraphvizDot(ModuleDependencyMatrix matrix) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph ModuleDependencies {\n");
        dot.append("  rankdir=LR;\n");
        dot.append("  node [shape=box, style=filled, fillcolor=lightblue];\n\n");

        for (String source : matrix.getAllModules()) {
            for (String target : matrix.getAllModules()) {
                int count = matrix.getDependencyCount(source, target);
                if (count > 0) {
                    dot.append("  \"").append(source).append("\" -> \"").append(target)
                       .append("\" [label=\"").append(count).append("\"];\n");
                }
            }
        }

        dot.append("}\n");
        return dot.toString();
    }

    private String extractModule(String className) {
        // Extract module from class name: com.example.module.layer.Class → module
        String[] parts = className.split("\\.");
        if (parts.length >= 4) {
            return parts[2]; // Use 3rd segment as module
        }
        return "default";
    }

    private static String spaces(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(' ');
        return sb.toString();
    }
}
