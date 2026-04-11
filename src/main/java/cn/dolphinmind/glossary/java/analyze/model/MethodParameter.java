package cn.dolphinmind.glossary.java.analyze.model;

import java.util.*;

/**
 * Strong-typed domain model for a method parameter.
 */
public class MethodParameter {

    private String name;
    private String type;
    private String typeName;
    private List<String> annotations = new ArrayList<>();
    private boolean isVarargs;
    private String description;

    public MethodParameter() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public List<String> getAnnotations() { return annotations; }
    public void setAnnotations(List<String> annotations) { this.annotations = annotations; }

    public boolean isVarargs() { return isVarargs; }
    public void setVarargs(boolean varargs) { isVarargs = varargs; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name != null ? name : "");
        map.put("type", type != null ? type : "");
        map.put("type_name", typeName != null ? typeName : "");
        map.put("annotations", annotations);
        map.put("is_varargs", isVarargs);
        if (description != null) map.put("description", description);
        return map;
    }
}
