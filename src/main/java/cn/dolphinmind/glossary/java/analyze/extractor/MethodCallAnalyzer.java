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
     * Extract method calls using JavaSymbolSolver for precise resolution.
     * Only tracks public/protected cross-class calls to reduce noise.
     */
    public void extractMethodCalls(CallableDeclaration<?> method, String classAddr, String fullAddr,
                                    List<Map<String, String>> globalDeps, java.util.Set<String> seenDeps) {
        boolean isPublic = method.getModifiers().stream()
                .anyMatch(m -> m.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.PUBLIC);
        boolean isProtected = method.getModifiers().stream()
                .anyMatch(m -> m.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.PROTECTED);
        if (!isPublic && !isProtected) return;

        method.findAll(MethodCallExpr.class).forEach(call -> {
            try {
                com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration resolved = call.resolve();
                String targetClass = resolved.declaringType().getQualifiedName();
                String targetMethod = resolved.getName();

                if (targetClass.startsWith("java.") || targetClass.startsWith("javax.") ||
                    targetClass.startsWith("jdk.") || targetClass.startsWith("org.slf4j") ||
                    targetClass.startsWith("org.apache.log4j")) {
                    return;
                }

                String sourceClass = classAddr.split("#")[0];
                if (targetClass.equals(sourceClass)) return;

                String targetAddr = targetClass + "#" + targetMethod;
                String depKey = fullAddr + "→" + targetAddr + ":CALLS";
                if (!seenDeps.contains(depKey)) {
                    seenDeps.add(depKey);
                    Map<String, String> callDep = new LinkedHashMap<>();
                    callDep.put("source", fullAddr);
                    callDep.put("target", targetAddr);
                    callDep.put("type", "CALLS");
                    globalDeps.add(callDep);
                }
            } catch (Exception ignored) {}
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
            Optional<BlockStmt> opt = ((MethodDeclaration) d).getBody();
            return opt.isPresent() ? opt.get() : null;
        } else if (d instanceof ConstructorDeclaration) {
            return ((ConstructorDeclaration) d).getBody();
        }
        return null;
    }

    /**
     * Extract key statements from a callable declaration.
     * Includes: CONDITION, THROW, RETURN, EXTERNAL_CALL, SYNCHRONIZED
     */
    public List<Map<String, String>> extractKeyStatements(CallableDeclaration<?> d) {
        List<Map<String, String>> statements = new ArrayList<>();
        BlockStmt body = getCallableBody(d);
        if (body == null) return statements;

        body.findAll(com.github.javaparser.ast.stmt.IfStmt.class).forEach(ifStmt -> {
            Map<String, String> stmt = new LinkedHashMap<>();
            stmt.put("type", "CONDITION");
            stmt.put("condition", ifStmt.getCondition().toString());
            stmt.put("line", ifStmt.getBegin().map(p -> p.line).orElse(0) + "");
            statements.add(stmt);
        });

        body.findAll(ThrowStmt.class).forEach(throwStmt -> {
            Map<String, String> stmt = new LinkedHashMap<>();
            stmt.put("type", "THROW");
            stmt.put("exception", throwStmt.getExpression().toString());
            stmt.put("line", throwStmt.getBegin().map(p -> p.line).orElse(0) + "");
            statements.add(stmt);
        });

        body.findAll(com.github.javaparser.ast.stmt.ReturnStmt.class).forEach(retStmt -> {
            Map<String, String> stmt = new LinkedHashMap<>();
            stmt.put("type", "RETURN");
            stmt.put("value", retStmt.getExpression().map(Object::toString).orElse("void"));
            stmt.put("line", retStmt.getBegin().map(p -> p.line).orElse(0) + "");
            statements.add(stmt);
        });

        body.findAll(MethodCallExpr.class).forEach(call -> {
            try {
                String scope = call.getScope().map(Object::toString).orElse("");
                if (!scope.isEmpty() && !scope.equals("this") && !scope.equals("super")) {
                    Map<String, String> stmt = new LinkedHashMap<>();
                    stmt.put("type", "EXTERNAL_CALL");
                    stmt.put("target", scope + "." + call.getNameAsString());
                    stmt.put("line", call.getBegin().map(p -> p.line).orElse(0) + "");
                    statements.add(stmt);
                }
            } catch (Exception ignored) {}
        });

        body.findAll(com.github.javaparser.ast.stmt.SynchronizedStmt.class).forEach(syncStmt -> {
            Map<String, String> stmt = new LinkedHashMap<>();
            stmt.put("type", "SYNCHRONIZED");
            stmt.put("expression", syncStmt.getExpression().toString());
            stmt.put("line", syncStmt.getBegin().map(p -> p.line).orElse(0) + "");
            statements.add(stmt);
        });

        return statements;
    }

    /**
     * Summarize method body business semantics.
     */
    public String summarizeMethodBody(CallableDeclaration<?> d) {
        BlockStmt body = getCallableBody(d);
        if (body == null) return "无方法体 (abstract/native)";
        List<String> summaries = new ArrayList<>();

        if (!body.findAll(com.github.javaparser.ast.stmt.CatchClause.class).isEmpty())
            summaries.add("包含异常处理逻辑");

        int ifCount = body.findAll(com.github.javaparser.ast.stmt.IfStmt.class).size();
        if (ifCount > 0) summaries.add(ifCount + " 个条件分支");

        int loopCount = body.findAll(com.github.javaparser.ast.stmt.ForEachStmt.class).size() +
                        body.findAll(com.github.javaparser.ast.stmt.WhileStmt.class).size() +
                        body.findAll(com.github.javaparser.ast.stmt.ForStmt.class).size();
        if (loopCount > 0) summaries.add(loopCount + " 个循环结构");

        int callCount = body.findAll(MethodCallExpr.class).size();
        if (callCount > 0) summaries.add(callCount + " 次方法调用");

        if (!body.findAll(com.github.javaparser.ast.stmt.SynchronizedStmt.class).isEmpty())
            summaries.add("使用同步块");

        int returnCount = body.findAll(com.github.javaparser.ast.stmt.ReturnStmt.class).size();
        if (returnCount > 0) summaries.add(returnCount + " 个返回点");

        return summaries.isEmpty() ? "简单方法体" : String.join(", ", summaries);
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
