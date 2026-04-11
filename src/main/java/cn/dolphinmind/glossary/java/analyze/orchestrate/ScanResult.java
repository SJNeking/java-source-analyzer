package cn.dolphinmind.glossary.java.analyze.orchestrate;

import cn.dolphinmind.glossary.java.analyze.translate.SemanticTranslator;

import java.util.*;

/**
 * Holds all intermediate results of the analysis pipeline.
 * Replaces scattered local variables in SourceUniversePro.runAnalysis().
 *
 * Each phase populates its section. Output generators consume this object.
 */
public class ScanResult {

    // Phase 1: Preparation
    private String frameworkName;
    private String detectedVersion;
    private String safeVersion;
    private String dateStr;
    private Map<String, Object> rootContainer = new LinkedHashMap<>();

    // Phase 2: Java Scanning
    private List<Map<String, Object>> classAssets = new ArrayList<>();
    private Map<String, List<Map<String, Object>>> moduleLibrary = new LinkedHashMap<>();
    private List<Map<String, String>> dependencies = new ArrayList<>();

    // Phase 3: Cache merge results
    private int filesScanned;
    private int filesFailed;
    private int filesSkipped;

    // Phase 4: Non-Java assets
    private Map<String, Object> projectAssets = new LinkedHashMap<>();

    // Phase 5: Cross-file relations
    private Map<String, Object> relationData = new LinkedHashMap<>();
    private int relationCount;

    // Phase 6: Quality analysis
    private List<Map<String, Object>> qualityIssues = new ArrayList<>();
    private Map<String, Object> qualitySummary = new LinkedHashMap<>();

    // Phase 7: Project type detection
    private Map<String, Object> projectTypeInfo = new LinkedHashMap<>();

    // Phase 8: Framework analysis
    private Map<String, Object> springData;
    private Map<String, Object> architectureLayerData;
    private int architectureViolationCount;

    // Phase 9: Reporting
    private Map<String, Object> technicalDebt;
    private Map<String, Object> qualityGate;

    // Phase 10: Metrics
    private Map<String, Object> codeMetrics;
    private Map<String, Object> dependencyGraph;

    // Phase 11: Core analysis
    private Map<String, Object> coreAnalysis;

    // Diagnostics
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    // Getters / Setters
    public String getFrameworkName() { return frameworkName; }
    public void setFrameworkName(String frameworkName) { this.frameworkName = frameworkName; }

    public String getDetectedVersion() { return detectedVersion; }
    public void setDetectedVersion(String detectedVersion) { this.detectedVersion = detectedVersion; }

    public String getSafeVersion() { return safeVersion; }
    public void setSafeVersion(String safeVersion) { this.safeVersion = safeVersion; }

    public String getDateStr() { return dateStr; }
    public void setDateStr(String dateStr) { this.dateStr = dateStr; }

    public Map<String, Object> getRootContainer() { return rootContainer; }
    public void setRootContainer(Map<String, Object> rootContainer) { this.rootContainer = rootContainer; }

    public List<Map<String, Object>> getClassAssets() { return classAssets; }
    public void setClassAssets(List<Map<String, Object>> classAssets) { this.classAssets = classAssets; }

    public Map<String, List<Map<String, Object>>> getModuleLibrary() { return moduleLibrary; }
    public void setModuleLibrary(Map<String, List<Map<String, Object>>> moduleLibrary) { this.moduleLibrary = moduleLibrary; }

    public List<Map<String, String>> getDependencies() { return dependencies; }
    public void setDependencies(List<Map<String, String>> dependencies) { this.dependencies = dependencies; }

    public int getFilesScanned() { return filesScanned; }
    public void setFilesScanned(int filesScanned) { this.filesScanned = filesScanned; }

    public int getFilesFailed() { return filesFailed; }
    public void setFilesFailed(int filesFailed) { this.filesFailed = filesFailed; }

    public int getFilesSkipped() { return filesSkipped; }
    public void setFilesSkipped(int filesSkipped) { this.filesSkipped = filesSkipped; }

    public Map<String, Object> getProjectAssets() { return projectAssets; }
    public void setProjectAssets(Map<String, Object> projectAssets) { this.projectAssets = projectAssets; }

    public Map<String, Object> getRelationData() { return relationData; }
    public void setRelationData(Map<String, Object> relationData) { this.relationData = relationData; }

    public int getRelationCount() { return relationCount; }
    public void setRelationCount(int relationCount) { this.relationCount = relationCount; }

    public List<Map<String, Object>> getQualityIssues() { return qualityIssues; }
    public void setQualityIssues(List<Map<String, Object>> qualityIssues) { this.qualityIssues = qualityIssues; }

    public Map<String, Object> getQualitySummary() { return qualitySummary; }
    public void setQualitySummary(Map<String, Object> qualitySummary) { this.qualitySummary = qualitySummary; }

    public Map<String, Object> getProjectTypeInfo() { return projectTypeInfo; }
    public void setProjectTypeInfo(Map<String, Object> projectTypeInfo) { this.projectTypeInfo = projectTypeInfo; }

    public Map<String, Object> getSpringData() { return springData; }
    public void setSpringData(Map<String, Object> springData) { this.springData = springData; }

    public Map<String, Object> getArchitectureLayerData() { return architectureLayerData; }
    public void setArchitectureLayerData(Map<String, Object> architectureLayerData) { this.architectureLayerData = architectureLayerData; }

    public int getArchitectureViolationCount() { return architectureViolationCount; }
    public void setArchitectureViolationCount(int architectureViolationCount) { this.architectureViolationCount = architectureViolationCount; }

    public Map<String, Object> getTechnicalDebt() { return technicalDebt; }
    public void setTechnicalDebt(Map<String, Object> technicalDebt) { this.technicalDebt = technicalDebt; }

    public Map<String, Object> getQualityGate() { return qualityGate; }
    public void setQualityGate(Map<String, Object> qualityGate) { this.qualityGate = qualityGate; }

    public Map<String, Object> getCodeMetrics() { return codeMetrics; }
    public void setCodeMetrics(Map<String, Object> codeMetrics) { this.codeMetrics = codeMetrics; }

    public Map<String, Object> getDependencyGraph() { return dependencyGraph; }
    public void setDependencyGraph(Map<String, Object> dependencyGraph) { this.dependencyGraph = dependencyGraph; }

    public Map<String, Object> getCoreAnalysis() { return coreAnalysis; }
    public void setCoreAnalysis(Map<String, Object> coreAnalysis) { this.coreAnalysis = coreAnalysis; }

    public List<String> getWarnings() { return warnings; }
    public void addWarning(String w) { this.warnings.add(w); }

    public List<String> getErrors() { return errors; }
    public void addError(String e) { this.errors.add(e); }

    /**
     * Total method count across all class assets.
     */
    public int getTotalMethods() {
        return classAssets.stream()
                .mapToInt(a -> ((List<?>) a.getOrDefault("methods_full", Collections.emptyList())).size())
                .sum();
    }

    /**
     * Total field count across all class assets.
     */
    public int getTotalFields() {
        return classAssets.stream()
                .mapToInt(a -> ((List<?>) a.getOrDefault("fields_matrix", Collections.emptyList())).size())
                .sum();
    }
}
