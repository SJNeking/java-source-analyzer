package cn.dolphinmind.glossary.java.analyze.dataflow;

import java.util.*;

/**
 * Cross-method taint analysis engine.
 *
 * Tracks tainted data across method calls:
 * 1. Source method returns tainted data (e.g., request.getParameter())
 * 2. Tainted data passed as argument to other methods
 * 3. Sink method receives tainted data (e.g., executeQuery(tainted))
 *
 * Uses the CALLS dependency graph from the scanner to trace data flow
 * across method boundaries.
 */
public class CrossMethodTaintAnalyzer {

    /**
     * A node in the taint propagation graph.
     */
    public static class TaintNode {
        private final String className;
        private final String methodName;
        private final String taintedVar;
        private final int line;
        private final List<TaintEdge> outgoing = new ArrayList<>();

        public TaintNode(String className, String methodName, String taintedVar, int line) {
            this.className = className;
            this.methodName = methodName;
            this.taintedVar = taintedVar;
            this.line = line;
        }

        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public String getTaintedVar() { return taintedVar; }
        public int getLine() { return line; }
        public List<TaintEdge> getOutgoing() { return Collections.unmodifiableList(outgoing); }

        public void addOutgoing(TaintEdge edge) { outgoing.add(edge); }

        @Override
        public String toString() {
            return className + "#" + methodName + "(" + taintedVar + ")@" + line;
        }
    }

    /**
     * An edge in the taint propagation graph.
     */
    public static class TaintEdge {
        private final TaintNode target;
        private final String propagationType; // ARGUMENT, RETURN_VALUE, FIELD

        public TaintEdge(TaintNode target, String propagationType) {
            this.target = target;
            this.propagationType = propagationType;
        }

        public TaintNode getTarget() { return target; }
        public String getPropagationType() { return propagationType; }
    }

    /**
     * A complete taint path from source to sink across methods.
     */
    public static class CrossMethodTaintPath {
        private final TaintNode source;
        private final List<TaintNode> path;
        private final TaintNode sink;
        private final String vulnerabilityType;

        public CrossMethodTaintPath(TaintNode source, List<TaintNode> path, TaintNode sink, String vulnerabilityType) {
            this.source = source;
            this.path = new ArrayList<>(path);
            this.sink = sink;
            this.vulnerabilityType = vulnerabilityType;
        }

        public TaintNode getSource() { return source; }
        public List<TaintNode> getPath() { return Collections.unmodifiableList(path); }
        public TaintNode getSink() { return sink; }
        public String getVulnerabilityType() { return vulnerabilityType; }
        public int getMethodDepth() { return path.size(); }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(source.getClassName()).append("#").append(source.getMethodName());
            for (TaintNode node : path) {
                sb.append(" → ").append(node.getClassName()).append("#").append(node.getMethodName());
            }
            sb.append(" → [").append(vulnerabilityType).append("]");
            return sb.toString();
        }
    }

    /**
     * Analyze cross-method taint flows.
     *
     * @param methodAssets List of method assets with source_code and key_statements
     * @param callGraph List of CALLS dependencies (source → target)
     * @return List of cross-method taint paths
     */
    @SuppressWarnings("unchecked")
    public List<CrossMethodTaintPath> analyze(List<Map<String, Object>> methodAssets,
                                               List<Map<String, String>> callGraph) {
        List<CrossMethodTaintPath> findings = new ArrayList<CrossMethodTaintPath>();

        // Build method lookup: address → method asset
        Map<String, Map<String, Object>> methodLookup = new HashMap<String, Map<String, Object>>();
        for (Map<String, Object> method : methodAssets) {
            String address = (String) method.getOrDefault("address", "");
            if (!address.isEmpty()) methodLookup.put(address, method);
        }

        // Build call graph: sourceAddress → [targetAddresses]
        Map<String, List<String>> callsMap = new HashMap<>();
        for (Map<String, String> dep : callGraph) {
            String type = dep.getOrDefault("type", "");
            if (!"CALLS".equals(type)) continue;
            String source = dep.getOrDefault("source", "");
            String target = dep.getOrDefault("target", "");
            if (!source.isEmpty() && !target.isEmpty()) {
                callsMap.computeIfAbsent(source, k -> new ArrayList<>()).add(target);
            }
        }

        // Find source methods (methods that return tainted data)
        List<String> sourceMethods = findSourceMethods(methodLookup);

        // For each source method, trace taint propagation through call graph
        for (String sourceAddr : sourceMethods) {
            Map<String, Object> sourceMethod = methodLookup.get(sourceAddr);
            if (sourceMethod == null) continue;

            String sourceClassName = extractClassName(sourceAddr);
            String sourceMethodName = extractMethodName(sourceAddr);

            // Find sink reachable from this source
            Set<String> visited = new HashSet<String>();
            List<CrossMethodTaintPath> paths = traceTaintFromSource(
                    sourceClassName, sourceMethodName, sourceAddr,
                    callsMap, methodLookup, visited, new ArrayList<CrossMethodTaintAnalyzer.TaintNode>(), 0, 5);

            findings.addAll(paths);
        }

        return findings;
    }

    /**
     * Find methods that are known sources of tainted data.
     */
    private List<String> findSourceMethods(Map<String, Map<String, Object>> methodLookup) {
        List<String> sources = new ArrayList<String>();
        for (Map.Entry<String, Map<String, Object>> entry : methodLookup.entrySet()) {
            String address = entry.getKey();
            Map<String, Object> method = entry.getValue();
            Object bodyObj = method.get("body_code");
            if (!(bodyObj instanceof String)) continue;
            String body = (String) bodyObj;

            // Check if method body contains source patterns
            if (body.contains("getParameter") || body.contains("getHeader") ||
                body.contains("getInputStream") || body.contains("getReader") ||
                body.contains("readLine") || body.contains("readObject") ||
                body.contains("getProperty") || body.contains("getenv")) {
                sources.add(address);
            }
        }
        return sources;
    }

    /**
     * Trace taint propagation from a source method through the call graph.
     */
    private List<CrossMethodTaintPath> traceTaintFromSource(
            String sourceClassName, String sourceMethodName, String sourceAddr,
            Map<String, List<String>> callsMap, Map<String, Map<String, Object>> methodLookup,
            Set<String> visited, List<TaintNode> path, int depth, int maxDepth) {

        List<CrossMethodTaintPath> findings = new ArrayList<CrossMethodTaintPath>();

        if (depth > maxDepth || visited.contains(sourceAddr)) return findings;
        visited.add(sourceAddr);

        TaintNode currentNode = new TaintNode(sourceClassName, sourceMethodName, "tainted", 0);
        path.add(currentNode);

        // Check if current method contains sinks
        Map<String, Object> method = methodLookup.get(sourceAddr);
        if (method != null) {
            String body = (String) method.getOrDefault("body_code", "");
            String vulnType = checkForSink(body);
            if (vulnType != null && depth > 0) {
                // Found a sink in the current method
                TaintNode sinkNode = new TaintNode(sourceClassName, sourceMethodName, vulnType, 0);
                findings.add(new CrossMethodTaintPath(
                        new TaintNode(path.get(0).getClassName(), path.get(0).getMethodName(), "source", 0),
                        new ArrayList<TaintNode>(path.subList(1, path.size())),
                        sinkNode, vulnType));
            }
        }

        // Follow call graph to find more sinks
        List<String> callees = callsMap.getOrDefault(sourceAddr, Collections.<String>emptyList());
        for (String callee : callees) {
            String calleeClassName = extractClassName(callee);
            String calleeMethodName = extractMethodName(callee);

            findings.addAll(traceTaintFromSource(
                    calleeClassName, calleeMethodName, callee,
                    callsMap, methodLookup, visited, path, depth + 1, maxDepth));
        }

        path.remove(path.size() - 1);
        visited.remove(sourceAddr);

        return findings;
    }

    /**
     * Check if method body contains sink patterns.
     */
    private String checkForSink(String body) {
        if (body == null || body.isEmpty()) return null;

        // SQL injection
        if ((body.contains("executeQuery") || body.contains("executeUpdate") ||
             body.contains("createStatement") || body.contains("prepareStatement")) &&
            (body.contains("+ \"") || body.contains("String.format") || body.contains("concat"))) {
            return "SQL Injection";
        }

        // Command injection
        if (body.contains("Runtime.getRuntime") && body.contains("exec")) {
            return "Command Injection";
        }

        // Path traversal
        if ((body.contains("new File(") || body.contains("Paths.get(") || body.contains("new FileInputStream(")) &&
            (body.contains("+ \"") || body.contains("getParameter"))) {
            return "Path Traversal";
        }

        // SSRF
        if (body.contains("openConnection") && body.contains("getParameter")) {
            return "SSRF";
        }

        return null;
    }

    private String extractClassName(String address) {
        int hashIdx = address.indexOf('#');
        return hashIdx > 0 ? address.substring(0, hashIdx) : address;
    }

    private String extractMethodName(String address) {
        int hashIdx = address.indexOf('#');
        return hashIdx > 0 ? address.substring(hashIdx + 1) : address;
    }
}
