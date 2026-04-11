package cn.dolphinmind.glossary.java.analyze.model;

import java.util.*;

/**
 * Strong-typed domain model for an annotation entry.
 */
public class AnnotationEntry {

    private String name;
    private String address;
    private String target;  // class, method, field
    private Map<String, Object> attributes;

    public AnnotationEntry() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name != null ? name : "");
        map.put("address", address != null ? address : "");
        if (target != null) map.put("target", target);
        if (attributes != null) map.put("attributes", attributes);
        return map;
    }
}
