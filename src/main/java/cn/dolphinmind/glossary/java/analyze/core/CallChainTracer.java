package cn.dolphinmind.glossary.java.analyze.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Core Feature 2: Call Chain Tracer
 *
 * From any entry point (main, @RequestMapping, @EventListener, etc.),
 * traces the complete call chain: A→B→C→D
 *
 * This is the key feature for understanding any Java project:
 * "用户下单" 的完整调用链是什么？
 * "定时任务" 调了哪些方法？
 * "消息消费" 的处理流程是什么？
 */
public class CallChainTracer {

    /**
     * A single call chain from entry point to leaf methods.
     */
    public static class CallChain {
        private final String entryPoint;
        private final List<String> chain;
        private final int depth;

        public CallChain(String entryPoint, List<String> chain) {
            this.entryPoint = entryPoint;
            this.chain = new ArrayList<>(chain);
            this.depth = chain.size();
        }

        public String getEntryPoint() { return entryPoint; }
        public List<String> getChain() { return Collections.unmodifiableList(chain); }
        public int getDepth() { return depth; }

        public String toArrowString() {
            return String.join(" → ", chain);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("entry_point", entryPoint);
            map.put("chain", chain);
            map.put("depth", depth);
            map.put("arrow", toArrowString());
            return map;
        }
    }

    /**
     * The call graph for an entire project.
     */
    public static class CallGraph {
        private final Map<String, Set<String>> adjacencyList = new LinkedHashMap<>();
        private final Map<String, String> methodToFile = new LinkedHashMap<>();

        public void addMethod(String className, String methodName, String filePath) {
            String key = className + "#" + methodName;
            methodToFile.put(key, filePath);
            adjacencyList.putIfAbsent(key, new LinkedHashSet<>());
        }

        public void addCall(String caller, String callee) {
            adjacencyList.computeIfAbsent(caller, k -> new LinkedHashSet<>()).add(callee);
        }

        public Map<String, Set<String>> getAdjacencyList() {
            return Collections.unmodifiableMap(adjacencyList);
        }

        public Map<String, String> getMethodToFile() {
            return Collections.unmodifiableMap(methodToFile);
        }

        public int getNodeCount() { return adjacencyList.size(); }
        public int getEdgeCount() {
            return adjacencyList.values().stream().mapToInt(Set::size).sum();
        }
    }

    /**
     * Build the complete call graph for a project.
     */
    public CallGraph buildCallGraph(Path projectRoot) throws IOException {
        CallGraph graph = new CallGraph();

        Files.walk(projectRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("src"))
                .filter(p -> !p.toString().contains("test"))
                .filter(p -> !p.toString().contains("target"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String className = cu.getPrimaryTypeName().orElse("Unknown");
                        String filePath = projectRoot.relativize(path).toString();

                        cu.findAll(MethodDeclaration.class).forEach(method -> {
                            String methodName = method.getNameAsString();
                            graph.addMethod(className, methodName, filePath);

                            // Extract method calls
                            method.findAll(MethodCallExpr.class).forEach(call -> {
                                String targetClass = call.getScope()
                                        .map(s -> extractTypeName(s.toString()))
                                        .orElse(className);
                                String targetMethod = call.getNameAsString();
                                graph.addCall(className + "#" + methodName,
                                        targetClass + "#" + targetMethod);
                            });
                        });
                    } catch (Exception e) {
                        // ignore parse errors
                    }
                });

        return graph;
    }

    /**
     * Trace call chains from a specific entry point.
     * Limits depth to prevent infinite loops in recursive calls.
     */
    public List<CallChain> traceFrom(CallGraph graph, String entryPoint, int maxDepth) {
        List<CallChain> chains = new ArrayList<>();
        traceDFS(graph, entryPoint, new ArrayList<>(), new HashSet<>(), chains, maxDepth);
        return chains;
    }

    /**
     * Trace call chains from multiple entry points.
     */
    public Map<String, List<CallChain>> traceAll(CallGraph graph, List<String> entryPoints, int maxDepth) {
        Map<String, List<CallChain>> result = new LinkedHashMap<>();
        for (String ep : entryPoints) {
            List<CallChain> chains = traceFrom(graph, ep, maxDepth);
            if (!chains.isEmpty()) {
                result.put(ep, chains);
            }
        }
        return result;
    }

    private void traceDFS(CallGraph graph, String current, List<String> path,
                          Set<String> visited, List<CallChain> chains, int maxDepth) {
        if (path.size() > maxDepth) return;
        if (visited.contains(current) && path.size() > 0) {
            // Found a cycle - record the chain up to the cycle
            List<String> chainWithCycle = new ArrayList<>(path);
            chainWithCycle.add(current + "(recursive)");
            chains.add(new CallChain(path.get(0), chainWithCycle));
            return;
        }

        path.add(current);
        visited.add(current);

        Set<String> callees = graph.getAdjacencyList().getOrDefault(current, Collections.emptySet());
        if (callees.isEmpty() || path.size() >= maxDepth) {
            // Leaf method or max depth reached
            chains.add(new CallChain(path.get(0), new ArrayList<>(path)));
        } else {
            for (String callee : callees) {
                traceDFS(graph, callee, path, new HashSet<>(visited), chains, maxDepth);
            }
        }

        path.remove(path.size() - 1);
    }

    /**
     * Extract simple type name from a scope expression.
     * e.g., "userService" → "UserService", "this.userService" → "UserService"
     */
    private String extractTypeName(String scope) {
        // Handle "this.xxx" → "xxx"
        if (scope.startsWith("this.")) {
            scope = scope.substring(5);
        }
        // Capitalize first letter: "userService" → "UserService"
        if (!scope.isEmpty()) {
            return Character.toUpperCase(scope.charAt(0)) + scope.substring(1);
        }
        return scope;
    }

    /**
     * Print call chains in a readable format.
     */
    public void printChains(Map<String, List<CallChain>> allChains) {
        System.out.println("\n=== 调用链路追踪 ===");
        for (Map.Entry<String, List<CallChain>> entry : allChains.entrySet()) {
            System.out.println("\n入口: " + entry.getKey() + " (" + entry.getValue().size() + " 条链路)");
            int shown = 0;
            for (CallChain chain : entry.getValue()) {
                if (shown >= 10) {
                    System.out.println("  ... 还有 " + (entry.getValue().size() - 10) + " 条链路");
                    break;
                }
                System.out.println("  " + chain.toArrowString());
                shown++;
            }
        }
    }
}
