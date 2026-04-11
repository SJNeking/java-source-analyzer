package cn.dolphinmind.glossary.java.analyze.extractor;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper utilities for type asset extraction.
 *
 * Moved from SourceUniversePro to decouple from the monolithic class.
 * Pure functions — no mutable state, fully testable.
 */
public class TypeAssetHelper {

    /**
     * Get the kind of a type declaration (class, interface, enum, annotation).
     */
    public static String getKind(TypeDeclaration<?> t) {
        if (t instanceof EnumDeclaration) return "ENUM";
        if (t instanceof AnnotationDeclaration) return "ANNOTATION";
        if (t instanceof ClassOrInterfaceDeclaration) {
            return ((ClassOrInterfaceDeclaration) t).isInterface() ? "INTERFACE" : "CLASS";
        }
        return "TYPE";
    }

    /**
     * Resolve parameter list to inventory maps.
     */
    public static List<Map<String, String>> resolveParametersInventory(NodeList<Parameter> parameters) {
        return parameters.stream().map(p -> {
            Map<String, String> pMap = new LinkedHashMap<>();
            pMap.put("name", p.getNameAsString());
            pMap.put("type_path", getSemanticPath(p.getType()));
            return pMap;
        }).collect(Collectors.toList());
    }

    /**
     * Semantic path resolution: try to resolve fully-qualified name, fallback to AST.
     */
    public static String getSemanticPath(Type type) {
        try {
            return type.resolve().describe();
        } catch (Exception e) {
            String typeName = type.asString();
            if (!typeName.contains(".") && typeName.matches("[A-Z]\\w*")) {
                return typeName;
            }
            return typeName;
        }
    }

    /**
     * Check if a method is an override.
     */
    public static boolean checkIsOverride(MethodDeclaration m) {
        try {
            return m.resolve().getQualifiedSignature().contains("Override")
                    || m.getAnnotationByClass(Override.class).isPresent();
        } catch (Exception e) {
            return m.getAnnotationByClass(Override.class).isPresent();
        }
    }

    /**
     * Extract hierarchy info (extends/implements).
     */
    public static Map<String, List<String>> resolveHierarchySemantic(ClassOrInterfaceDeclaration cid) {
        Map<String, List<String>> h = new LinkedHashMap<>();
        h.put("extends", cid.getExtendedTypes().stream()
                .map(TypeAssetHelper::getSemanticPath).collect(Collectors.toList()));
        h.put("implements", cid.getImplementedTypes().stream()
                .map(TypeAssetHelper::getSemanticPath).collect(Collectors.toList()));
        return h;
    }

    /**
     * Resolve modifiers to string list.
     */
    public static List<String> resolveMods(com.github.javaparser.ast.NodeList<com.github.javaparser.ast.Modifier> modifiers) {
        return modifiers.stream().map(m -> m.getKeyword().asString()).collect(Collectors.toList());
    }

    /**
     * Resolve generic type parameters.
     */
    public static List<String> resolveTypeParameters(TypeDeclaration<?> t) {
        if (t instanceof ClassOrInterfaceDeclaration) {
            return ((ClassOrInterfaceDeclaration) t).getTypeParameters().stream()
                    .map(tp -> tp.asString()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Extract method-level semantic tags based on naming patterns.
     */
    public static Set<String> extractMethodTags(String methodName, String returnType) {
        Set<String> tags = new HashSet<>();
        if (methodName == null) return tags;
        String lowerName = methodName.toLowerCase();

        if (lowerName.matches(".*(get|find|select|query|list|search|fetch).*")) tags.add("Query");
        else if (lowerName.matches(".*(add|create|insert|save|register).*")) tags.add("Create");
        else if (lowerName.matches(".*(update|modify|edit|change|set).*")) tags.add("Update");
        else if (lowerName.matches(".*(delete|remove|drop|clear).*")) tags.add("Delete");

        if (lowerName.matches(".*(sync|lock|unlock|mutex).*")) tags.add("Locking");
        else if (lowerName.matches(".*(async|future|callback|executor).*")) tags.add("Async");
        else if (lowerName.matches(".*(atomic|cas).*")) tags.add("Atomic");

        if (lowerName.matches(".*(init|start|stop|destroy|close|open).*")) tags.add("Lifecycle");
        else if (lowerName.matches(".*(config|property|setting).*")) tags.add("Config");
        else if (lowerName.matches(".*(util|helper|convert|parse|format).*")) tags.add("Utility");

        if (lowerName.matches(".*(validate|check|verify|assert).*")) tags.add("Validation");
        else if (lowerName.matches(".*(error|catch|rollback|retry).*")) tags.add("ErrorHandling");

        return tags;
    }

    /**
     * Extract annotations from a node.
     */
    public static List<Map<String, Object>> extractAnnos(NodeWithAnnotations<?> n, String target) {
        List<Map<String, Object>> annotations = new ArrayList<>();
        n.getAnnotations().forEach(annotation -> {
            Map<String, Object> ann = new LinkedHashMap<>();
            ann.put("name", annotation.getNameAsString());
            ann.put("target", target);
            List<Map<String, String>> params = new ArrayList<>();
            annotation.getChildNodes().forEach(node -> {
                if (node instanceof com.github.javaparser.ast.expr.MemberValuePair) {
                    com.github.javaparser.ast.expr.MemberValuePair mvp =
                            (com.github.javaparser.ast.expr.MemberValuePair) node;
                    Map<String, String> param = new LinkedHashMap<>();
                    param.put("key", mvp.getNameAsString());
                    param.put("value", mvp.getValue().toString());
                    params.add(param);
                }
            });
            if (!params.isEmpty()) ann.put("parameters", params);
            annotations.add(ann);
        });
        return annotations;
    }

    /**
     * Extract source code of a node from file lines.
     */
    public static String extractNodeSource(List<String> fileLines, Node node, boolean includeBody) {
        if (!node.getBegin().isPresent() || !node.getEnd().isPresent()) return "";
        int start = node.getBegin().get().line - 1;
        int end = Math.min(node.getEnd().get().line, fileLines.size());
        if (start < 0 || end <= start) return "";
        List<String> lines = fileLines.subList(start, end);
        if (!includeBody && node instanceof MethodDeclaration) {
            // Return just the signature
            return ((MethodDeclaration) node).getDeclarationAsString(false, false, false);
        }
        return String.join("\n", lines);
    }

    /**
     * Calculate lines of code for a type.
     */
    public static int calculateClassLOC(TypeDeclaration<?> type, List<String> fileLines) {
        if (!type.getBegin().isPresent() || !type.getEnd().isPresent()) return 0;
        int start = type.getBegin().get().line - 1;
        int end = Math.min(type.getEnd().get().line, fileLines.size());
        int loc = 0;
        for (int i = start; i < end && i < fileLines.size(); i++) {
            String line = fileLines.get(i).trim();
            if (!line.isEmpty() && !line.startsWith("//") && !line.startsWith("/*") && !line.startsWith("*")) {
                loc++;
            }
        }
        return loc;
    }

    /**
     * Calculate approximate cyclomatic complexity for a type.
     */
    public static double calculateClassComplexity(TypeDeclaration<?> type, List<String> fileLines) {
        double totalComplexity = 0;
        int methodCount = 0;
        for (MethodDeclaration method : type.getMethods()) {
            if (!method.getBody().isPresent()) continue;
            String body = method.getBody().get().toString();
            double complexity = 1;
            complexity += countPattern(body, "\\bif\\s*\\(");
            complexity += countPattern(body, "\\belse\\s+if\\s*\\(");
            complexity += countPattern(body, "\\bfor\\s*\\(");
            complexity += countPattern(body, "\\bwhile\\s*\\(");
            complexity += countPattern(body, "\\bcase\\s+");
            complexity += countPattern(body, "\\bcatch\\s*\\(");
            complexity += countPattern(body, "&&");
            complexity += countPattern(body, "\\|\\|");
            complexity += countPattern(body, "\\?[^?]");
            totalComplexity += complexity;
            methodCount++;
        }
        return methodCount > 0 ? Math.round(totalComplexity * 100.0) / 100.0 : 1.0;
    }

    private static int countPattern(String text, String regex) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    /**
     * Calculate inheritance depth.
     */
    public static int calculateInheritanceDepth(TypeDeclaration<?> type) {
        if (!(type instanceof ClassOrInterfaceDeclaration)) return 0;
        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) type;
        int depth = 0;
        if (!classDecl.getExtendedTypes().isEmpty()) depth++;
        return depth;
    }

    /**
     * Extract method call graph summary from a type.
     */
    public static Map<String, Object> extractCallGraphSummary(TypeDeclaration<?> type) {
        Map<String, Object> summary = new LinkedHashMap<>();
        List<String> calledMethods = new ArrayList<>();
        java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger(0);
        for (MethodDeclaration m : type.getMethods()) {
            if (!m.getBody().isPresent()) continue;
            m.getBody().get().findAll(MethodCallExpr.class).forEach(call -> {
                calledMethods.add(call.getNameAsString());
                callCount.incrementAndGet();
            });
        }
        summary.put("total_calls", callCount.get());
        summary.put("unique_methods_called", calledMethods.stream().distinct().collect(Collectors.toList()));
        return summary;
    }

    /**
     * Extract module name from package.
     */
    public static String extractModuleName(String pkg) {
        if (pkg.startsWith("java.") || pkg.startsWith("javax.") || pkg.startsWith("jdk.") || pkg.startsWith("sun.")) {
            int dotIndex = pkg.indexOf('.');
            return dotIndex > 0 ? pkg.substring(0, dotIndex) : pkg;
        }
        if (pkg.startsWith("com.sun.")) return "com-sun";
        String[] prefixes = {
                "org.apache.catalina.", "org.springframework.cloud.", "org.springframework.boot.",
                "io.netty.", "org.apache.dubbo.", "org.apache.rocketmq.",
                "org.apache.ibatis.", "org.redisson.", "com.zaxxer.hikari.", "org.springframework."
        };
        for (String prefix : prefixes) {
            if (pkg.startsWith(prefix)) {
                String sub = pkg.substring(prefix.length());
                int idx = sub.indexOf('.');
                String module = idx != -1 ? sub.substring(0, idx) : sub;
                if (prefix.contains("springframework")) return "spring-" + module;
                return module;
            }
        }
        return pkg;
    }
}
