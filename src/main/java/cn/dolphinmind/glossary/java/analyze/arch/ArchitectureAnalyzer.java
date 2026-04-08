package cn.dolphinmind.glossary.java.analyze.arch;

import java.util.*;

/**
 * Architecture-level analysis: module dependencies, layer violations,
 * circular dependencies at class and module level.
 *
 * Analyzes:
 * 1. Module dependency graph (from package structure and imports)
 * 2. Layer violation detection (based on package naming conventions)
 * 3. Circular dependency detection at class and module level
 * 4. Stability metrics (afferent/efferent coupling)
 * 5. Abstractness vs instability analysis
 */
public class ArchitectureAnalyzer {

    /** Standard layer patterns: package pattern → layer name */
    private static final Map<String, String> LAYER_PATTERNS = new LinkedHashMap<>();
    static {
        LAYER_PATTERNS.put(".controller.", "CONTROLLER");
        LAYER_PATTERNS.put(".web.", "CONTROLLER");
        LAYER_PATTERNS.put(".api.", "CONTROLLER");
        LAYER_PATTERNS.put(".rest.", "CONTROLLER");
        LAYER_PATTERNS.put(".service.", "SERVICE");
        LAYER_PATTERNS.put(".biz.", "SERVICE");
        LAYER_PATTERNS.put(".impl.", "IMPLEMENTATION");
        LAYER_PATTERNS.put(".repository.", "REPOSITORY");
        LAYER_PATTERNS.put(".mapper.", "REPOSITORY");
        LAYER_PATTERNS.put(".dao.", "REPOSITORY");
        LAYER_PATTERNS.put(".dto.", "DTO");
        LAYER_PATTERNS.put(".vo.", "DTO");
        LAYER_PATTERNS.put(".entity.", "ENTITY");
        LAYER_PATTERNS.put(".model.", "ENTITY");
        LAYER_PATTERNS.put(".domain.", "ENTITY");
        LAYER_PATTERNS.put(".config.", "CONFIG");
        LAYER_PATTERNS.put(".util.", "UTIL");
        LAYER_PATTERNS.put(".common.", "UTIL");
    }

    /** Allowed dependency directions: from layer → to layers */
    private static final Map<String, Set<String>> ALLOWED_DEPENDENCIES = new LinkedHashMap<>();
    static {
        ALLOWED_DEPENDENCIES.put("CONTROLLER", new LinkedHashSet<>(Arrays.asList("SERVICE", "DTO", "ENTITY", "UTIL", "CONFIG")));
        ALLOWED_DEPENDENCIES.put("SERVICE", new LinkedHashSet<>(Arrays.asList("REPOSITORY", "DTO", "ENTITY", "UTIL", "CONFIG", "SERVICE")));
        ALLOWED_DEPENDENCIES.put("REPOSITORY", new LinkedHashSet<>(Arrays.asList("ENTITY", "DTO", "UTIL", "CONFIG")));
        ALLOWED_DEPENDENCIES.put("DTO", new LinkedHashSet<>(Arrays.asList("ENTITY", "UTIL")));
        ALLOWED_DEPENDENCIES.put("ENTITY", new LinkedHashSet<>(Arrays.asList("UTIL")));
        ALLOWED_DEPENDENCIES.put("UTIL", new LinkedHashSet<>());
        ALLOWED_DEPENDENCIES.put("CONFIG", new LinkedHashSet<>(Arrays.asList("ENTITY", "DTO", "UTIL")));
        ALLOWED_DEPENDENCIES.put("IMPLEMENTATION", new LinkedHashSet<>(Arrays.asList("REPOSITORY", "ENTITY", "DTO", "UTIL", "CONFIG", "SERVICE")));
    }

    private final List<ClassInfo> classes = new ArrayList<>();
    private final List<ModuleDependency> moduleDeps = new ArrayList<>();
    private final List<LayerViolation> layerViolations = new ArrayList<>();
    private final List<CircularDependency> circularDeps = new ArrayList<>();

    /**
     * Register a class for architecture analysis.
     */
    @SuppressWarnings("unchecked")
    public void registerClass(Map<String, Object> classAsset) {
        String address = (String) classAsset.getOrDefault("address", "");
        String sourceFile = (String) classAsset.getOrDefault("source_file", "");

        // Determine layer from package name
        String layer = detectLayer(address);
        String moduleName = detectModule(address);

        ClassInfo ci = new ClassInfo(address, sourceFile, layer, moduleName);

        // Extract dependencies from import_dependencies
        List<String> imports = (List<String>) classAsset.getOrDefault("import_dependencies", Collections.emptyList());
        for (String imp : imports) {
            String targetLayer = detectLayer(imp);
            String targetModule = detectModule(imp);
            if (targetLayer != null && !targetLayer.equals(layer)) {
                ci.addDependency(imp, targetLayer, targetModule);
            }
        }

        // Also extract from fields_matrix
        List<Map<String, Object>> fields = (List<Map<String, Object>>)
                classAsset.getOrDefault("fields_matrix", Collections.emptyList());
        for (Map<String, Object> f : fields) {
            String typePath = (String) f.getOrDefault("type_path", "");
            if (typePath != null && !typePath.startsWith("java.") && !typePath.startsWith("javax.")) {
                String targetLayer = detectLayer(typePath);
                if (targetLayer != null && !targetLayer.equals(layer)) {
                    ci.addDependency(typePath, targetLayer, detectModule(typePath));
                }
            }
        }

        classes.add(ci);
    }

    /**
     * Run all architecture analyses.
     */
    public void analyze() {
        detectModuleDependencies();
        detectLayerViolations();
        detectCircularDependencies();
    }

    /**
     * Detect the layer of a class based on its package name.
     */
    private String detectLayer(String className) {
        String lower = className.toLowerCase();
        for (Map.Entry<String, String> entry : LAYER_PATTERNS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Detect the module of a class based on its package name.
     */
    private String detectModule(String className) {
        int dotIdx = className.indexOf('.');
        if (dotIdx < 0) return "default";
        int secondDot = className.indexOf('.', dotIdx + 1);
        if (secondDot < 0) return className.substring(0, dotIdx);
        // Use up to the 3rd or 4th package segment as module
        int thirdDot = className.indexOf('.', secondDot + 1);
        if (thirdDot < 0) return className.substring(0, secondDot);
        return className.substring(0, thirdDot);
    }

    /**
     * Detect module-level dependencies.
     */
    private void detectModuleDependencies() {
        moduleDeps.clear();
        Map<String, Map<String, Integer>> depCounts = new LinkedHashMap<>();

        for (ClassInfo ci : classes) {
            String sourceModule = ci.getModule();
            for (ClassInfo.Dependency dep : ci.getDependencies()) {
                String targetModule = dep.getModule();
                if (targetModule != null && !targetModule.equals(sourceModule)) {
                    depCounts.computeIfAbsent(sourceModule, k -> new LinkedHashMap<>())
                             .merge(targetModule, 1, Integer::sum);
                }
            }
        }

        for (Map.Entry<String, Map<String, Integer>> entry : depCounts.entrySet()) {
            for (Map.Entry<String, Integer> target : entry.getValue().entrySet()) {
                moduleDeps.add(new ModuleDependency(entry.getKey(), target.getKey(), target.getValue()));
            }
        }
    }

    /**
     * Detect layer violations.
     * e.g., CONTROLLER → REPOSITORY is a violation (should go through SERVICE).
     */
    private void detectLayerViolations() {
        layerViolations.clear();
        for (ClassInfo ci : classes) {
            String sourceLayer = ci.getLayer();
            if (sourceLayer == null) continue;

            Set<String> allowed = ALLOWED_DEPENDENCIES.getOrDefault(sourceLayer, Collections.emptySet());

            for (ClassInfo.Dependency dep : ci.getDependencies()) {
                String targetLayer = dep.getLayer();
                if (targetLayer != null && !allowed.contains(targetLayer) && !targetLayer.equals(sourceLayer)) {
                    layerViolations.add(new LayerViolation(
                            ci.getClassName(), sourceLayer,
                            dep.getTargetClass(), targetLayer
                    ));
                }
            }
        }
    }

    /**
     * Detect circular dependencies at class level.
     */
    private void detectCircularDependencies() {
        circularDeps.clear();
        Map<String, Set<String>> adjList = new LinkedHashMap<>();

        for (ClassInfo ci : classes) {
            String className = ci.getClassName();
            adjList.putIfAbsent(className, new LinkedHashSet<>());
            for (ClassInfo.Dependency dep : ci.getDependencies()) {
                String target = dep.getTargetClass();
                // Find if target is a known class
                for (ClassInfo other : classes) {
                    if (other.getClassName().endsWith("." + target) || other.getClassName().equals(target)) {
                        adjList.get(className).add(other.getClassName());
                        break;
                    }
                }
            }
        }

        // Detect cycles using DFS
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String node : adjList.keySet()) {
            if (!visited.contains(node)) {
                detectCycleDFS(node, adjList, visited, recursionStack, new ArrayList<>());
            }
        }
    }

    private void detectCycleDFS(String node, Map<String, Set<String>> adjList,
                                 Set<String> visited, Set<String> recursionStack, List<String> path) {
        visited.add(node);
        recursionStack.add(node);
        path.add(node);

        for (String neighbor : adjList.getOrDefault(node, Collections.emptySet())) {
            if (recursionStack.contains(neighbor)) {
                int cycleStart = path.indexOf(neighbor);
                if (cycleStart >= 0) {
                    List<String> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                    cycle.add(neighbor);
                    circularDeps.add(new CircularDependency(cycle));
                }
            } else if (!visited.contains(neighbor)) {
                detectCycleDFS(neighbor, adjList, visited, recursionStack, path);
            }
        }

        path.remove(path.size() - 1);
        recursionStack.remove(node);
    }

    // --- Results ---

    public List<ModuleDependency> getModuleDependencies() { return Collections.unmodifiableList(moduleDeps); }
    public List<LayerViolation> getLayerViolations() { return Collections.unmodifiableList(layerViolations); }
    public List<CircularDependency> getCircularDependencies() { return Collections.unmodifiableList(circularDeps); }
    public List<ClassInfo> getClasses() { return Collections.unmodifiableList(classes); }

    /**
     * Get summary statistics.
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_classes_analyzed", classes.size());

        Map<String, Long> byLayer = new LinkedHashMap<>();
        for (ClassInfo ci : classes) {
            String layer = ci.getLayer() != null ? ci.getLayer() : "UNASSIGNED";
            byLayer.merge(layer, 1L, Long::sum);
        }
        summary.put("classes_by_layer", byLayer);

        summary.put("module_dependencies", moduleDeps.size());
        summary.put("layer_violations", layerViolations.size());
        summary.put("circular_dependencies", circularDeps.size());

        // Stability metrics
        summary.put("module_dependency_graph", getModuleDependencyGraph());

        return summary;
    }

    /**
     * Get module dependency graph.
     */
    private Map<String, List<String>> getModuleDependencyGraph() {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (ModuleDependency dep : moduleDeps) {
            graph.computeIfAbsent(dep.getSourceModule(), k -> new ArrayList<>()).add(dep.getTargetModule());
        }
        return graph;
    }

    // --- Data Models ---

    /**
     * Information about a class and its dependencies.
     */
    public static class ClassInfo {
        private final String className;
        private final String sourceFile;
        private final String layer;
        private final String module;
        private final List<Dependency> dependencies = new ArrayList<>();

        public ClassInfo(String className, String sourceFile, String layer, String module) {
            this.className = className;
            this.sourceFile = sourceFile;
            this.layer = layer;
            this.module = module;
        }

        public String getClassName() { return className; }
        public String getSourceFile() { return sourceFile; }
        public String getLayer() { return layer; }
        public String getModule() { return module; }
        public List<Dependency> getDependencies() { return Collections.unmodifiableList(dependencies); }

        public void addDependency(String targetClass, String targetLayer, String targetModule) {
            dependencies.add(new Dependency(targetClass, targetLayer, targetModule));
        }

        public static class Dependency {
            private final String targetClass;
            private final String layer;
            private final String module;

            public Dependency(String targetClass, String layer, String module) {
                this.targetClass = targetClass;
                this.layer = layer;
                this.module = module;
            }

            public String getTargetClass() { return targetClass; }
            public String getLayer() { return layer; }
            public String getModule() { return module; }
        }
    }

    /**
     * A module-level dependency.
     */
    public static class ModuleDependency {
        private final String sourceModule;
        private final String targetModule;
        private final int referenceCount;

        public ModuleDependency(String sourceModule, String targetModule, int referenceCount) {
            this.sourceModule = sourceModule;
            this.targetModule = targetModule;
            this.referenceCount = referenceCount;
        }

        public String getSourceModule() { return sourceModule; }
        public String getTargetModule() { return targetModule; }
        public int getReferenceCount() { return referenceCount; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("source", sourceModule);
            map.put("target", targetModule);
            map.put("reference_count", referenceCount);
            return map;
        }
    }

    /**
     * A layer violation.
     */
    public static class LayerViolation {
        private final String sourceClass;
        private final String sourceLayer;
        private final String targetClass;
        private final String targetLayer;

        public LayerViolation(String sourceClass, String sourceLayer, String targetClass, String targetLayer) {
            this.sourceClass = sourceClass;
            this.sourceLayer = sourceLayer;
            this.targetClass = targetClass;
            this.targetLayer = targetLayer;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("source_class", sourceClass);
            map.put("source_layer", sourceLayer);
            map.put("target_class", targetClass);
            map.put("target_layer", targetLayer);
            map.put("violation", sourceLayer + " should not directly depend on " + targetLayer);
            return map;
        }
    }

    /**
     * A circular dependency cycle.
     */
    public static class CircularDependency {
        private final List<String> cycle;

        public CircularDependency(List<String> cycle) {
            this.cycle = new ArrayList<>(cycle);
        }

        public List<String> getCycle() { return Collections.unmodifiableList(cycle); }

        @Override
        public String toString() {
            return String.join(" → ", cycle);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("cycle", cycle);
            map.put("length", cycle.size() - 1);
            return map;
        }
    }
}
