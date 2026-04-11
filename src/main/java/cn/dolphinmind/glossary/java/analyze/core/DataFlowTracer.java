package cn.dolphinmind.glossary.java.analyze.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Core Feature 5: Data Flow Tracer (JavaParser Symbol Solver-based)
 *
 * Traces how method parameters flow through method calls.
 * Uses Symbol Solver for accurate type resolution.
 * Tracks multi-hop flows: param → methodA → methodB → methodC
 */
public class DataFlowTracer {

    public static class DataFlow {
        private final String variable;
        private final String sourceMethod;
        private final List<String> path;
        private final String sinkMethod;
        private final String description;

        public DataFlow(String variable, String sourceMethod, List<String> path,
                        String sinkMethod, String description) {
            this.variable = variable;
            this.sourceMethod = sourceMethod;
            this.path = new ArrayList<>(path);
            this.sinkMethod = sinkMethod;
            this.description = description;
        }

        public String getVariable() { return variable; }
        public String getSourceMethod() { return sourceMethod; }
        public List<String> getPath() { return Collections.unmodifiableList(path); }
        public String getSinkMethod() { return sinkMethod; }
        public String getDescription() { return description; }

        public String toArrowString() {
            return variable + ": " + sourceMethod + " → " + String.join(" → ", path) +
                    (sinkMethod.isEmpty() ? "" : " → [" + sinkMethod + "]");
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("variable", variable);
            map.put("source", sourceMethod);
            map.put("path", path);
            map.put("sink", sinkMethod);
            map.put("description", description);
            map.put("flow", toArrowString());
            return map;
        }
    }

    /**
     * Index all methods in the project for data flow analysis.
     */
    public Map<String, MethodDeclaration> indexMethods(Path projectRoot) throws IOException {
        Map<String, MethodDeclaration> methodIndex = new LinkedHashMap<>();

        Files.walk(projectRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("src"))
                .filter(p -> !p.toString().contains("test") && !p.toString().contains("target"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String pkg = cu.getPackageDeclaration()
                                .map(pd -> pd.getNameAsString()).orElse("");

                        for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                            String fullClass = (pkg.isEmpty() ? "" : pkg + ".") + classDecl.getNameAsString();
                            for (MethodDeclaration method : classDecl.getMethods()) {
                                String key = fullClass + "#" + method.getNameAsString();
                                methodIndex.put(key, method);
                            }
                        }
                    } catch (Exception e) {}
                });

        return methodIndex;
    }

    /**
     * Trace data flow for a specific method.
     * Multi-hop tracking: follows parameters through internal method calls.
     */
    public List<DataFlow> traceMethod(Map<String, MethodDeclaration> methodIndex,
                                       String className, String methodName) {
        List<DataFlow> flows = new ArrayList<>();
        String methodKey = className + "#" + methodName;
        MethodDeclaration method = methodIndex.get(methodKey);
        if (method == null) return flows;

        for (Parameter param : method.getParameters()) {
            String paramName = param.getNameAsString();
            String paramType = param.getTypeAsString();
            traceParamMultiHop(method, paramName, paramType, className, methodIndex,
                    new ArrayList<>(), new HashSet<>(), flows, 0, 6);
        }

        return flows;
    }

    /**
     * Multi-hop parameter tracing with depth limit.
     */
    private void traceParamMultiHop(MethodDeclaration method, String paramName, String paramType,
                                     String currentClass, Map<String, MethodDeclaration> methodIndex,
                                     List<String> path, Set<String> visited, List<DataFlow> flows,
                                     int depth, int maxDepth) {
        if (depth > maxDepth) return;

        String methodKey = currentClass + "#" + method.getNameAsString();
        if (visited.contains(methodKey)) {
            // Cycle detected
            List<String> chainWithCycle = new ArrayList<>(path);
            chainWithCycle.add(methodKey + "(循环)");
            flows.add(new DataFlow(paramName, path.get(0), chainWithCycle,
                    methodKey, "Cycle detected at depth " + depth));
            return;
        }

        visited.add(methodKey);
        path.add(methodKey);

        // Track where param is used
        method.findAll(MethodCallExpr.class).forEach(call -> {
            call.getArguments().forEach(arg -> {
                if (arg instanceof NameExpr && ((NameExpr) arg).getNameAsString().equals(paramName)) {
                    String targetMethod = call.getNameAsString();
                    String targetClass = resolveCalleeClass(call, currentClass);
                    String calleeKey = targetClass + "#" + targetMethod;

                    flows.add(new DataFlow(paramName, path.get(0), new ArrayList<>(path),
                            calleeKey, "Parameter passed to " + calleeKey + " (depth " + (depth + 1) + ")"));

                    // Continue tracing into callee if it's indexed
                    MethodDeclaration callee = methodIndex.get(calleeKey);
                    if (callee != null) {
                        // Find which parameter receives our value
                        int argIndex = getArgumentIndex(call, arg);
                        if (argIndex >= 0 && argIndex < callee.getParameters().size()) {
                            String calleeParamName = callee.getParameter(argIndex).getNameAsString();
                            traceParamMultiHop(callee, calleeParamName, paramType, targetClass, methodIndex,
                                    new ArrayList<>(path), new HashSet<>(visited), flows, depth + 1, maxDepth);
                        }
                    }
                }
            });
        });

        // Check if param is returned
        method.findAll(ReturnStmt.class).forEach(ret -> {
            ret.getExpression().ifPresent(expr -> {
                if (expr instanceof NameExpr && ((NameExpr) expr).getNameAsString().equals(paramName)) {
                    flows.add(new DataFlow(paramName, path.get(0), new ArrayList<>(path),
                            methodKey, "Parameter returned (depth " + depth + ")"));
                }
            });
        });

        // Check if param is assigned to a field
        method.findAll(AssignExpr.class).forEach(assign -> {
            if (assign.getTarget() instanceof FieldAccessExpr) {
                FieldAccessExpr fa = (FieldAccessExpr) assign.getTarget();
                if (fa.getScope() instanceof NameExpr &&
                    ((NameExpr) fa.getScope()).getNameAsString().equals(paramName)) {
                    flows.add(new DataFlow(paramName, path.get(0), new ArrayList<>(path),
                            fa.getNameAsString(), "Parameter assigned to field " + fa.getNameAsString()));
                }
            }
        });
    }

    /**
     * Resolve the class of a method call target using Symbol Solver.
     */
    private String resolveCalleeClass(MethodCallExpr call, String currentClass) {
        // Try Symbol Solver first
        try {
            ResolvedMethodDeclaration resolved = call.resolve();
            return resolved.declaringType().getQualifiedName();
        } catch (Exception e) {
            // Fallback
            return call.getScope()
                    .map(s -> {
                        String scopeStr = s.toString();
                        if (scopeStr.equals("this")) return currentClass;
                        // Capitalize first letter as fallback
                        if (!scopeStr.isEmpty()) {
                            return Character.toUpperCase(scopeStr.charAt(0)) + scopeStr.substring(1);
                        }
                        return currentClass;
                    })
                    .orElse(currentClass);
        }
    }

    /**
     * Get the index of an argument in a method call.
     */
    private int getArgumentIndex(MethodCallExpr call, Expression arg) {
        int index = 0;
        for (Expression c : call.getArguments()) {
            if (c == arg) return index;
            index++;
        }
        return -1;
    }

    /**
     * Fallback variable type resolution when Symbol Solver fails.
     */
    private String resolveVariableTypeFallback(CompilationUnit cu, String varName, String currentClass) {
        // Check imports
        for (com.github.javaparser.ast.ImportDeclaration imp : cu.getImports()) {
            String impName = imp.getNameAsString();
            if (impName.toLowerCase().contains(varName.toLowerCase())) {
                return impName;
            }
        }
        // Capitalize first letter as last resort
        if (!varName.isEmpty()) {
            return Character.toUpperCase(varName.charAt(0)) + varName.substring(1);
        }
        return currentClass;
    }

    public void printFlows(List<DataFlow> flows) {
        if (flows.isEmpty()) {
            System.out.println("  (未找到数据流)");
            return;
        }
        System.out.println("\n=== 数据流追踪 (" + flows.size() + " 条, 最多6跳) ===");
        int shown = 0;
        for (DataFlow flow : flows) {
            if (shown >= 20) {
                System.out.println("  ... 还有 " + (flows.size() - 20) + " 条");
                break;
            }
            System.out.println("  " + flow.toArrowString());
            shown++;
        }
    }

    public Map<String, Object> export(List<DataFlow> flows) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total_flows", flows.size());
        map.put("max_depth", 6);
        map.put("flows", flows.stream().map(DataFlow::toMap)
                .collect(java.util.stream.Collectors.toList()));
        return map;
    }

    public List<Map<String, Object>> exportList(List<DataFlow> flows) {
        return flows.stream().map(DataFlow::toMap)
                .collect(java.util.stream.Collectors.toList());
    }
}
