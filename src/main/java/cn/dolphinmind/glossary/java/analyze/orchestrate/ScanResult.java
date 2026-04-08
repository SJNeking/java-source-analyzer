package cn.dolphinmind.glossary.java.analyze.orchestrate;

import java.util.*;

/**
 * Holds intermediate scan results during pipeline execution.
 */
public class ScanResult {
    private final List<Map<String, Object>> globalLibrary = new ArrayList<>();
    private final Map<String, List<Map<String, Object>>> moduleLibrary = new LinkedHashMap<>();
    private final List<Map<String, String>> globalDependencies = new ArrayList<>();

    private int filesScanned;
    private int filesSkipped;
    private int filesFailed;

    public List<Map<String, Object>> getGlobalLibrary() {
        return Collections.unmodifiableList(globalLibrary);
    }

    public void addClassAsset(Map<String, Object> asset, String moduleName) {
        synchronized (globalLibrary) {
            globalLibrary.add(asset);
        }
        synchronized (moduleLibrary) {
            moduleLibrary.computeIfAbsent(moduleName, k -> new ArrayList<>()).add(asset);
        }
    }

    public Map<String, List<Map<String, Object>>> getModuleLibrary() {
        return Collections.unmodifiableMap(moduleLibrary);
    }

    public List<Map<String, String>> getGlobalDependencies() {
        return Collections.unmodifiableList(globalDependencies);
    }

    public void addDependency(Map<String, String> dep) {
        synchronized (globalDependencies) {
            globalDependencies.add(dep);
        }
    }

    public int getFilesScanned() { return filesScanned; }
    public void setFilesScanned(int filesScanned) { this.filesScanned = filesScanned; }
    public int getFilesSkipped() { return filesSkipped; }
    public void setFilesSkipped(int filesSkipped) { this.filesSkipped = filesSkipped; }
    public int getFilesFailed() { return filesFailed; }
    public void setFilesFailed(int filesFailed) { this.filesFailed = filesFailed; }
}
