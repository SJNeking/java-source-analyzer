package cn.dolphinmind.glossary.java.analyze.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Core Feature 2: Call Chain Tracer (JavaParser Symbol Solver-based)
 *
 * Uses JavaParser's Symbol Solver to accurately resolve method call targets.
 * Only tracks calls between INTERNAL project classes.
 * External library calls are filtered out.
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
        private final Map<String, Set<String>> adjacencyList = new LinkedHashMap<>();
        private final Set<String> internalClasses = new HashSet<>();
        private int skippedExternal = 0;
        private int resolvedInternal = 0;
        private int unresolvedFallback = 0;

        public void registerClass(String fullClassName) {
            internalClasses.add(fullClassName);
            int lastDot = fullClassName.lastIndexOf('.');
            if (lastDot > 0) internalClasses.add(fullClassName.substring(lastDot + 1));
        }

        public void addCall(String caller, String callee, boolean isResolved) {
            if (isInternalClass(callee)) {
                adjacencyList.computeIfAbsent(caller, k -> new LinkedHashSet<>()).add(callee);
                if (isResolved) resolvedInternal++;
            } else {
                skippedExternal++;
            }
        }

        public void addUnresolvedFallback(String caller, String callee) {
            unresolvedFallback++;
            addCall(caller, callee, false);
        }

        private boolean isInternalClass(String callee) {
            String className = callee.contains("#") ? callee.substring(0, callee.indexOf('#')) : callee;
            return internalClasses.contains(className);
        }

        public Map<String, Set<String>> getAdjacencyList() { return Collections.unmodifiableMap(adjacencyList); }
        public Set<String> getInternalClasses() { return Collections.unmodifiableSet(internalClasses); }
        public int getNodeCount() { return adjacencyList.size(); }
        public int getEdgeCount() { return adjacencyList.values().stream().mapToInt(Set::size).sum(); }
        public int getSkippedExternal() { return skippedExternal; }
        public int getResolvedInternal() { return resolvedInternal; }
        public int getUnresolvedFallback() { return unresolvedFallback; }
    }

    /**
     * Build call graph from all Java files in project.
     * Uses Symbol Solver for accurate resolution.
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

        // Second pass: build call edges using Symbol Solver
        Files.walk(projectRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("src"))
                .filter(p -> !p.toString().contains("test") && !p.toString().contains("target"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String pkg = cu.getPackageDeclaration()
                                .map(pd -> pd.getNameAsString()).orElse("");

                        for (com.github.javaparser.ast.body.ClassOrInterfaceDeclaration classDecl : cu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)) {
                            String className = classDecl.getNameAsString();
                            String fullClassName = (pkg.isEmpty() ? "" : pkg + ".") + className;

                            for (MethodDeclaration method : classDecl.getMethods()) {
                                String callerKey = fullClassName + "#" + method.getNameAsString();

                                method.findAll(MethodCallExpr.class).forEach(call -> {
                                    String methodName = call.getNameAsString();
                                    
                                    // Try Symbol Solver first
                                    try {
                                        ResolvedMethodDeclaration resolved = call.resolve();
                                        String targetClass = resolved.declaringType().getQualifiedName();
                                        String calleeKey = targetClass + "#" + methodName;
                                        graph.addCall(callerKey, calleeKey, true);
                                    } catch (Exception e) {
                                        // Symbol Solver failed - fallback to heuristic
                                        String targetClass = call.getScope()
                                                .map(s -> {
                                                    String scopeStr = s.toString();
                                                    if (scopeStr.equals("this")) return fullClassName;
                                                    return resolveVariableTypeFallback(cu, scopeStr, fullClassName);
                                                })
                                                .orElse(fullClassName);
                                        String calleeKey = targetClass + "#" + methodName;
                                        graph.addUnresolvedFallback(callerKey, calleeKey);
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {}
                });

        return graph;
    }

    /**
     * Fallback variable type resolution when Symbol Solver fails.
     * Uses import statements and naming conventions.
     */
    private String resolveVariableTypeFallback(CompilationUnit cu, String varName, String currentClass) {
        // Check local variable declarations in the method
        for (com.github.javaparser.ast.body.MethodDeclaration method : cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class)) {
            for (com.github.javaparser.ast.body.Parameter param : method.getParameters()) {
                if (param.getNameAsString().equals(varName)) {
                    return resolveTypeFromDeclaration(param.getType().toString(), cu);
                }
            }
            // Check local variable declarations
            for (com.github.javaparser.ast.stmt.ExpressionStmt stmt : method.findAll(com.github.javaparser.ast.stmt.ExpressionStmt.class)) {
                String expr = stmt.getExpression().toString();
                if (expr.startsWith(varName + " =") || expr.equals(varName)) {
                    // Try to find the type from the declaration
                }
            }
        }

        // Check field declarations
        for (com.github.javaparser.ast.body.ClassOrInterfaceDeclaration classDecl : cu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)) {
            for (com.github.javaparser.ast.body.FieldDeclaration field : classDecl.getFields()) {
                for (com.github.javaparser.ast.body.VariableDeclarator var : field.getVariables()) {
                    if (var.getNameAsString().equals(varName)) {
                        return resolveTypeFromDeclaration(field.getCommonType().toString(), cu);
                    }
                }
            }
        }

        // Last resort: capitalize first letter
        if (!varName.isEmpty()) {
            return Character.toUpperCase(varName.charAt(0)) + varName.substring(1);
        }
        return currentClass;
    }

    private String resolveTypeFromDeclaration(String typeStr, CompilationUnit cu) {
        // Handle generic types: List<User> → User
        if (typeStr.contains("<")) {
            typeStr = typeStr.substring(0, typeStr.indexOf('<'));
        }
        // Handle arrays: User[] → User
        if (typeStr.endsWith("[]")) {
            typeStr = typeStr.substring(0, typeStr.length() - 2);
        }
        // If simple name, try to resolve from imports
        if (!typeStr.contains(".")) {
            for (com.github.javaparser.ast.ImportDeclaration imp : cu.getImports()) {
                String impName = imp.getNameAsString();
                if (impName.endsWith("." + typeStr)) {
                    return impName;
                }
            }
        }
        return typeStr;
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

    public void printChains(Map<String, List<CallChain>> allChains, CallGraph graph) {
        if (allChains.isEmpty()) {
            System.out.println("  (未找到入口点调用链)");
            return;
        }
        System.out.println("\n=== 调用链路追踪 ===");
        System.out.println("  解析统计: " + graph.getResolvedInternal() + " 内部调用已解析, " +
                graph.getUnresolvedFallback() + " 使用回退, " +
                graph.getSkippedExternal() + " 外部库已跳过");

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
