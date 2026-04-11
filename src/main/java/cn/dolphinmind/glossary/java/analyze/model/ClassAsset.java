package cn.dolphinmind.glossary.java.analyze.model;

import java.util.*;

/**
 * Strong-typed domain model for a class/interface/enum asset.
 *
 * Replaces Map<String, Object> for class assets.
 * Has toMap() for backward compatibility with existing JSON output.
 */
public class ClassAsset {

    private String address;
    private String kind;          // class, interface, enum, annotation
    private String description;
    private String sourceFile;
    private List<String> modifiers = new ArrayList<>();
    private String classGenerics;

    // Type flags
    private boolean isInterface;
    private boolean isAbstract;
    private boolean isEnum;
    private int extendsCount;
    private int implementsCount;

    // Content
    private CommentDetails commentDetails;
    private List<MethodAsset> methodsFull = new ArrayList<>();
    private List<FieldAsset> fields = new ArrayList<>();
    private List<Map<String, Object>> fieldsMatrix = new ArrayList<>();
    private List<Map<String, Object>> fieldSegments = new ArrayList<>();
    private List<AnnotationEntry> annotations = new ArrayList<>();

    // Analysis results
    private Map<String, Object> semanticProfile;
    private Map<String, Object> reasoningResults;
    private Map<String, Object> archTags;
    private Map<String, Object> aiGuidance;
    private Map<String, Object> callGraphSummary;
    private String insightSummary;
    private List<String> componentTags = new ArrayList<>();
    private String domainContext;
    private Map<String, Object> aiEvolution;

    // Metrics
    private int linesOfCode;
    private int commentLines;
    private double cyclomaticComplexity;
    private int inheritanceDepth;

    // Dependencies
    private List<Map<String, String>> dependencies = new ArrayList<>();

    // Hierarchy
    private List<String> hierarchy;

    public ClassAsset() {}

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }

    public List<String> getModifiers() { return modifiers; }
    public void setModifiers(List<String> modifiers) { this.modifiers = modifiers; }

    public String getClassGenerics() { return classGenerics; }
    public void setClassGenerics(String classGenerics) { this.classGenerics = classGenerics; }

    public boolean isInterface() { return isInterface; }
    public void setInterface(boolean isInterface) { this.isInterface = isInterface; }

    public boolean isAbstract() { return isAbstract; }
    public void setAbstract(boolean isAbstract) { this.isAbstract = isAbstract; }

    public boolean isEnum() { return isEnum; }
    public void setEnum(boolean isEnum) { this.isEnum = isEnum; }

    public int getExtendsCount() { return extendsCount; }
    public void setExtendsCount(int extendsCount) { this.extendsCount = extendsCount; }

    public int getImplementsCount() { return implementsCount; }
    public void setImplementsCount(int implementsCount) { this.implementsCount = implementsCount; }

    public CommentDetails getCommentDetails() { return commentDetails; }
    public void setCommentDetails(CommentDetails commentDetails) { this.commentDetails = commentDetails; }

    public List<MethodAsset> getMethodsFull() { return methodsFull; }
    public void setMethodsFull(List<MethodAsset> methodsFull) { this.methodsFull = methodsFull; }

    public List<FieldAsset> getFields() { return fields; }
    public void setFields(List<FieldAsset> fields) { this.fields = fields; }

    public List<Map<String, Object>> getFieldsMatrix() { return fieldsMatrix; }
    public void setFieldsMatrix(List<Map<String, Object>> fieldsMatrix) { this.fieldsMatrix = fieldsMatrix; }

    public List<Map<String, Object>> getFieldSegments() { return fieldSegments; }
    public void setFieldSegments(List<Map<String, Object>> fieldSegments) { this.fieldSegments = fieldSegments; }

    public List<AnnotationEntry> getAnnotations() { return annotations; }
    public void setAnnotations(List<AnnotationEntry> annotations) { this.annotations = annotations; }

    public Map<String, Object> getSemanticProfile() { return semanticProfile; }
    public void setSemanticProfile(Map<String, Object> semanticProfile) { this.semanticProfile = semanticProfile; }

    public Map<String, Object> getReasoningResults() { return reasoningResults; }
    public void setReasoningResults(Map<String, Object> reasoningResults) { this.reasoningResults = reasoningResults; }

    public Map<String, Object> getArchTags() { return archTags; }
    public void setArchTags(Map<String, Object> archTags) { this.archTags = archTags; }

    public Map<String, Object> getAiGuidance() { return aiGuidance; }
    public void setAiGuidance(Map<String, Object> aiGuidance) { this.aiGuidance = aiGuidance; }

    public Map<String, Object> getCallGraphSummary() { return callGraphSummary; }
    public void setCallGraphSummary(Map<String, Object> callGraphSummary) { this.callGraphSummary = callGraphSummary; }

    public String getInsightSummary() { return insightSummary; }
    public void setInsightSummary(String insightSummary) { this.insightSummary = insightSummary; }

    public List<String> getComponentTags() { return componentTags; }
    public void setComponentTags(List<String> componentTags) { this.componentTags = componentTags; }

    public String getDomainContext() { return domainContext; }
    public void setDomainContext(String domainContext) { this.domainContext = domainContext; }

    public Map<String, Object> getAiEvolution() { return aiEvolution; }
    public void setAiEvolution(Map<String, Object> aiEvolution) { this.aiEvolution = aiEvolution; }

    public int getLinesOfCode() { return linesOfCode; }
    public void setLinesOfCode(int linesOfCode) { this.linesOfCode = linesOfCode; }

    public int getCommentLines() { return commentLines; }
    public void setCommentLines(int commentLines) { this.commentLines = commentLines; }

    public double getCyclomaticComplexity() { return cyclomaticComplexity; }
    public void setCyclomaticComplexity(double cyclomaticComplexity) { this.cyclomaticComplexity = cyclomaticComplexity; }

    public int getInheritanceDepth() { return inheritanceDepth; }
    public void setInheritanceDepth(int inheritanceDepth) { this.inheritanceDepth = inheritanceDepth; }

    public List<Map<String, String>> getDependencies() { return dependencies; }
    public void setDependencies(List<Map<String, String>> dependencies) { this.dependencies = dependencies; }

    public List<String> getHierarchy() { return hierarchy; }
    public void setHierarchy(List<String> hierarchy) { this.hierarchy = hierarchy; }

    /**
     * Convert to Map for backward compatibility with existing JSON output.
     * This allows incremental migration - use the typed model internally,
     * then call toMap() only at the JSON serialization boundary.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("address", address);
        map.put("kind", kind);
        map.put("description", description != null ? description : "");
        map.put("source_file", sourceFile != null ? sourceFile : "");
        map.put("modifiers", modifiers);
        map.put("class_generics", classGenerics != null ? classGenerics : "");
        map.put("is_interface", isInterface);
        map.put("is_abstract", isAbstract);
        map.put("is_enum", isEnum);
        map.put("extends_count", extendsCount);
        map.put("implements_count", implementsCount);

        if (commentDetails != null) {
            map.put("comment_details", commentDetails.toMap());
        }

        map.put("methods_full", methodsFull != null ? mapList(methodsFull, MethodAsset::toMap) : new ArrayList<>());
        map.put("fields", fields != null ? mapList(fields, FieldAsset::toMap) : new ArrayList<>());
        map.put("fields_matrix", fieldsMatrix != null ? fieldsMatrix : new ArrayList<>());
        map.put("field_segments", fieldSegments != null ? fieldSegments : new ArrayList<>());
        map.put("annotations", annotations != null ? mapList(annotations, AnnotationEntry::toMap) : new ArrayList<>());

        if (semanticProfile != null) map.put("semantic_profile", semanticProfile);
        if (reasoningResults != null) map.put("reasoning_results", reasoningResults);
        if (archTags != null) map.put("arch_tags", archTags);
        if (aiGuidance != null) map.put("ai_guidance", aiGuidance);
        if (callGraphSummary != null) map.put("call_graph_summary", callGraphSummary);
        if (insightSummary != null) map.put("insight_summary", insightSummary);
        if (componentTags != null && !componentTags.isEmpty()) map.put("component_tags", componentTags);
        if (domainContext != null) map.put("domain_context", domainContext);
        if (aiEvolution != null) map.put("ai_evolution", aiEvolution);

        map.put("lines_of_code", linesOfCode);
        map.put("comment_lines", commentLines);
        map.put("cyclomatic_complexity", cyclomaticComplexity);
        map.put("inheritance_depth", inheritanceDepth);

        if (dependencies != null && !dependencies.isEmpty()) map.put("dependencies", dependencies);
        if (hierarchy != null) map.put("hierarchy", hierarchy);

        return map;
    }

    private static <T> List<Map<String, Object>> mapList(List<T> list, java.util.function.Function<T, Map<String, Object>> fn) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (T item : list) {
            result.add(fn.apply(item));
        }
        return result;
    }
}
