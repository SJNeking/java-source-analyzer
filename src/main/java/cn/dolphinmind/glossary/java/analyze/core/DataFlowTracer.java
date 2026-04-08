package cn.dolphinmind.glossary.java.analyze.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Core Feature 5: Data Flow Tracer
 *
 * Traces key variables from input to output through method calls.
 * Answers:
 * - "这个参数传到了哪里？"
 * - "这个返回值是从哪里来的？"
 * - "关键数据 (user, order, request) 经过了哪些方法？"
 *
 * This is a simplified data flow analysis that tracks:
 * 1. Parameter usage within a method
 * 2. Parameter passing through method calls
 * 3. Return value flow
 */
public class DataFlowTracer {

    /**
     * A single data flow path.
     */
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
     * Trace data flow for a specific method.
     * Tracks how parameters flow through method calls.
     */
    public List<DataFlow> traceMethod(String className, String methodName, String filePath) throws IOException {
        List<DataFlow> flows = new ArrayList<>();

        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            CompilationUnit cu = StaticJavaParser.parse(content);

            // Find the method
            Optional<MethodDeclaration> methodOpt = cu.findAll(MethodDeclaration.class).stream()
                    .filter(m -> m.getNameAsString().equals(methodName))
                    .findFirst();

            if (!methodOpt.isPresent()) return flows;

            MethodDeclaration method = methodOpt.get();

            // Track each parameter
            method.getParameters().forEach(param -> {
                String paramName = param.getNameAsString();
                String paramType = param.getTypeAsString();
                List<String> path = new ArrayList<>();

                traceVariable(method, paramName, paramType, path, new HashSet<>(), flows, className);
            });
        } catch (Exception e) {
            // ignore parse errors
        }

        return flows;
    }

    /**
     * Trace a variable through method body.
     */
    private void traceVariable(MethodDeclaration method, String varName, String varType,
                                List<String> path, Set<String> visited, List<DataFlow> flows,
                                String currentClass) {
        String methodKey = currentClass + "#" + method.getNameAsString();
        if (visited.contains(methodKey)) {
            // Cycle detected
            path.add(methodKey + "(cycle)");
            flows.add(new DataFlow(varName, path.get(0), new ArrayList<>(path),
                    methodKey, "Cycle detected"));
            return;
        }

        visited.add(methodKey);
        path.add(methodKey);

        // Check if variable is used in method calls
        method.findAll(MethodCallExpr.class).forEach(call -> {
            // Check if variable is passed as argument
            call.getArguments().forEach(arg -> {
                if (arg instanceof NameExpr && ((NameExpr) arg).getNameAsString().equals(varName)) {
                    String targetMethod = call.getNameAsString();
                    String targetClass = call.getScope()
                            .map(s -> capitalize(s.toString().replaceAll("this\\.", "")))
                            .orElse(currentClass);

                    traceMethodCall(targetClass, targetMethod, varName, varType,
                            new ArrayList<>(path), new HashSet<>(visited), flows);
                }
            });
        });

        // Check if variable is returned
        method.findAll(ReturnStmt.class).forEach(ret -> {
            ret.getExpression().ifPresent(expr -> {
                if (expr instanceof NameExpr && ((NameExpr) expr).getNameAsString().equals(varName)) {
                    flows.add(new DataFlow(varName, path.get(0), new ArrayList<>(path),
                            methodKey, "Variable returned"));
                }
            });
        });

        // Check if variable is passed to field assignment
        method.findAll(AssignExpr.class).forEach(assign -> {
            Expression target = assign.getTarget();
            if (target instanceof FieldAccessExpr) {
                FieldAccessExpr fieldAccess = (FieldAccessExpr) target;
                Expression scope = fieldAccess.getScope();
                if (scope instanceof NameExpr && ((NameExpr) scope).getNameAsString().equals(varName)) {
                    flows.add(new DataFlow(varName, path.get(0), new ArrayList<>(path),
                            fieldAccess.getNameAsString(), "Variable assigned to field"));
                }
            }
        });
    }

    private void traceMethodCall(String targetClass, String targetMethod, String varName,
                                   String varType, List<String> path, Set<String> visited,
                                   List<DataFlow> flows) {
        // In a real implementation, this would load the target method and continue tracing
        // For now, we just record the call
        path.add(targetClass + "#" + targetMethod);

        flows.add(new DataFlow(varName, path.get(0), new ArrayList<>(path),
                targetClass + "#" + targetMethod, "Variable passed as argument"));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Print data flows in a readable format.
     */
    public void printFlows(List<DataFlow> flows) {
        if (flows.isEmpty()) {
            System.out.println("  (no data flow found)");
            return;
        }

        System.out.println("\n=== 数据流追踪 ===");
        int shown = 0;
        for (DataFlow flow : flows) {
            if (shown >= 20) {
                System.out.println("  ... 还有 " + (flows.size() - 20) + " 条数据流");
                break;
            }
            System.out.println("  " + flow.toArrowString());
            shown++;
        }
    }

    /**
     * Export data flows as JSON-compatible map.
     */
    public Map<String, Object> export(List<DataFlow> flows) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total_flows", flows.size());
        map.put("flows", flows.stream().map(DataFlow::toMap).collect(java.util.stream.Collectors.toList()));
        return map;
    }
}
