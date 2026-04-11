package cn.dolphinmind.glossary.java.analyze.core;

import cn.dolphinmind.glossary.java.analyze.orchestrate.AnalysisConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Shared context passed to all AnalyzerModule implementations.
 * Carries configuration, collected assets, dependencies, and progress reporting.
 */
public class AnalysisContext {

    private final AnalysisConfig config;
    private Path sourceRoot;
    private Path outputDir;
    private String frameworkName;
    private String version;

    // Collected assets from earlier pipeline stages
    private List<Map<String, Object>> classAssets;
    private List<Map<String, String>> dependencies;
    private Map<String, Object> projectAssets;
    private Map<String, Object> rootContainer;

    // Cross-cutting concerns
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private Map<String, Object> sharedData = new LinkedHashMap<>();

    public AnalysisContext(AnalysisConfig config) {
        this.config = config;
        if (config != null) {
            this.sourceRoot = Paths.get(config.getSourceRoot());
            this.outputDir = config.getOutputDirPath();
            this.version = config.getEffectiveVersion();
        }
    }

    // Getters
    public AnalysisConfig getConfig() { return config; }
    public Path getSourceRoot() { return sourceRoot; }
    public void setSourceRoot(Path sourceRoot) { this.sourceRoot = sourceRoot; }

    public Path getOutputDir() { return outputDir; }
    public void setOutputDir(Path outputDir) { this.outputDir = outputDir; }

    public String getFrameworkName() { return frameworkName; }
    public void setFrameworkName(String frameworkName) { this.frameworkName = frameworkName; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public List<Map<String, Object>> getClassAssets() { return classAssets; }
    public void setClassAssets(List<Map<String, Object>> classAssets) { this.classAssets = classAssets; }

    public List<Map<String, String>> getDependencies() { return dependencies; }
    public void setDependencies(List<Map<String, String>> dependencies) { this.dependencies = dependencies; }

    public Map<String, Object> getProjectAssets() { return projectAssets; }
    public void setProjectAssets(Map<String, Object> projectAssets) { this.projectAssets = projectAssets; }

    public Map<String, Object> getRootContainer() { return rootContainer; }
    public void setRootContainer(Map<String, Object> rootContainer) { this.rootContainer = rootContainer; }

    public List<String> getWarnings() { return warnings; }
    public void addWarning(String warning) { this.warnings.add(warning); }

    public List<String> getErrors() { return errors; }
    public void addError(String error) { this.errors.add(error); }

    public Map<String, Object> getSharedData() { return sharedData; }
    public void putSharedData(String key, Object value) { this.sharedData.put(key, value); }

    @SuppressWarnings("unchecked")
    public <V> V getSharedData(String key, V defaultValue) {
        return (V) sharedData.getOrDefault(key, defaultValue);
    }
}
