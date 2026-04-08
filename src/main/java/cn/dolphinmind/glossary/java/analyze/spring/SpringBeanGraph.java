package cn.dolphinmind.glossary.java.analyze.spring;

import java.util.*;
import java.util.regex.*;

/**
 * Analyzes Spring beans and builds the dependency injection graph.
 *
 * Capabilities:
 * 1. Bean discovery from @Component, @Service, @Repository, @Controller, @RestController
 * 2. Dependency injection analysis (@Autowired, @Resource, @Inject, @Value, constructor injection)
 * 3. Circular dependency detection (A→B→C→A)
 * 4. Layer violation detection (Controller→Repository direct call)
 * 5. Bean lifecycle analysis
 * 6. Missing bean detection (injected type not found in the project)
 */
public class SpringBeanGraph {

    private final Map<String, SpringBean> beans = new LinkedHashMap<>();
    private final List<CircularDependency> circularDeps = new ArrayList<>();
    private final List<LayerViolation> layerViolations = new ArrayList<>();
    private final List<String> missingBeans = new ArrayList<>();

    /**
     * Analyze a single class asset and extract Spring bean information.
     */
    @SuppressWarnings("unchecked")
    public void analyzeClass(Map<String, Object> classAsset) {
        String className = (String) classAsset.getOrDefault("address", "");
        String filePath = (String) classAsset.getOrDefault("source_file", "");
        String kind = (String) classAsset.getOrDefault("kind", "");

        if (!"CLASS".equals(kind)) return;

        // Extract annotations
        List<Map<String, Object>> annParams = (List<Map<String, Object>>)
                classAsset.getOrDefault("annotation_params", Collections.emptyList());

        SpringBean.BeanType beanType = null;
        for (Map<String, Object> ann : annParams) {
            String annName = (String) ann.get("name");
            if (annName == null) continue;

            if (annName.contains("Controller") || annName.contains("RestController")) {
                beanType = SpringBean.BeanType.CONTROLLER;
            } else if (annName.contains("Service")) {
                beanType = SpringBean.BeanType.SERVICE;
            } else if (annName.contains("Repository")) {
                beanType = SpringBean.BeanType.REPOSITORY;
            } else if (annName.contains("Component")) {
                beanType = SpringBean.BeanType.COMPONENT;
            } else if (annName.contains("Configuration")) {
                beanType = SpringBean.BeanType.CONFIGURATION;
            }
        }

        if (beanType == null) return; // Not a Spring bean

        SpringBean bean = new SpringBean(className, beanType, filePath);
        for (Map<String, Object> ann : annParams) {
            bean.addAnnotation((String) ann.get("name"));
        }

        // Extract field-level dependencies (@Autowired, @Resource)
        List<Map<String, Object>> fields = (List<Map<String, Object>>)
                classAsset.getOrDefault("fields", Collections.emptyList());
        for (Map<String, Object> field : fields) {
            String fieldName = (String) field.get("name");
            String fieldType = (String) field.get("type");
            @SuppressWarnings("unchecked")
            Set<String> fieldTags = (Set<String>) field.getOrDefault("semantic_tags", Collections.emptySet());

            // Check if field is a Spring dependency injection
            if (fieldTags.contains("Autowired") || fieldTags.contains("Resource") ||
                fieldTags.contains("Inject") || fieldTags.contains("Value")) {

                String injectionType = "unknown";
                if (fieldTags.contains("Autowired")) injectionType = "@Autowired";
                else if (fieldTags.contains("Resource")) injectionType = "@Resource";
                else if (fieldTags.contains("Inject")) injectionType = "@Inject";
                else if (fieldTags.contains("Value")) injectionType = "@Value";

                bean.addDependency(fieldType, fieldName, injectionType);
            }
        }

        // Extract constructor-level dependencies
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> constructors = (List<Map<String, Object>>)
                classAsset.getOrDefault("constructor_matrix", Collections.emptyList());
        for (Map<String, Object> ctor : constructors) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> params = (List<Map<String, String>>)
                    ctor.getOrDefault("parameters_inventory", Collections.emptyList());
            for (Map<String, String> param : params) {
                String paramType = param.getOrDefault("type_path", "");
                String paramName = param.getOrDefault("name", "");
                if (!paramType.isEmpty() && !paramType.startsWith("java.") &&
                    !paramType.startsWith("javax.")) {
                    bean.addDependency(paramType, paramName, "constructor");
                }
            }
        }

        // Extract method parameter dependencies
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> methods = (List<Map<String, Object>>)
                classAsset.getOrDefault("methods_full", Collections.emptyList());
        for (Map<String, Object> method : methods) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> params = (List<Map<String, String>>)
                    method.getOrDefault("parameters", Collections.emptyList());
            for (Map<String, String> param : params) {
                String paramType = param.getOrDefault("type_path", "");
                if (paramType != null && paramType.contains("HttpServletRequest") ||
                    (paramType != null && paramType.contains("HttpServletResponse"))) {
                    bean.addDependency(paramType, param.getOrDefault("name", ""), "method-param");
                }
            }
        }

        beans.put(className, bean);
    }

    /**
     * Detect circular dependencies.
     * A→B→C→A means Controller A depends on Service B which depends on Repository C
     * which depends on Controller A.
     */
    public void detectCircularDependencies() {
        circularDeps.clear();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String beanName : beans.keySet()) {
            if (!visited.contains(beanName)) {
                detectCircularDFS(beanName, visited, recursionStack, new ArrayList<>());
            }
        }
    }

    private void detectCircularDFS(String beanName, Set<String> visited,
                                    Set<String> recursionStack, List<String> path) {
        visited.add(beanName);
        recursionStack.add(beanName);
        path.add(beanName);

        SpringBean bean = beans.get(beanName);
        if (bean != null) {
            for (SpringBean.Dependency dep : bean.getDependencies()) {
                String target = dep.getTargetClass();
                // Normalize: try to find the bean by simple name
                String targetBean = findBeanBySimpleName(target);
                if (targetBean != null && !targetBean.equals(beanName)) {
                    if (recursionStack.contains(targetBean)) {
                        // Found a cycle
                        int cycleStart = path.indexOf(targetBean);
                        if (cycleStart >= 0) {
                            List<String> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                            cycle.add(targetBean); // Complete the cycle
                            circularDeps.add(new CircularDependency(cycle));
                        }
                    } else if (!visited.contains(targetBean)) {
                        detectCircularDFS(targetBean, visited, recursionStack, path);
                    }
                }
            }
        }

        path.remove(path.size() - 1);
        recursionStack.remove(beanName);
    }

    /**
     * Detect layer violations.
     * Controller should not directly depend on Repository.
     * Controller → Service → Repository is the correct flow.
     */
    public void detectLayerViolations() {
        layerViolations.clear();
        for (SpringBean bean : beans.values()) {
            if (bean.getType() == SpringBean.BeanType.CONTROLLER) {
                for (SpringBean.Dependency dep : bean.getDependencies()) {
                    String targetBean = findBeanBySimpleName(dep.getTargetClass());
                    if (targetBean != null) {
                        SpringBean target = beans.get(targetBean);
                        if (target != null && target.getType() == SpringBean.BeanType.REPOSITORY) {
                            layerViolations.add(new LayerViolation(
                                    bean.getClassName(), target.getClassName(),
                                    dep.getFieldName(), dep.getInjectionType()
                            ));
                        }
                    }
                }
            }
        }
    }

    /**
     * Detect missing beans: dependencies that reference types not found in the project.
     */
    public void detectMissingBeans() {
        missingBeans.clear();
        Set<String> knownTypes = new HashSet<>();
        for (SpringBean bean : beans.values()) {
            knownTypes.add(bean.getClassName());
            knownTypes.add(bean.getSimpleName());
        }

        for (SpringBean bean : beans.values()) {
            for (SpringBean.Dependency dep : bean.getDependencies()) {
                String target = dep.getTargetClass();
                if (!target.startsWith("java.") && !target.startsWith("javax.") &&
                    !target.startsWith("org.springframework") && !target.startsWith("org.apache") &&
                    !knownTypes.contains(target) && !knownTypes.stream().anyMatch(t -> t.endsWith("." + target) || t.equals(target))) {
                    missingBeans.add(bean.getClassName() + "." + dep.getFieldName() + " → " + target);
                }
            }
        }
    }

    /**
     * Find a bean by its simple name (without package).
     */
    private String findBeanBySimpleName(String targetClass) {
        // Direct match
        if (beans.containsKey(targetClass)) return targetClass;

        // Match by simple name
        String simpleTarget = targetClass.contains(".") ?
                targetClass.substring(targetClass.lastIndexOf('.') + 1) : targetClass;
        for (String beanName : beans.keySet()) {
            String simpleBean = beanName.contains(".") ?
                    beanName.substring(beanName.lastIndexOf('.') + 1) : beanName;
            if (simpleBean.equals(simpleTarget)) return beanName;
        }
        return null;
    }

    /**
     * Run all analyses.
     */
    public void analyzeAll(List<Map<String, Object>> classAssets) {
        for (Map<String, Object> asset : classAssets) {
            analyzeClass(asset);
        }
        detectCircularDependencies();
        detectLayerViolations();
        detectMissingBeans();
    }

    // --- Results ---

    public Map<String, SpringBean> getBeans() { return Collections.unmodifiableMap(beans); }
    public List<CircularDependency> getCircularDependencies() { return Collections.unmodifiableList(circularDeps); }
    public List<LayerViolation> getLayerViolations() { return Collections.unmodifiableList(layerViolations); }
    public List<String> getMissingBeans() { return Collections.unmodifiableList(missingBeans); }

    /**
     * Get the bean dependency graph as a map.
     */
    public Map<String, List<String>> getDependencyGraph() {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (SpringBean bean : beans.values()) {
            List<String> deps = new ArrayList<>();
            for (SpringBean.Dependency dep : bean.getDependencies()) {
                String targetBean = findBeanBySimpleName(dep.getTargetClass());
                if (targetBean != null) deps.add(targetBean);
            }
            graph.put(bean.getClassName(), deps);
        }
        return graph;
    }

    /**
     * Get summary statistics.
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_beans", beans.size());

        Map<String, Long> byType = new LinkedHashMap<>();
        for (SpringBean.BeanType type : SpringBean.BeanType.values()) {
            long count = beans.values().stream().filter(b -> b.getType() == type).count();
            if (count > 0) byType.put(type.name(), count);
        }
        summary.put("beans_by_type", byType);
        summary.put("circular_dependencies", circularDeps.size());
        summary.put("layer_violations", layerViolations.size());
        summary.put("missing_beans", missingBeans.size());

        return summary;
    }

    // --- Result Models ---

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
            map.put("length", cycle.size() - 1); // -1 because last element == first
            return map;
        }
    }

    /**
     * A layer violation (e.g., Controller → Repository).
     */
    public static class LayerViolation {
        private final String sourceClass;
        private final String targetClass;
        private final String fieldName;
        private final String injectionType;

        public LayerViolation(String sourceClass, String targetClass, String fieldName, String injectionType) {
            this.sourceClass = sourceClass;
            this.targetClass = targetClass;
            this.fieldName = fieldName;
            this.injectionType = injectionType;
        }

        public String getSourceClass() { return sourceClass; }
        public String getTargetClass() { return targetClass; }
        public String getFieldName() { return fieldName; }
        public String getInjectionType() { return injectionType; }

        @Override
        public String toString() {
            return sourceClass + " → " + targetClass + " (via " + fieldName + " " + injectionType + ")";
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("source", sourceClass);
            map.put("target", targetClass);
            map.put("field", fieldName);
            map.put("injection_type", injectionType);
            map.put("violation", "Controller directly depends on Repository");
            return map;
        }
    }
}
