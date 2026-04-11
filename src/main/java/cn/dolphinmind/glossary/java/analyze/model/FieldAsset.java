package cn.dolphinmind.glossary.java.analyze.model;

import java.util.*;

/**
 * Strong-typed domain model for a field asset.
 */
public class FieldAsset {

    private String name;
    private String address;
    private String typePath;
    private String typeName;
    private String description;
    private List<String> modifiers = new ArrayList<>();
    private boolean isStatic;
    private boolean isFinal;
    private boolean isPublic;
    private boolean isPrivate;
    private String fieldType;  // INTERNAL_STATE, INTERNAL_COMPONENT, EXTERNAL_SERVICE

    public FieldAsset() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTypePath() { return typePath; }
    public void setTypePath(String typePath) { this.typePath = typePath; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getModifiers() { return modifiers; }
    public void setModifiers(List<String> modifiers) { this.modifiers = modifiers; }

    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean isStatic) { this.isStatic = isStatic; }

    public boolean isFinal() { return isFinal; }
    public void setFinal(boolean aFinal) { isFinal = aFinal; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name != null ? name : "");
        map.put("address", address != null ? address : "");
        map.put("type_path", typePath != null ? typePath : "");
        map.put("type_name", typeName != null ? typeName : "");
        map.put("description", description != null ? description : "");
        map.put("modifiers", modifiers);
        map.put("is_static", isStatic);
        map.put("is_final", isFinal);
        map.put("is_public", isPublic);
        map.put("is_private", isPrivate);
        if (fieldType != null) map.put("field_type", fieldType);
        return map;
    }
}
