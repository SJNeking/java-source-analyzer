package cn.dolphinmind.glossary.java.analyze.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ReturnStmt;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Core Feature 5: Data Flow Tracer (JavaParser-based)
 *
 * Traces how method parameters flow through internal method calls.
 * Answers: "这个参数传到了哪些方法？"
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
     * Build a mapping of method signatures to their bodies for data flow analysis.
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
     * Tracks how each parameter is used: passed to other methods, returned, assigned to fields.
     */
    public List<DataFlow> traceMethod(Map<String, MethodDeclaration> methodIndex,
                                       String className, String methodName) {
        List<DataFlow> flows = new ArrayList<>();
        String methodKey = className + "#" + methodName;
        MethodDeclaration method = methodIndex.get(methodKey);
        if (method == null) return flows;

        for (com.github.javaparser.ast.body.Parameter param : method.getParameters()) {
            String paramName = param.getNameAsString();
            String paramType = param.getTypeAsString();
            traceParam(method, paramName, paramType, className, methodIndex,
                    new ArrayList<>(), new HashSet<>(), flows);
        }

        return flows;
    }

    private void traceParam(MethodDeclaration method, String paramName, String paramType,
                             String currentClass, Map<String, MethodDeclaration> methodIndex,
                             List<String> path, Set<String> visited, List<DataFlow> flows) {
        String methodKey = currentClass + "#" + method.getNameAsString();
        if (visited.contains(methodKey) || path.size() > 8) return;
        visited.add(methodKey);
        path.add(methodKey);

        // Track where param is used
        method.findAll(MethodCallExpr.class).forEach(call -> {
            // Check if param is passed as argument
            call.getArguments().forEach(arg -> {
                if (arg instanceof NameExpr && ((NameExpr) arg).getNameAsString().equals(paramName)) {
                    String targetMethod = call.getNameAsString();
                    String targetClass = call.getScope()
                            .map(s -> capitalize(s.toString()))
                            .orElse(currentClass);
                    String calleeKey = targetClass + "#" + targetMethod;

                    flows.add(new DataFlow(paramName, path.get(0), new ArrayList<>(path),
                            calleeKey, "Parameter passed as argument to " + calleeKey));

                    // Continue tracing into callee if it's indexed
                    MethodDeclaration callee = methodIndex.get(calleeKey);
                    if (callee != null) {
                        traceParam(callee, paramName, paramType, targetClass, methodIndex,
                                new ArrayList<>(path), new HashSet<>(visited), flows);
                    }
                }
            });
        });

        // Check if param is returned
        method.findAll(ReturnStmt.class).forEach(ret -> {
            ret.getExpression().ifPresent(expr -> {
                if (expr instanceof NameExpr && ((NameExpr) expr).getNameAsString().equals(paramName)) {
                    flows.add(new DataFlow(paramName, path.get(0), new ArrayList<>(path),
                            methodKey, "Parameter returned"));
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.equals("this")) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public void printFlows(List<DataFlow> flows) {
        if (flows.isEmpty()) {
            System.out.println("  (未找到数据流)");
            return;
        }
        System.out.println("\n=== 数据流追踪 (" + flows.size() + " 条) ===");
        int shown = 0;
        for (DataFlow flow : flows) {
            if (shown >= 15) {
                System.out.println("  ... 还有 " + (flows.size() - 15) + " 条");
                break;
            }
            System.out.println("  " + flow.toArrowString());
            shown++;
        }
    }

    public Map<String, Object> export(List<DataFlow> flows) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total_flows", flows.size());
        map.put("flows", flows.stream().map(DataFlow::toMap)
                .collect(java.util.stream.Collectors.toList()));
        return map;
    }
}
