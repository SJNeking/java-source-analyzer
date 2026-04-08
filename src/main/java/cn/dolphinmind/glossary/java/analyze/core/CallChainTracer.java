package cn.dolphinmind.glossary.java.analyze.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Core Feature 2: Call Chain Tracer (JavaParser-based, internal-only)
 *
 * Builds call graph ONLY for internal project classes.
 * External library calls (java.*, javax.*, org.*, com.*) are EXCLUDED.
 */
public class CallChainTracer {

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
        public String toArrowString() { return String.join(" → ", chain); }
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("entry_point", entryPoint);
            map.put("chain", chain);
            map.put("depth", depth);
            map.put("arrow", toArrowString());
            return map;
        }
    }

    public static class CallGraph {
        // Adjacency list: caller → set of internal callees
        private final Map<String, Set<String>> adjacencyList = new LinkedHashMap<>();
        // Set of all known internal class names (simple + full)
        private final Set<String> internalClasses = new HashSet<>();

        public void registerClass(String fullClassName) {
            internalClasses.add(fullClassName);
            int lastDot = fullClassName.lastIndexOf('.');
            if (lastDot > 0) internalClasses.add(fullClassName.substring(lastDot + 1));
        }

        public void addCall(String caller, String callee) {
            // Only add if callee is an internal class
            if (isInternalClass(callee)) {
                adjacencyList.computeIfAbsent(caller, k -> new LinkedHashSet<>()).add(callee);
            }
        }

        private boolean isInternalClass(String callee) {
            String className = callee.contains("#") ? callee.substring(0, callee.indexOf('#')) : callee;
            return internalClasses.contains(className);
        }

        public Map<String, Set<String>> getAdjacencyList() { return Collections.unmodifiableMap(adjacencyList); }
        public int getNodeCount() { return adjacencyList.size(); }
        public int getEdgeCount() { return adjacencyList.values().stream().mapToInt(Set::size).sum(); }
    }

    /**
     * Build call graph from all Java files in project.
     * Only tracks calls between INTERNAL project classes.
     */
    public CallGraph buildCallGraph(Path projectRoot) throws IOException {
        CallGraph graph = new CallGraph();

        // First pass: register all internal classes
        Files.walk(projectRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("src"))
                .filter(p -> !p.toString().contains("test") && !p.toString().contains("target"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String pkg = cu.getPackageDeclaration()
                                .map(pd -> pd.getNameAsString()).orElse("");
                        for (com.github.javaparser.ast.body.ClassOrInterfaceDeclaration c : cu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)) {
                            String full = (pkg.isEmpty() ? "" : pkg + ".") + c.getNameAsString();
                            graph.registerClass(full);
                        }
                    } catch (Exception e) {}
                });

        // Second pass: build call edges (only internal)
        Files.walk(projectRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("src"))
                .filter(p -> !p.toString().contains("test") && !p.toString().contains("target"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String pkg = cu.getPackageDeclaration()
                                .map(pd -> pd.getNameAsString()).orElse("");
                        String currentClass = pkg.isEmpty() ? "Unknown" : pkg;

                        for (com.github.javaparser.ast.body.ClassOrInterfaceDeclaration classDecl : cu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)) {
                            String className = classDecl.getNameAsString();
                            String fullClassName = (pkg.isEmpty() ? "" : pkg + ".") + className;

                            for (MethodDeclaration method : classDecl.getMethods()) {
                                String callerKey = fullClassName + "#" + method.getNameAsString();

                                method.findAll(MethodCallExpr.class).forEach(call -> {
                                    String methodName = call.getNameAsString();
                                    // Try to resolve the scope to a class name
                                    String targetClass = call.getScope()
                                            .map(s -> {
                                                String scopeStr = s.toString();
                                                // Handle "this.xxx" → current class
                                                if (scopeStr.equals("this")) return fullClassName;
                                                // Handle variable name → guess class from type
                                                return resolveVariableType(cu, scopeStr, fullClassName);
                                            })
                                            .orElse(fullClassName); // No scope = same class

                                    String calleeKey = targetClass + "#" + methodName;
                                    graph.addCall(callerKey, calleeKey);
                                });
                            }
                        }
                    } catch (Exception e) {}
                });

        return graph;
    }

    /**
     * Try to resolve a variable name to its type/class.
     * This is a simplified heuristic without full symbol resolution.
     */
    private String resolveVariableType(CompilationUnit cu, String varName, String currentClass) {
        // Check imports for the type
        for (com.github.javaparser.ast.ImportDeclaration imp : cu.getImports()) {
            String impName = imp.getNameAsString();
            if (impName.toLowerCase().contains(varName.toLowerCase())) {
                return impName;
            }
        }
        // If we can't resolve, assume it's an internal class (capitalize first letter)
        if (!varName.isEmpty()) {
            return Character.toUpperCase(varName.charAt(0)) + varName.substring(1);
        }
        return currentClass;
    }

    public List<CallChain> traceFrom(CallGraph graph, String entryPoint, int maxDepth) {
        List<CallChain> chains = new ArrayList<>();
        traceDFS(graph, entryPoint, new ArrayList<>(), new HashSet<>(), chains, maxDepth);
        return chains;
    }

    public Map<String, List<CallChain>> traceAll(CallGraph graph, List<String> entryPoints, int maxDepth) {
        Map<String, List<CallChain>> result = new LinkedHashMap<>();
        for (String ep : entryPoints) {
            List<CallChain> chains = traceFrom(graph, ep, maxDepth);
            if (!chains.isEmpty()) result.put(ep, chains);
        }
        return result;
    }

    private void traceDFS(CallGraph graph, String current, List<String> path,
                          Set<String> visited, List<CallChain> chains, int maxDepth) {
        if (path.size() > maxDepth) return;

        if (visited.contains(current) && !path.isEmpty()) {
            List<String> chainWithCycle = new ArrayList<>(path);
            chainWithCycle.add(current + "(循环)");
            chains.add(new CallChain(path.get(0), chainWithCycle));
            return;
        }

        path.add(current);
        visited.add(current);

        Set<String> callees = graph.getAdjacencyList().getOrDefault(current, Collections.emptySet());
        if (callees.isEmpty() || path.size() >= maxDepth) {
            chains.add(new CallChain(path.get(0), new ArrayList<>(path)));
        } else {
            for (String callee : callees) {
                traceDFS(graph, callee, path, new HashSet<>(visited), chains, maxDepth);
            }
        }

        path.remove(path.size() - 1);
    }

    public void printChains(Map<String, List<CallChain>> allChains) {
        if (allChains.isEmpty()) {
            System.out.println("  (未找到入口点调用链)");
            return;
        }
        System.out.println("\n=== 调用链路追踪 ===");
        for (Map.Entry<String, List<CallChain>> entry : allChains.entrySet()) {
            System.out.println("\n入口: " + entry.getKey() + " (" + entry.getValue().size() + " 条链路)");
            int shown = 0;
            for (CallChain chain : entry.getValue()) {
                if (shown >= 10) {
                    System.out.println("  ... 还有 " + (entry.getValue().size() - 10) + " 条");
                    break;
                }
                System.out.println("  " + chain.toArrowString());
                shown++;
            }
        }
    }
}
