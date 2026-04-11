package cn.dolphinmind.glossary.java.analyze.extractor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.Type;

import java.util.*;

/**
 * Extracts method call information, method body structure, and call graph data from Java AST.
 *
 * Split from SourceUniversePro to separate call-graph/body analysis from asset extraction.
 */
public class MethodCallAnalyzer {

    /**
     * Extract method calls and build dependency edges.
     */
    public void extractMethodCalls(CallableDeclaration<?> method, String classAddr, String fullAddr,
                                    List<Map<String, String>> globalDeps) {
        List<String> calledMethods = new ArrayList<>();
        List<String> calledClasses = new ArrayList<>();

        // Extract method call expressions
        method.findAll(MethodCallExpr.class).forEach(call -> {
            String calledMethod = call.getNameAsString();
            calledMethods.add(calledMethod);

            // Try to resolve the scope (target class)
            call.getScope().ifPresent(scope -> {
                String scopeStr = scope.toString();
                if (!scopeStr.isEmpty()) {
                    calledClasses.add(scopeStr);
                }
            });
        });

        // Extract throw statements for dependency analysis
        method.findAll(ThrowStmt.class).forEach(throwStmt -> {
            String exception = throwStmt.getExpression().toString();
            if (exception.contains(".")) {
                Map<String, String> dep = new LinkedHashMap<>();
                dep.put("source", fullAddr);
                dep.put("target", extractSimpleType(exception));
                dep.put("type", "throws");
                globalDeps.add(dep);
            }
        });
    }

    /**
     * Extract the body code of a callable declaration.
     */
    public String extractCallableBody(CallableDeclaration<?> d) {
        BlockStmt body = getCallableBody(d);
        if (body == null) return "";
        return body.toString();
    }

    /**
     * Calculate the line count of a callable declaration.
     */
    public int calculateLineCount(CallableDeclaration<?> d) {
        BlockStmt body = getCallableBody(d);
        if (body == null) return 0;
        int start = body.getBegin().map(p -> p.line).orElse(0);
        int end = body.getEnd().map(p -> p.line).orElse(0);
        return Math.max(0, end - start + 1);
    }

    /**
     * Get the body block statement of a callable declaration.
     */
    public BlockStmt getCallableBody(CallableDeclaration<?> d) {
        if (d instanceof MethodDeclaration) {
            Optional<BlockStmt> body = ((MethodDeclaration) d).getBody();
            return body.isPresent() ? body.get() : null;
        } else if (d instanceof ConstructorDeclaration) {
            return ((ConstructorDeclaration) d).getBody();
        }
        return null;
    }

    /**
     * Extract key statements from a callable declaration.
     */
    public List<Map<String, String>> extractKeyStatements(CallableDeclaration<?> d) {
        List<Map<String, String>> statements = new ArrayList<>();
        BlockStmt body = getCallableBody(d);
        if (body == null) return statements;

        // Extract throw statements
        body.findAll(ThrowStmt.class).forEach(throwStmt -> {
            Map<String, String> stmt = new LinkedHashMap<>();
            stmt.put("type", "throw");
            stmt.put("expression", throwStmt.getExpression().toString());
            stmt.put("line", throwStmt.getBegin().map(p -> String.valueOf(p.line)).orElse(""));
            statements.add(stmt);
        });

        // Extract if statements
        body.findAll(com.github.javaparser.ast.stmt.IfStmt.class).forEach(ifStmt -> {
            Map<String, String> stmt = new LinkedHashMap<>();
            stmt.put("type", "if");
            stmt.put("condition", ifStmt.getCondition().toString());
            stmt.put("line", ifStmt.getBegin().map(p -> String.valueOf(p.line)).orElse(""));
            statements.add(stmt);
        });

        // Extract return statements
        body.findAll(com.github.javaparser.ast.stmt.ReturnStmt.class).forEach(retStmt -> {
            Map<String, String> stmt = new LinkedHashMap<>();
            stmt.put("type", "return");
            stmt.put("expression", retStmt.getExpression().map(Object::toString).orElse("void"));
            stmt.put("line", retStmt.getBegin().map(p -> String.valueOf(p.line)).orElse(""));
            statements.add(stmt);
        });

        return statements;
    }

    /**
     * Summarize method body by extracting key patterns.
     */
    public String summarizeMethodBody(CallableDeclaration<?> d) {
        String body = extractCallableBody(d);
        if (body.isEmpty()) return "empty";

        List<String> patterns = new ArrayList<>();

        if (body.contains("if (") || body.contains("if(")) patterns.add("conditional");
        if (body.contains("for (") || body.contains("for(")) patterns.add("loop");
        if (body.contains("while (") || body.contains("while(")) patterns.add("loop");
        if (body.contains("try {") || body.contains("try{")) patterns.add("try-catch");
        if (body.contains("catch (") || body.contains("catch(")) patterns.add("exception");
        if (body.contains("throw ")) patterns.add("throws");
        if (body.contains("return ")) patterns.add("returns");
        if (body.contains(".stream()")) patterns.add("stream");
        if (body.contains("lambda") || body.contains("->")) patterns.add("lambda");

        return patterns.isEmpty() ? "simple" : String.join(",", patterns);
    }

    /**
     * Extract simple type name from a fully qualified type reference.
     */
    public String extractSimpleType(String fullType) {
        if (fullType == null) return "";
        // Handle generic types
        int genericIdx = fullType.indexOf('<');
        if (genericIdx > 0) fullType = fullType.substring(0, genericIdx);
        // Get simple name
        int dotIdx = fullType.lastIndexOf('.');
        return dotIdx >= 0 ? fullType.substring(dotIdx + 1) : fullType.trim();
    }

    /**
     * Extract semantic path from a type reference.
     */
    public String getSemanticPath(Type type) {
        if (type == null) return "unknown";
        String typeStr = type.asString();
        if (typeStr.startsWith("java.") || typeStr.startsWith("javax.") ||
            typeStr.startsWith("com.sun.") || typeStr.startsWith("sun.")) {
            return "jdk";
        }
        if (typeStr.contains(".springframework.")) return "spring";
        if (typeStr.contains(".mybatis.")) return "mybatis";
        if (typeStr.contains(".apache.")) return "apache";
        return "project";
    }

    /**
     * Check if a method has @Override annotation.
     */
    public boolean checkIsOverride(MethodDeclaration m) {
        return m.getAnnotations().stream()
                .anyMatch(a -> a.getNameAsString().equals("Override"));
    }

    /**
     * Resolve hierarchy information (extends/implements) for a class.
     */
    public Map<String, List<String>> resolveHierarchySemantic(ClassOrInterfaceDeclaration cid) {
        Map<String, List<String>> hierarchy = new LinkedHashMap<>();

        List<String> extendsList = new ArrayList<>();
        cid.getExtendedTypes().forEach(t -> extendsList.add(t.getNameAsString()));
        hierarchy.put("extends", extendsList);

        List<String> implementsList = new ArrayList<>();
        cid.getImplementedTypes().forEach(t -> implementsList.add(t.getNameAsString()));
        hierarchy.put("implements", implementsList);

        return hierarchy;
    }

    /**
     * Resolve modifier list to string list.
     */
    public List<String> resolveMods(com.github.javaparser.ast.NodeList<com.github.javaparser.ast.Modifier> modifiers) {
        List<String> mods = new ArrayList<>();
        modifiers.forEach(m -> mods.add(m.getKeyword().asString()));
        return mods;
    }

    /**
     * Resolve type parameters for a type declaration.
     */
    public List<String> resolveTypeParameters(TypeDeclaration<?> t) {
        List<String> params = new ArrayList<>();
        if (t instanceof ClassOrInterfaceDeclaration) {
            ((ClassOrInterfaceDeclaration) t).getTypeParameters().forEach(tp -> params.add(tp.getNameAsString()));
        }
        return params;
    }

    /**
     * Extract module name from package name.
     */
    public String extractModuleName(String pkg) {
        if (pkg == null || pkg.isEmpty()) return "default";
        String[] parts = pkg.split("\\.");
        if (parts.length >= 3) {
            // com.example.module → module
            return parts[parts.length - 1];
        }
        return parts.length > 0 ? parts[parts.length - 1] : "default";
    }
}
