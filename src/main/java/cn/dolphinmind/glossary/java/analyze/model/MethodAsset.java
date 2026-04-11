package cn.dolphinmind.glossary.java.analyze.model;

import java.util.*;

/**
 * Strong-typed domain model for a method/constructor asset.
 */
public class MethodAsset {

    private String name;
    private String address;
    private String returnType;
    private String returnTypeName;
    private List<String> modifiers = new ArrayList<>();
    private List<MethodParameter> parameters = new ArrayList<>();
    private List<String> throwsTypes = new ArrayList<>();
    private String generics;
    private int lineStart;
    private int lineEnd;
    private int linesOfCode;
    private double cyclomaticComplexity;
    private String bodyCode;
    private String description;
    private CommentDetails commentDetails;
    private String intent;
    private Map<String, Object> aiGuidance;
    private List<String> archTags;
    private Map<String, Object> callGraphSummary;
    private Map<String, Object> enhancement;

    // Inventory-style fields (from existing JSON output)
    private List<Map<String, Object>> parametersInventory;
    private String returnTypePath;
    private List<String> internalThrows;

    public MethodAsset() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }

    public String getReturnTypeName() { return returnTypeName; }
    public void setReturnTypeName(String returnTypeName) { this.returnTypeName = returnTypeName; }

    public List<String> getModifiers() { return modifiers; }
    public void setModifiers(List<String> modifiers) { this.modifiers = modifiers; }

    public List<MethodParameter> getParameters() { return parameters; }
    public void setParameters(List<MethodParameter> parameters) { this.parameters = parameters; }

    public List<String> getThrowsTypes() { return throwsTypes; }
    public void setThrowsTypes(List<String> throwsTypes) { this.throwsTypes = throwsTypes; }

    public String getGenerics() { return generics; }
    public void setGenerics(String generics) { this.generics = generics; }

    public int getLineStart() { return lineStart; }
    public void setLineStart(int lineStart) { this.lineStart = lineStart; }

    public int getLineEnd() { return lineEnd; }
    public void setLineEnd(int lineEnd) { this.lineEnd = lineEnd; }

    public int getLinesOfCode() { return linesOfCode; }
    public void setLinesOfCode(int linesOfCode) { this.linesOfCode = linesOfCode; }

    public double getCyclomaticComplexity() { return cyclomaticComplexity; }
    public void setCyclomaticComplexity(double cyclomaticComplexity) { this.cyclomaticComplexity = cyclomaticComplexity; }

    public String getBodyCode() { return bodyCode; }
    public void setBodyCode(String bodyCode) { this.bodyCode = bodyCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CommentDetails getCommentDetails() { return commentDetails; }
    public void setCommentDetails(CommentDetails commentDetails) { this.commentDetails = commentDetails; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public Map<String, Object> getAiGuidance() { return aiGuidance; }
    public void setAiGuidance(Map<String, Object> aiGuidance) { this.aiGuidance = aiGuidance; }

    public List<String> getArchTags() { return archTags; }
    public void setArchTags(List<String> archTags) { this.archTags = archTags; }

    public Map<String, Object> getCallGraphSummary() { return callGraphSummary; }
    public void setCallGraphSummary(Map<String, Object> callGraphSummary) { this.callGraphSummary = callGraphSummary; }

    public Map<String, Object> getEnhancement() { return enhancement; }
    public void setEnhancement(Map<String, Object> enhancement) { this.enhancement = enhancement; }

    public List<Map<String, Object>> getParametersInventory() { return parametersInventory; }
    public void setParametersInventory(List<Map<String, Object>> parametersInventory) { this.parametersInventory = parametersInventory; }

    public String getReturnTypePath() { return returnTypePath; }
    public void setReturnTypePath(String returnTypePath) { this.returnTypePath = returnTypePath; }

    public List<String> getInternalThrows() { return internalThrows; }
    public void setInternalThrows(List<String> internalThrows) { this.internalThrows = internalThrows; }

    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name != null ? name : "");
        map.put("address", address != null ? address : "");
        map.put("return_type", returnType != null ? returnType : "void");
        map.put("return_type_name", returnTypeName != null ? returnTypeName : "");
        map.put("modifiers", modifiers);
        map.put("parameters", parameters != null ? mapList(parameters, MethodParameter::toMap) : new ArrayList<>());
        map.put("throws", throwsTypes != null ? throwsTypes : new ArrayList<>());
        map.put("generics", generics != null ? generics : "");
        map.put("line_start", lineStart);
        map.put("line_end", lineEnd);
        map.put("lines_of_code", linesOfCode);
        map.put("cyclomatic_complexity", cyclomaticComplexity);
        map.put("body_code", bodyCode != null ? bodyCode : "");
        map.put("description", description != null ? description : "");

        if (commentDetails != null) map.put("comment_details", commentDetails.toMap());
        if (intent != null) map.put("intent", intent);
        if (aiGuidance != null) map.put("ai_guidance", aiGuidance);
        if (archTags != null && !archTags.isEmpty()) map.put("arch_tags", archTags);
        if (callGraphSummary != null) map.put("call_graph_summary", callGraphSummary);
        if (enhancement != null) map.put("enhancement", enhancement);

        // Inventory-style fields for backward compatibility
        if (parametersInventory != null) map.put("parameters_inventory", parametersInventory);
        if (returnTypePath != null) map.put("return_type_path", returnTypePath);
        if (internalThrows != null && !internalThrows.isEmpty()) map.put("internal_throws", internalThrows);

        return map;
    }

    private static <T> List<Map<String, Object>> mapList(List<T> list, java.util.function.Function<T, Map<String, Object>> fn) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (T item : list) result.add(fn.apply(item));
        return result;
    }
}
