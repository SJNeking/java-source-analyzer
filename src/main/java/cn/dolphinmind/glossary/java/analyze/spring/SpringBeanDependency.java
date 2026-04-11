package cn.dolphinmind.glossary.java.analyze.spring;

import java.util.*;

/**
 * Spring Bean dependency data model.
 * Represents a bean wiring relationship discovered from @Autowired, @Inject, @Bean, etc.
 */
public class SpringBeanDependency {
    private String sourceBean; // the bean that has the dependency
    private String targetBean; // the bean that is depended upon
    private String injectionType; // FIELD, CONSTRUCTOR, SETTER, METHOD
    private String fieldName; // field name for field injection
    private int line; // line number of the injection point
    private String module;

    public SpringBeanDependency() {}

    public String getSourceBean() { return sourceBean; }
    public void setSourceBean(String sourceBean) { this.sourceBean = sourceBean; }
    public String getTargetBean() { return targetBean; }
    public void setTargetBean(String targetBean) { this.targetBean = targetBean; }
    public String getInjectionType() { return injectionType; }
    public void setInjectionType(String injectionType) { this.injectionType = injectionType; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    /**
     * Convert to Map for JSON serialization.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sourceBean", sourceBean);
        map.put("targetBean", targetBean);
        map.put("injectionType", injectionType);
        map.put("fieldName", fieldName);
        map.put("line", line);
        map.put("module", module);
        return map;
    }
}
