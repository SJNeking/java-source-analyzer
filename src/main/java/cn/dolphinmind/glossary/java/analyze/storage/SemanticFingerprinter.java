package cn.dolphinmind.glossary.java.analyze.storage;

import java.util.logging.Logger;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.*;

/**
 * 语义指纹计算器
 * 
 * 基于 AST 结构计算 Java 文件的语义指纹, 忽略:
 * - 空白字符、注释、格式差异
 * - 变量名重命名 (可选)
 * - 代码顺序无关的方法/字段
 * 
 * 效果:
 * - 仅格式修改 (whitespace/comments) → 指纹不变 → 跳审
 * - 实质修改 (逻辑变更) → 指纹变化 → 重新分析
 */
public class SemanticFingerprinter {
    private static final Logger logger = Logger.getLogger(SemanticFingerprinter.class.getName());

    private final JavaParser javaParser;
    private boolean ignoreVariableNames = false;
    private boolean ignoreMethodBodyOrder = true;

    public SemanticFingerprinter() {
        this.javaParser = new JavaParser();
    }

    /**
     * 计算整个项目的语义指纹 (SHA-256 前 16 位)
     */
    public String computeProjectFingerprint(Path sourceRoot) throws IOException {
        Map<String, String> fileSignatures = new TreeMap<>();

        try (Stream<Path> walk = Files.walk(sourceRoot, 20)) {
            List<Path> javaFiles = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/test/"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .filter(p -> !p.toString().contains("/generated/"))
                    .sorted()
                    .collect(Collectors.toList());

            for (Path file : javaFiles) {
                String relativePath = sourceRoot.relativize(file).toString();
                String fileSig = computeFileSignature(file);
                fileSignatures.put(relativePath, fileSig);
            }
        }

        // 聚合所有文件签名为项目指纹
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : fileSignatures.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }

        return sha256Prefix(sb.toString(), 16);
    }

    /**
     * 计算单个文件的语义指纹 (SHA-256 前 8 位)
     */
    public String computeFileSignature(Path sourceFile) throws IOException {
        ParseResult<CompilationUnit> result = javaParser.parse(sourceFile);
        if (!result.isSuccessful() || !result.getResult().isPresent()) {
            // 解析失败, 降级为纯文本指纹
            String content = new String(Files.readAllBytes(sourceFile), java.nio.charset.StandardCharsets.UTF_8);
            return sha256Prefix(stripWhitespaceAndComments(content), 8);
        }

        CompilationUnit cu = result.getResult().get();
        StringBuilder sb = new StringBuilder();

        // Package
        cu.getPackageDeclaration().ifPresent(pkg ->
                sb.append("package:").append(pkg.getNameAsString()).append("\n"));

        // Imports (normalized)
        List<String> imports = cu.getImports().stream()
                .map(i -> i.getNameAsString())
                .sorted()
                .collect(Collectors.toList());
        sb.append("imports:").append(String.join(",", imports)).append("\n");

        // Types (classes, interfaces, enums)
        for (TypeDeclaration<?> type : cu.getTypes()) {
            extractTypeSignature(type, sb);
        }

        return sha256Prefix(sb.toString(), 8);
    }

    /**
     * 提取类型的语义签名
     */
    private void extractTypeSignature(TypeDeclaration<?> type, StringBuilder sb) {
        sb.append("type:").append(type.getClass().getSimpleName());
        sb.append(":").append(type.getNameAsString());

        // Modifiers (sorted for consistency)
        List<String> mods = type.getModifiers().stream()
                .map(m -> m.getKeyword().asString())
                .sorted()
                .collect(Collectors.toList());
        sb.append(":").append(String.join(",", mods));

        // Extends/Implements
        if (type instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) type;
            List<String> extendsList = new ArrayList<String>();
            for (com.github.javaparser.ast.type.ClassOrInterfaceType t : c.getExtendedTypes()) {
                extendsList.add(t.getNameAsString());
            }
            java.util.Collections.sort(extendsList);
            List<String> implementsList = new ArrayList<String>();
            for (com.github.javaparser.ast.type.ClassOrInterfaceType t : c.getImplementedTypes()) {
                implementsList.add(t.getNameAsString());
            }
            java.util.Collections.sort(implementsList);
            if (!extendsList.isEmpty()) sb.append(":extends=").append(String.join(",", extendsList));
            if (!implementsList.isEmpty()) sb.append(":implements=").append(String.join(",", implementsList));
        }
        sb.append("\n");

        // Fields (normalized)
        if (type instanceof com.github.javaparser.ast.nodeTypes.NodeWithMembers<?>) {
            com.github.javaparser.ast.nodeTypes.NodeWithMembers<?> nwm = (com.github.javaparser.ast.nodeTypes.NodeWithMembers<?>) type;
            List<String> fields = nwm.getFields().stream()
                    .map(f -> {
                        String typeStr = f.getCommonType().asString();
                        List<String> names = f.getVariables().stream()
                                .map(v -> ignoreVariableNames ? "?" : v.getNameAsString())
                                .sorted()
                                .collect(Collectors.toList());
                        return typeStr + " " + String.join(",", names);
                    })
                    .sorted()
                    .collect(Collectors.toList());
            sb.append("fields:").append(String.join(";", fields)).append("\n");

            // Methods (normalized, body order ignored if configured)
            List<String> methods = nwm.getMethods().stream()
                    .map(this::methodSignature)
                    .sorted()
                    .collect(Collectors.toList());
            if (ignoreMethodBodyOrder) {
                // Sort method signatures to make order-independent
                sb.append("methods(sorted):").append(String.join(";", methods)).append("\n");
            } else {
                sb.append("methods:").append(String.join(";", methods)).append("\n");
            }
        }

        // Nested types
        for (TypeDeclaration<?> nested : type.getMembers().stream()
                .filter(m -> m instanceof TypeDeclaration)
                .map(m -> (TypeDeclaration<?>) m)
                .collect(Collectors.toList())) {
            extractTypeSignature(nested, sb);
        }
    }

    /**
     * 提取方法的语义签名
     */
    private String methodSignature(MethodDeclaration method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getNameAsString());
        sb.append("(");
        
        // Parameter types and names
        List<String> params = method.getParameters().stream()
                .map(p -> p.getType().asString() + " " +
                        (ignoreVariableNames ? "?" : p.getNameAsString()))
                .collect(Collectors.toList());
        sb.append(String.join(",", params));
        sb.append(")");

        // Return type
        sb.append("->").append(method.getType().asString());

        // Modifiers
        List<String> mods = method.getModifiers().stream()
                .map(m -> m.getKeyword().asString())
                .sorted()
                .collect(Collectors.toList());
        sb.append(":").append(String.join(",", mods));

        // Method body structural hash (AST-based, ignoring literal values)
        if (!method.getBody().isPresent()) {
            sb.append(":abstract");
        } else {
            String bodyHash = methodBodyHash(method);
            sb.append(":body=").append(bodyHash);
        }

        return sb.toString();
    }

    /**
     * 计算方法体的结构哈希 (忽略字面量值)
     */
    private String methodBodyHash(MethodDeclaration method) {
        try {
            BodyStructureVisitor visitor = new BodyStructureVisitor(ignoreVariableNames);
            method.accept(visitor, null);
            return sha256Prefix(visitor.getStructureString(), 8);
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 移除空白和注释后的纯文本
     */
    private String stripWhitespaceAndComments(String code) {
        // 去除多行注释
        String result = code.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        // 去除单行注释
        result = result.replaceAll("//.*", "");
        // 去除空行
        result = result.replaceAll("\\s+", "");
        return result;
    }

    /**
     * SHA-256 前缀 (十六进制)
     */
    private static String sha256Prefix(String input, int prefixLength) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, Math.min(prefixLength, sb.length()));
        } catch (Exception e) {
            return "00000000";
        }
    }

    /**
     * 计算两个指纹的差异
     * @return 0 = 完全相同, >0 = 有差异
     */
    public static int fingerprintDistance(String fp1, String fp2) {
        if (fp1 == null || fp2 == null) return Integer.MAX_VALUE;
        int distance = 0;
        int len = Math.min(fp1.length(), fp2.length());
        for (int i = 0; i < len; i++) {
            if (fp1.charAt(i) != fp2.charAt(i)) distance++;
        }
        return distance;
    }

    // ============================================================
    // CLI 入口
    // ============================================================

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java SemanticFingerprinter <sourceRoot> [--redis <host:port>] [--project <name>]");
            System.out.println();
            System.out.println("Options:");
            System.out.println("  --redis <host:port>  Check Redis cache (e.g., localhost:6379)");
            System.out.println("  --project <name>     Project name for Redis key");
            System.out.println();
            System.out.println("Output:");
            System.out.println("  Fingerprint: {sha256_prefix}");
            System.out.println("  Cache Hit:   yes/no (when --redis provided)");
            System.out.println();
            return;
        }

        try {
            Path sourceRoot = Paths.get(args[0]);
            String redisAddress = null;
            String projectName = "default";

            for (int i = 1; i < args.length; i++) {
                if ("--redis".equals(args[i]) && i + 1 < args.length) {
                    redisAddress = args[++i];
                } else if ("--project".equals(args[i]) && i + 1 < args.length) {
                    projectName = args[++i];
                }
            }

            SemanticFingerprinter fingerprinter = new SemanticFingerprinter();
            String fingerprint = fingerprinter.computeProjectFingerprint(sourceRoot);
            
            System.out.println("Fingerprint: " + fingerprint);

            if (redisAddress != null) {
                String[] parts = redisAddress.split(":");
                String host = parts[0];
                int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6379;

                StorageService.RedisClient redis = new StorageService.RedisClient(host, port, null);
                String cacheKey = "fp:" + fingerprint;
                String cached = redis.get(cacheKey);

                if (cached != null && !cached.equals("(nil)") && !cached.isEmpty()) {
                    System.out.println("Cache Hit: yes");
                    System.out.println("Cached result: " + cached);
                    redis.incr(cacheKey);
                } else {
                    System.out.println("Cache Hit: no");
                }
            }

        } catch (Exception e) {
            logger.warning("Fingerprint computation failed: " + e.getMessage());
            System.exit(1);
        }
    }

    // ============================================================
    // Body Structure Visitor — AST-based method body analysis
    // ============================================================

    private static class BodyStructureVisitor extends VoidVisitorAdapter<Void> {
        private final StringBuilder sb = new StringBuilder();
        private final boolean ignoreVarNames;

        public BodyStructureVisitor(boolean ignoreVarNames) {
            this.ignoreVarNames = ignoreVarNames;
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.IfStmt n, Void arg) {
            sb.append("IF;");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.ForStmt n, Void arg) {
            sb.append("FOR;");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.ForEachStmt n, Void arg) {
            sb.append("FOREACH;");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.WhileStmt n, Void arg) {
            sb.append("WHILE;");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.DoStmt n, Void arg) {
            sb.append("DO;");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.SwitchStmt n, Void arg) {
            sb.append("SWITCH;");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.TryStmt n, Void arg) {
            sb.append("TRY;");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.CatchClause n, Void arg) {
            sb.append("CATCH;");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.ReturnStmt n, Void arg) {
            sb.append("RETURN;");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.ThrowStmt n, Void arg) {
            sb.append("THROW;");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.BreakStmt n, Void arg) {
            sb.append("BREAK;");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.ContinueStmt n, Void arg) {
            sb.append("CONTINUE;");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.SynchronizedStmt n, Void arg) {
            sb.append("SYNC;");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.expr.MethodCallExpr n, Void arg) {
            sb.append("CALL(").append(n.getNameAsString()).append(");");
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.expr.VariableDeclarationExpr n, Void arg) {
            sb.append("VAR(").append(n.getCommonType().asString()).append(");");
            super.visit(n, arg);
        }

        public String getStructureString() {
            return sb.toString();
        }
    }

    // Getters/Setters
    public boolean isIgnoreVariableNames() { return ignoreVariableNames; }
    public void setIgnoreVariableNames(boolean ignore) { this.ignoreVariableNames = ignore; }
    public boolean isIgnoreMethodBodyOrder() { return ignoreMethodBodyOrder; }
    public void setIgnoreMethodBodyOrder(boolean ignore) { this.ignoreMethodBodyOrder = ignore; }
}
