package cn.dolphinmind.glossary.java.analyze.core;

import java.util.*;

/**
 * Strong-typed result model for CoreAnalysisEngine.
 *
 * Wraps all sub-analyses: entry points, call graph, package structure,
 * type index, data flows, and external library signatures.
 * Has toMap() for backward compatibility with existing JSON output.
 */
public class CoreAnalysisResult {

    // Entry points
    private int totalEntryPoints;
    private Map<String, List<Map<String, Object>>> entryPointsByType;

    // Call graph
    private CallGraphSummary callGraph;

    // Call chains from entry points
    private Map<String, List<Map<String, Object>>> callChains;

    // Package structure
    private Map<String, Object> packageStructure;

    // Type definition index
    private int indexedTypes;

    // Data flow
    private int totalDataFlows;
    private List<Map<String, Object>> dataFlows;

    // External library signatures
    private int jarsScanned;
    private int classesScanned;
    private int methodsIndexed;

    // Warnings (e.g., bytecode analysis unavailable)
    private List<String> warnings = new ArrayList<>();

    public CoreAnalysisResult() {}

    public int getTotalEntryPoints() { return totalEntryPoints; }
    public void setTotalEntryPoints(int totalEntryPoints) { this.totalEntryPoints = totalEntryPoints; }

    public Map<String, List<Map<String, Object>>> getEntryPointsByType() { return entryPointsByType; }
    public void setEntryPointsByType(Map<String, List<Map<String, Object>>> entryPointsByType) { this.entryPointsByType = entryPointsByType; }

    public CallGraphSummary getCallGraph() { return callGraph; }
    public void setCallGraph(CallGraphSummary callGraph) { this.callGraph = callGraph; }

    public Map<String, List<Map<String, Object>>> getCallChains() { return callChains; }
    public void setCallChains(Map<String, List<Map<String, Object>>> callChains) { this.callChains = callChains; }

    public Map<String, Object> getPackageStructure() { return packageStructure; }
    public void setPackageStructure(Map<String, Object> packageStructure) { this.packageStructure = packageStructure; }

    public int getIndexedTypes() { return indexedTypes; }
    public void setIndexedTypes(int indexedTypes) { this.indexedTypes = indexedTypes; }

    public int getTotalDataFlows() { return totalDataFlows; }
    public void setTotalDataFlows(int totalDataFlows) { this.totalDataFlows = totalDataFlows; }

    public List<Map<String, Object>> getDataFlows() { return dataFlows; }
    public void setDataFlows(List<Map<String, Object>> dataFlows) { this.dataFlows = dataFlows; }

    public int getJarsScanned() { return jarsScanned; }
    public void setJarsScanned(int jarsScanned) { this.jarsScanned = jarsScanned; }

    public int getClassesScanned() { return classesScanned; }
    public void setClassesScanned(int classesScanned) { this.classesScanned = classesScanned; }

    public int getMethodsIndexed() { return methodsIndexed; }
    public void setMethodsIndexed(int methodsIndexed) { this.methodsIndexed = methodsIndexed; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public void addWarning(String warning) { this.warnings.add(warning); }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Entry points
        Map<String, Object> entryInfo = new LinkedHashMap<>();
        entryInfo.put("total", totalEntryPoints);
        if (entryPointsByType != null) entryInfo.put("by_type", entryPointsByType);
        result.put("entry_points", entryInfo);

        // Call graph
        if (callGraph != null) result.put("call_graph", callGraph.toMap());

        // Call chains
        if (callChains != null) result.put("call_chains", callChains);

        // Package structure
        if (packageStructure != null) result.put("package_structure", packageStructure);

        // Type index
        result.put("type_index", Collections.singletonMap("indexed_types", indexedTypes));

        // Data flow
        Map<String, Object> flowInfo = new LinkedHashMap<>();
        flowInfo.put("total", totalDataFlows);
        if (dataFlows != null) flowInfo.put("flows", dataFlows);
        result.put("data_flow", flowInfo);

        // External library signatures
        Map<String, Object> sigInfo = new LinkedHashMap<>();
        sigInfo.put("jars_scanned", jarsScanned);
        sigInfo.put("classes_scanned", classesScanned);
        sigInfo.put("methods_indexed", methodsIndexed);
        result.put("external_signatures", sigInfo);

        // Warnings
        if (!warnings.isEmpty()) result.put("warnings", warnings);

        return result;
    }

    /**
     * Summary of call graph statistics.
     */
    public static class CallGraphSummary {
        private int sourceResolved;
        private int sourceUnresolved;
        private int sourceSkipped;
        private int bytecodeResolved;
        private int bytecodeExternal;
        private int progressiveEnriched;
        private int nodeCount;
        private int edgeCount;
        private String mergeStrategy;

        public CallGraphSummary() {}

        public int getSourceResolved() { return sourceResolved; }
        public void setSourceResolved(int sourceResolved) { this.sourceResolved = sourceResolved; }

        public int getSourceUnresolved() { return sourceUnresolved; }
        public void setSourceUnresolved(int sourceUnresolved) { this.sourceUnresolved = sourceUnresolved; }

        public int getSourceSkipped() { return sourceSkipped; }
        public void setSourceSkipped(int sourceSkipped) { this.sourceSkipped = sourceSkipped; }

        public int getBytecodeResolved() { return bytecodeResolved; }
        public void setBytecodeResolved(int bytecodeResolved) { this.bytecodeResolved = bytecodeResolved; }

        public int getBytecodeExternal() { return bytecodeExternal; }
        public void setBytecodeExternal(int bytecodeExternal) { this.bytecodeExternal = bytecodeExternal; }

        public int getProgressiveEnriched() { return progressiveEnriched; }
        public void setProgressiveEnriched(int progressiveEnriched) { this.progressiveEnriched = progressiveEnriched; }

        public int getNodeCount() { return nodeCount; }
        public void setNodeCount(int nodeCount) { this.nodeCount = nodeCount; }

        public int getEdgeCount() { return edgeCount; }
        public void setEdgeCount(int edgeCount) { this.edgeCount = edgeCount; }

        public String getMergeStrategy() { return mergeStrategy; }
        public void setMergeStrategy(String mergeStrategy) { this.mergeStrategy = mergeStrategy; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            Map<String, Object> sourceLayer = new LinkedHashMap<>();
            sourceLayer.put("resolved_internal", sourceResolved);
            sourceLayer.put("unresolved_fallback", sourceUnresolved);
            sourceLayer.put("skipped_external", sourceSkipped);
            map.put("source_layer", sourceLayer);

            Map<String, Object> bytecodeLayer = new LinkedHashMap<>();
            bytecodeLayer.put("resolved_internal", bytecodeResolved);
            bytecodeLayer.put("skipped_external", bytecodeExternal);
            map.put("bytecode_layer", bytecodeLayer);

            map.put("node_count", nodeCount);
            map.put("edge_count", edgeCount);
            map.put("progressive_enriched", progressiveEnriched);
            if (mergeStrategy != null) map.put("merge_strategy", mergeStrategy);
            return map;
        }
    }
}
