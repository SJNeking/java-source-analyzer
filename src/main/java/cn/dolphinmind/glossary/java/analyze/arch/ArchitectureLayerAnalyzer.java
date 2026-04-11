package cn.dolphinmind.glossary.java.analyze.arch;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Architecture layer analyzer: identifies layer membership and detects layer violations.
 *
 * Standard layers (by package convention):
 * - CONTROLLER: web/controllers, api, rest
 * - SERVICE: service, biz, business
 * - REPOSITORY: repository, dao, mapper, dal
 * - ENTITY/MODEL: model, entity, domain, dto, vo, po
 * - CONFIG: config, configuration
 * - UTIL: util, helper, common
 *
 * Violations detected when:
 * - Controller calls Repository directly (skipping Service)
 * - Entity calls Repository
 * - etc.
 */
public class ArchitectureLayerAnalyzer {

    // Layer definitions with package patterns
    private static final Map<String, List<String>> LAYER_PATTERNS = new LinkedHashMap<>();
    static {
        LAYER_PATTERNS.put("CONTROLLER", Arrays.asList("controller", "web", "api", "rest", "handler"));
        LAYER_PATTERNS.put("SERVICE", Arrays.asList("service", "biz", "business", "impl"));
        LAYER_PATTERNS.put("REPOSITORY", Arrays.asList("repository", "dao", "mapper", "dal", "data"));
        LAYER_PATTERNS.put("ENTITY", Arrays.asList("model", "entity", "domain", "dto", "vo", "po", "bean"));
        LAYER_PATTERNS.put("CONFIG", Arrays.asList("config", "configuration"));
        LAYER_PATTERNS.put("UTIL", Arrays.asList("util", "helper", "common", "constant", "enums"));
    }

    // Allowed layer dependencies (source -> allowed target layers)
    private static final Map<String, Set<String>> ALLOWED_DEPS = new LinkedHashMap<>();
    static {
        ALLOWED_DEPS.put("CONTROLLER", new HashSet<>(Arrays.asList("SERVICE", "ENTITY", "UTIL", "CONFIG")));
        ALLOWED_DEPS.put("SERVICE", new HashSet<>(Arrays.asList("REPOSITORY", "ENTITY", "UTIL", "CONFIG", "SERVICE")));
        ALLOWED_DEPS.put("REPOSITORY", new HashSet<>(Arrays.asList("ENTITY", "UTIL", "CONFIG")));
        ALLOWED_DEPS.put("ENTITY", new HashSet<>(Arrays.asList("ENTITY", "UTIL")));
        ALLOWED_DEPS.put("CONFIG", new HashSet<>(Arrays.asList("CONFIG", "ENTITY", "UTIL", "SERVICE", "REPOSITORY")));
        ALLOWED_DEPS.put("UTIL", new HashSet<>(Arrays.asList("UTIL", "ENTITY")));
    }

    /**
     * Layer assignment for a class.
     */
    public static class LayerAssignment {
        private String className;
        private String assignedLayer;
        private String packageName;
        private String reasoning; // why it was assigned to this layer

        public LayerAssignment(String className, String assignedLayer, String packageName, String reasoning) {
            this.className = className;
            this.assignedLayer = assignedLayer;
            this.packageName = packageName;
            this.reasoning = reasoning;
        }

        public String getClassName() { return className; }
        public String getAssignedLayer() { return assignedLayer; }
        public String getPackageName() { return packageName; }
        public String getReasoning() { return reasoning; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("class", className);
            map.put("layer", assignedLayer);
            map.put("package", packageName);
            map.put("reasoning", reasoning);
            return map;
        }
    }

    /**
     * Layer violation detected.
     */
    public static class LayerViolation {
        private String sourceClass;
        private String sourceLayer;
        private String targetClass;
        private String targetLayer;
        private String violationType;
        private String description;
        private int line;

        public LayerViolation(String sourceClass, String sourceLayer, String targetClass,
                String targetLayer, String violationType, String description, int line) {
            this.sourceClass = sourceClass;
            this.sourceLayer = sourceLayer;
            this.targetClass = targetClass;
            this.targetLayer = targetLayer;
            this.violationType = violationType;
            this.description = description;
            this.line = line;
        }

        public String getSourceClass() { return sourceClass; }
        public String getSourceLayer() { return sourceLayer; }
        public String getTargetClass() { return targetClass; }
        public String getTargetLayer() { return targetLayer; }
        public String getViolationType() { return violationType; }
        public String getDescription() { return description; }
        public int getLine() { return line; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sourceClass", sourceClass);
            map.put("sourceLayer", sourceLayer);
            map.put("targetClass", targetClass);
            map.put("targetLayer", targetLayer);
            map.put("violationType", violationType);
            map.put("description", description);
            map.put("line", line);
            return map;
        }
    }

    /**
     * Analyze layer assignments and violations from the full analysis data.
     *
     * @param assets List of class assets
     * @param dependencies List of dependency edges
     * @return Map with "layerAssignments", "violations", "layerGraph"
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyze(List<Map<String, Object>> assets,
            List<Map<String, String>> dependencies) {

        Map<String, LayerAssignment> layerAssignments = new LinkedHashMap<>();
        Map<String, String> classToLayer = new HashMap<>();

        // 1. Assign layers to each class based on package name
        for (Map<String, Object> asset : assets) {
            String address = (String) asset.get("address");
            if (address == null) continue;

            String layer = determineLayer(address);
            String pkg = extractPackage(address);
            String className = extractSimpleName(address);

            LayerAssignment assignment = new LayerAssignment(
                    className,
                    layer,
                    pkg,
                    "基于包名匹配: " + pkg
            );

            layerAssignments.put(address, assignment);
            classToLayer.put(address, layer);
        }

        // 2. Detect layer violations from dependencies
        List<LayerViolation> violations = new ArrayList<>();

        for (Map<String, String> dep : dependencies) {
            String source = dep.get("source");
            String target = dep.get("target");
            String depType = dep.getOrDefault("type", "DEPENDS_ON");

            if (source == null || target == null) continue;
            // Only check class-level dependencies (not method-level)
            if (source.contains("#") || target.contains("#")) continue;

            String sourceLayer = classToLayer.get(source);
            String targetLayer = classToLayer.get(target);

            if (sourceLayer == null || targetLayer == null) continue;
            if (sourceLayer.equals(targetLayer)) continue; // Same layer, no violation

            // Check if this dependency is allowed
            Set<String> allowedTargets = ALLOWED_DEPS.getOrDefault(sourceLayer, Collections.emptySet());
            if (!allowedTargets.contains(targetLayer)) {
                String violationType = sourceLayer + "_TO_" + targetLayer;
                String description = String.format("%s (%s) 直接依赖 %s (%s) - 违反架构分层规则",
                        extractSimpleName(source), sourceLayer,
                        extractSimpleName(target), targetLayer);

                violations.add(new LayerViolation(
                        extractSimpleName(source), sourceLayer,
                        extractSimpleName(target), targetLayer,
                        violationType, description, 0
                ));
            }
        }

        // 3. Build layer graph (inter-layer dependencies)
        Map<String, Map<String, Integer>> layerGraph = new LinkedHashMap<>();
        for (String layer : LAYER_PATTERNS.keySet()) {
            layerGraph.put(layer, new LinkedHashMap<>());
        }

        for (Map<String, String> dep : dependencies) {
            String source = dep.get("source");
            String target = dep.get("target");
            if (source == null || target == null) continue;
            if (source.contains("#") || target.contains("#")) continue;

            String sourceLayer = classToLayer.get(source);
            String targetLayer = classToLayer.get(target);
            if (sourceLayer == null || targetLayer == null) continue;
            if (sourceLayer.equals(targetLayer)) continue;

            Map<String, Integer> targets = layerGraph.computeIfAbsent(sourceLayer, k -> new LinkedHashMap<>());
            targets.put(targetLayer, targets.getOrDefault(targetLayer, 0) + 1);
        }

        // 4. Build result
        Map<String, Object> result = new LinkedHashMap<>();

        // Layer assignments
        result.put("layerAssignments", layerAssignments.values().stream()
                .map(LayerAssignment::toMap)
                .collect(Collectors.toList()));

        // Violations
        result.put("violations", violations.stream()
                .map(LayerViolation::toMap)
                .collect(Collectors.toList()));

        // Layer graph
        result.put("layerGraph", layerGraph);

        // Layer statistics
        Map<String, Long> layerCounts = layerAssignments.values().stream()
                .collect(Collectors.groupingBy(LayerAssignment::getAssignedLayer, Collectors.counting()));
        result.put("layerCounts", layerCounts);

        // Violation summary
        Map<String, Object> violationSummary = new LinkedHashMap<>();
        violationSummary.put("total", violations.size());
        Map<String, Long> violationByType = violations.stream()
                .collect(Collectors.groupingBy(LayerViolation::getViolationType, Collectors.counting()));
        violationSummary.put("byType", violationByType);
        result.put("violationSummary", violationSummary);

        return result;
    }

    /**
     * Determine the architecture layer for a class based on its fully qualified name.
     */
    private String determineLayer(String fullyQualifiedName) {
        String lowerName = fullyQualifiedName.toLowerCase();
        String pkg = extractPackage(fullyQualifiedName).toLowerCase();

        // Check package patterns
        for (Map.Entry<String, List<String>> entry : LAYER_PATTERNS.entrySet()) {
            String layer = entry.getKey();
            for (String pattern : entry.getValue()) {
                if (pkg.contains(pattern.toLowerCase())) {
                    return layer;
                }
            }
        }

        // Fallback: check class name patterns
        String className = extractSimpleName(fullyQualifiedName).toLowerCase();
        if (className.endsWith("controller") || className.endsWith("handler") || className.endsWith("api")) {
            return "CONTROLLER";
        }
        if (className.endsWith("service") || className.endsWith("biz")) {
            return "SERVICE";
        }
        if (className.endsWith("repository") || className.endsWith("dao") || className.endsWith("mapper")) {
            return "REPOSITORY";
        }
        if (className.endsWith("dto") || className.endsWith("vo") || className.endsWith("entity") ||
                className.endsWith("model") || className.endsWith("bean")) {
            return "ENTITY";
        }
        if (className.endsWith("config") || className.endsWith("configuration")) {
            return "CONFIG";
        }
        if (className.endsWith("util") || className.endsWith("helper") || className.endsWith("constants")) {
            return "UTIL";
        }

        // Default: SERVICE (most common for business classes)
        return "SERVICE";
    }

    /**
     * Extract package name from fully qualified class name.
     */
    private String extractPackage(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        if (lastDot > 0) {
            return fullyQualifiedName.substring(0, lastDot);
        }
        return "";
    }

    /**
     * Extract simple class name from fully qualified name.
     */
    private String extractSimpleName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        if (lastDot > 0) {
            return fullyQualifiedName.substring(lastDot + 1);
        }
        return fullyQualifiedName;
    }
}
