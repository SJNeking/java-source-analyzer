package cn.dolphinmind.glossary.java.analyze.model;

import java.util.*;

/**
 * Strong-typed domain model for comment/Javadoc details.
 */
public class CommentDetails {

    private String raw;
    private String summary;
    private String cleaned;
    private String style;  // line, block, javadoc
    private int line;
    private List<String> params;
    private String returnDesc;
    private List<String> throwsDesc;
    private List<String> seeRefs;
    private String since;
    private String deprecated;
    private Map<String, Object> javadocTags;
    private Map<String, Object> semanticNotes;
    private Map<String, Object> translation;

    public CommentDetails() {}

    public String getRaw() { return raw; }
    public void setRaw(String raw) { this.raw = raw; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getCleaned() { return cleaned; }
    public void setCleaned(String cleaned) { this.cleaned = cleaned; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }

    public List<String> getParams() { return params; }
    public void setParams(List<String> params) { this.params = params; }

    public String getReturnDesc() { return returnDesc; }
    public void setReturnDesc(String returnDesc) { this.returnDesc = returnDesc; }

    public List<String> getThrowsDesc() { return throwsDesc; }
    public void setThrowsDesc(List<String> throwsDesc) { this.throwsDesc = throwsDesc; }

    public List<String> getSeeRefs() { return seeRefs; }
    public void setSeeRefs(List<String> seeRefs) { this.seeRefs = seeRefs; }

    public String getSince() { return since; }
    public void setSince(String since) { this.since = since; }

    public String getDeprecated() { return deprecated; }
    public void setDeprecated(String deprecated) { this.deprecated = deprecated; }

    public Map<String, Object> getJavadocTags() { return javadocTags; }
    public void setJavadocTags(Map<String, Object> javadocTags) { this.javadocTags = javadocTags; }

    public Map<String, Object> getSemanticNotes() { return semanticNotes; }
    public void setSemanticNotes(Map<String, Object> semanticNotes) { this.semanticNotes = semanticNotes; }

    public Map<String, Object> getTranslation() { return translation; }
    public void setTranslation(Map<String, Object> translation) { this.translation = translation; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (raw != null) map.put("raw", raw);
        if (summary != null) map.put("summary", summary);
        if (cleaned != null) map.put("cleaned", cleaned);
        if (style != null) map.put("style", style);
        map.put("line", line);
        if (params != null && !params.isEmpty()) map.put("params", params);
        if (returnDesc != null) map.put("return", returnDesc);
        if (throwsDesc != null && !throwsDesc.isEmpty()) map.put("throws", throwsDesc);
        if (seeRefs != null && !seeRefs.isEmpty()) map.put("see", seeRefs);
        if (since != null) map.put("since", since);
        if (deprecated != null) map.put("deprecated", deprecated);
        if (javadocTags != null) map.put("javadoc_tags", javadocTags);
        if (semanticNotes != null) map.put("semantic_notes", semanticNotes);
        if (translation != null) map.put("translation", translation);
        return map;
    }

    /**
     * Create a minimal CommentDetails from a simple summary string.
     */
    public static CommentDetails fromSummary(String summary) {
        CommentDetails cd = new CommentDetails();
        cd.setSummary(summary != null ? summary : "");
        return cd;
    }
}
