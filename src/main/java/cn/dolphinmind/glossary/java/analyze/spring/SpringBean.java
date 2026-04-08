package cn.dolphinmind.glossary.java.analyze.spring;

import java.util.*;

/**
 * Represents a Spring Bean discovered through annotations.
 */
public class SpringBean {

    public enum BeanType {
        CONTROLLER, SERVICE, REPOSITORY, COMPONENT, CONFIGURATION, UNKNOWN
    }

    private final String className;
    private final BeanType type;
    private final List<String> annotations = new ArrayList<>();
    private final List<Dependency> dependencies = new ArrayList<>();
    private final String filePath;

    public SpringBean(String className, BeanType type, String filePath) {
        this.className = className;
        this.type = type;
        this.filePath = filePath;
    }

    public String getClassName() { return className; }
    public BeanType getType() { return type; }
    public List<String> getAnnotations() { return Collections.unmodifiableList(annotations); }
    public List<Dependency> getDependencies() { return Collections.unmodifiableList(dependencies); }
    public String getFilePath() { return filePath; }

    public void addAnnotation(String annotation) {
        annotations.add(annotation);
    }

    public void addDependency(String targetClass, String fieldName, String injectionType) {
        dependencies.add(new Dependency(targetClass, fieldName, injectionType));
    }

    /**
     * Simple name without package.
     */
    public String getSimpleName() {
        int dotIdx = className.lastIndexOf('.');
        return dotIdx > 0 ? className.substring(dotIdx + 1) : className;
    }

    /**
     * Check if this bean depends on another bean.
     */
    public boolean dependsOn(String targetClassName) {
        return dependencies.stream().anyMatch(d -> d.getTargetClass().equals(targetClassName));
    }

    @Override
    public String toString() {
        return type + ": " + getSimpleName() + " (" + dependencies.size() + " deps)";
    }

    /**
     * A dependency injection relationship.
     */
    public static class Dependency {
        private final String targetClass;
        private final String fieldName;
        private final String injectionType; // @Autowired, @Resource, constructor, @Value

        public Dependency(String targetClass, String fieldName, String injectionType) {
            this.targetClass = targetClass;
            this.fieldName = fieldName;
            this.injectionType = injectionType;
        }

        public String getTargetClass() { return targetClass; }
        public String getFieldName() { return fieldName; }
        public String getInjectionType() { return injectionType; }

        @Override
        public String toString() {
            return fieldName + " → " + targetClass + " (" + injectionType + ")";
        }
    }
}
