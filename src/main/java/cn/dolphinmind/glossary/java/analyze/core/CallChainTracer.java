package cn.dolphinmind.glossary.java.analyze.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Core Feature 2: Call Chain Tracer (JavaParser Symbol Solver-based)
 *
 * Uses JavaParser's Symbol Solver with full Maven classpath to accurately
 * resolve method call targets. Only tracks calls between INTERNAL project classes.
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
     * Comprehensive variable type inference.
     *
     * Two-pass approach with method return type tracking:
     * 1. Build variable → type map (params + local vars + fields)
     * 2. Build method → return type map for chained call resolution
     * 3. Use both maps to resolve method call targets
     *
     * Handles: chained calls (a.b().c()), lambda parameters, generics, arrays.
     */
    private String resolveVariableTypeFallback(CompilationUnit cu, String varName, String currentClass) {
        // Pass 1: Build complete variable type table
        Map<String, String> varTypeTable = buildVariableTypeTable(cu, currentClass);

        // Pass 2: Build method return type table
        Map<String, String> methodReturnTypeTable = buildMethodReturnTypeTable(cu);

        // Pass 3: Resolve the variable - could be simple var or chained call
        String type = resolveVariableWithChaining(varName, varTypeTable, methodReturnTypeTable, currentClass);
        if (type != null) return type;

        // Last resort: capitalize first letter
        if (!varName.isEmpty() && Character.isLowerCase(varName.charAt(0))) {
            return Character.toUpperCase(varName.charAt(0)) + varName.substring(1);
        }
        return currentClass;
    }

    /**
     * Resolve a variable that might be a chained call like "a.b().c()".
     * Returns the type of the final expression.
     */
    private String resolveVariableWithChaining(String varName, Map<String, String> varTypeTable,
                                                Map<String, String> methodReturnTypeTable,
                                                String currentClass) {
        // Simple variable lookup
        String type = varTypeTable.get(varName);
        if (type != null) return type;

        // Try to resolve as chained method call: "method1.method2().method3"
        if (varName.contains(".")) {
            String[] parts = varName.split("\\.");
            String currentType = null;

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                // Remove () suffix if present
                String methodName = part.endsWith("()") ? part.substring(0, part.length() - 2) : part;

                if (i == 0) {
                    // First part should be a variable
                    currentType = varTypeTable.get(methodName);
                    if (currentType == null) {
                        // Try as static method call or field
                        currentType = methodReturnTypeTable.get(currentClass + "#" + methodName);
                    }
                } else {
                    // Subsequent parts are method calls on the current type
                    String methodKey = currentType + "#" + methodName;
                    String returnType = methodReturnTypeTable.get(methodKey);
                    if (returnType != null) {
                        currentType = returnType;
                    } else {
                        // Try with simple type name
                        String simpleType = currentType.contains(".") ? currentType.substring(currentType.lastIndexOf('.') + 1) : currentType;
                        methodKey = simpleType + "#" + methodName;
                        returnType = methodReturnTypeTable.get(methodKey);
                        if (returnType != null) {
                            currentType = returnType;
                        } else {
                            return null; // Can't resolve
                        }
                    }
                }

                if (currentType == null) return null;
            }

            return currentType;
        }

        return null;
    }

    /**
     * Build a method → return type mapping for all methods in the CU.
     */
    private Map<String, String> buildMethodReturnTypeTable(CompilationUnit cu) {
        Map<String, String> table = new LinkedHashMap<>();
        String pkg = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString()).orElse("");

        for (com.github.javaparser.ast.body.ClassOrInterfaceDeclaration classDecl : cu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)) {
            String fullClassName = (pkg.isEmpty() ? "" : pkg + ".") + classDecl.getNameAsString();

            for (com.github.javaparser.ast.body.MethodDeclaration method : classDecl.getMethods()) {
                String methodName = method.getNameAsString();
                String returnType = method.getType().toString();
                returnType = resolveTypeFromDeclaration(returnType, cu);

                table.put(fullClassName + "#" + methodName, returnType);

                // Also add with simple class name for cross-CU lookups
                String className = classDecl.getNameAsString();
                table.put(className + "#" + methodName, returnType);
            }
        }

        return table;
    }

    /**
     * Build a complete variable → type mapping for all methods and fields in the CU.
     */
    private Map<String, String> buildVariableTypeTable(CompilationUnit cu, String currentClass) {
        Map<String, String> table = new LinkedHashMap<>();

        // 1. Field declarations (class-level variables)
        for (com.github.javaparser.ast.body.ClassOrInterfaceDeclaration classDecl : cu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)) {
            for (com.github.javaparser.ast.body.FieldDeclaration field : classDecl.getFields()) {
                String fieldType = resolveTypeFromDeclaration(field.getCommonType().toString(), cu);
                for (com.github.javaparser.ast.body.VariableDeclarator var : field.getVariables()) {
                    table.put(var.getNameAsString(), fieldType);
                }
            }
        }

        // 2. Method parameters
        for (com.github.javaparser.ast.body.MethodDeclaration method : cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class)) {
            for (com.github.javaparser.ast.body.Parameter param : method.getParameters()) {
                String paramType = resolveTypeFromDeclaration(param.getType().toString(), cu);
                table.put(param.getNameAsString(), paramType);
            }
        }

        // 3. Local variable declarations (VariableDeclarationExpr)
        for (com.github.javaparser.ast.body.MethodDeclaration method : cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class)) {
            method.findAll(com.github.javaparser.ast.expr.VariableDeclarationExpr.class).forEach(varExpr -> {
                String varType = resolveTypeFromDeclaration(varExpr.getCommonType().toString(), cu);
                for (com.github.javaparser.ast.body.VariableDeclarator var : varExpr.getVariables()) {
                    table.put(var.getNameAsString(), varType);
                }
            });

            // 4. For-each loop variables
            method.findAll(com.github.javaparser.ast.stmt.ForEachStmt.class).forEach(foreach -> {
                com.github.javaparser.ast.expr.VariableDeclarationExpr var = foreach.getVariable();
                String varType = resolveTypeFromDeclaration(var.getCommonType().toString(), cu);
                for (com.github.javaparser.ast.body.VariableDeclarator v : var.getVariables()) {
                    table.put(v.getNameAsString(), varType);
                }
            });

            // 5. Lambda parameters (with explicit types)
            method.findAll(com.github.javaparser.ast.expr.LambdaExpr.class).forEach(lambda -> {
                lambda.getParameters().forEach(param -> {
                    if (!param.getType().isUnknownType()) {
                        String paramType = resolveTypeFromDeclaration(param.getType().toString(), cu);
                        table.put(param.getNameAsString(), paramType);
                    }
                });
            });
        }

        return table;
    }

    private String resolveTypeFromDeclaration(String typeStr, CompilationUnit cu) {
        // Handle generic types: List<User> → List
        if (typeStr.contains("<")) {
            typeStr = typeStr.substring(0, typeStr.indexOf('<'));
        }
        // Handle arrays: User[] → User
        if (typeStr.endsWith("[]")) {
            typeStr = typeStr.substring(0, typeStr.length() - 2);
        }
        // Handle wildcard types: ? extends User → User
        if (typeStr.startsWith("? extends ")) {
            typeStr = typeStr.substring(10);
        } else if (typeStr.startsWith("? super ")) {
            typeStr = typeStr.substring(8);
        }
        // If simple name, try to resolve from imports
        if (!typeStr.contains(".")) {
            // Check known Java types
            String[] knownTypes = {
                "String", "Integer", "Long", "Double", "Float", "Boolean", "Byte", "Short", "Character",
                "List", "ArrayList", "LinkedList", "Set", "HashSet", "TreeSet", "LinkedHashSet",
                "Map", "HashMap", "TreeMap", "LinkedHashMap", "ConcurrentHashMap",
                "Optional", "Stream", "Collection", "Iterable", "Iterator",
                "Runnable", "Callable", "Future", "CompletableFuture",
                "Exception", "RuntimeException", "Throwable", "Error",
                "Object", "Class", "Enum"
            };
            for (String known : knownTypes) {
                if (typeStr.equals(known)) return "java.lang." + known;
            }

            // Check imports
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
