package cn.dolphinmind.glossary.java.analyze.slicing;

import java.util.logging.Logger;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AST 精准切片器
 * 
 * 使用 JavaParser 从源码提取精确代码切片, 避免无关代码入模
 * 
 * 功能:
 * 1. 按方法名精准切片
 * 2. 按行号范围切片 (对应静态分析发现的 issue 行)
 * 3. 提取方法调用依赖
 * 4. 预估 Token 消耗
 */
public class CodeSlicer {
    private static final Logger logger = Logger.getLogger(CodeSlicer.class.getName());

    private final JavaParser javaParser;

    public CodeSlicer() {
        this.javaParser = new JavaParser();
    }

    /**
     * 从文件切片整个类
     */
    public CodeSlice sliceClass(Path sourceFile) throws IOException {
        ParseResult<CompilationUnit> result = javaParser.parse(sourceFile);
        if (!result.isSuccessful() || !result.getResult().isPresent()) {
            return createErrorSlice(sourceFile.toString());
        }

        CompilationUnit cu = result.getResult().get();
        Optional<TypeDeclaration<?>> primaryType = cu.getPrimaryType();
        if (!primaryType.isPresent()) {
            return createErrorSlice(sourceFile.toString());
        }

        TypeDeclaration<?> type = primaryType.get();
        CodeSlice slice = new CodeSlice();
        slice.setFilePath(sourceFile.toString());
        slice.setClassName(getFullClassName(cu, type.getNameAsString()));
        slice.setType(CodeSlice.SliceType.CLASS);
        slice.setStartLine(type.getBegin().map(p -> p.line).orElse(0));
        slice.setEndLine(type.getEnd().map(p -> p.line).orElse(0));
        slice.setCode(type.toString());
        slice.setDependencies(extractDependencies(type));
        slice.setContext(buildClassContext(type));

        return slice;
    }

    /**
     * 按方法名切片
     */
    public CodeSlice sliceMethod(Path sourceFile, String methodName) throws IOException {
        ParseResult<CompilationUnit> result = javaParser.parse(sourceFile);
        if (!result.isSuccessful() || !result.getResult().isPresent()) {
            return createErrorSlice(sourceFile.toString());
        }

        CompilationUnit cu = result.getResult().get();
        Optional<TypeDeclaration<?>> primaryType = cu.getPrimaryType();
        if (!primaryType.isPresent()) return createErrorSlice(sourceFile.toString());

        TypeDeclaration<?> type = primaryType.get();
        Optional<MethodDeclaration> methodOpt = findMethod(type, methodName);
        if (!methodOpt.isPresent()) {
            return createNotFoundSlice(sourceFile.toString(), methodName);
        }

        MethodDeclaration method = methodOpt.get();
        CodeSlice slice = new CodeSlice();
        slice.setFilePath(sourceFile.toString());
        slice.setClassName(getFullClassName(cu, type.getNameAsString()));
        slice.setMethodName(methodName);
        slice.setType(CodeSlice.SliceType.METHOD);
        slice.setStartLine(method.getBegin().map(p -> p.line).orElse(0));
        slice.setEndLine(method.getEnd().map(p -> p.line).orElse(0));
        slice.setCode(method.toString());
        slice.setDependencies(extractMethodDependencies(method));
        slice.setContext(buildMethodContext(method));

        return slice;
    }

    /**
     * 按行号范围切片 (用于精确定位静态分析发现的 issue 所在代码块)
     */
    public CodeSlice sliceByLineRange(Path sourceFile, int targetLine, int contextLines) throws IOException {
        String content = new String(Files.readAllBytes(sourceFile), StandardCharsets.UTF_8);
        String[] lines = content.split("\n");

        if (targetLine < 1 || targetLine > lines.length) {
            return createNotFoundSlice(sourceFile.toString(), "line " + targetLine);
        }

        // 找到包含目标行的方法
        ParseResult<CompilationUnit> result = javaParser.parse(sourceFile);
        int startLine = Math.max(1, targetLine - contextLines);
        int endLine = Math.min(lines.length, targetLine + contextLines);

        // 尝试找到方法包裹
        if (result.isSuccessful() && result.getResult().isPresent()) {
            CompilationUnit cu = result.getResult().get();
            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                int mStart = method.getBegin().map(p -> p.line).orElse(0);
                int mEnd = method.getEnd().map(p -> p.line).orElse(0);
                if (targetLine >= mStart && targetLine <= mEnd) {
                    // 方法包含了目标行, 扩展上下文
                    startLine = Math.max(1, mStart - 2);
                    endLine = Math.min(lines.length, mEnd + 2);
                    break;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = startLine - 1; i < endLine; i++) {
            sb.append(lines[i]).append("\n");
        }

        CodeSlice slice = new CodeSlice();
        slice.setFilePath(sourceFile.toString());
        slice.setMethodName(null);
        slice.setType(CodeSlice.SliceType.ISSUE_AREA);
        slice.setStartLine(startLine);
        slice.setEndLine(endLine);
        slice.setCode(sb.toString());

        return slice;
    }

    /**
     * 批量切片: 从分析结果中提取所有有问题的代码区域
     */
    @SuppressWarnings("unchecked")
    public List<CodeSlice> sliceIssues(Path sourceRoot, Map<String, Object> analysisResult) throws IOException {
        List<CodeSlice> slices = new ArrayList<>();
        Object rawIssues = analysisResult.get("quality_issues");
        if (!(rawIssues instanceof List)) return slices;

        Set<String> slicedKeys = new HashSet<>();
        for (Object obj : (List<?>) rawIssues) {
            if (!(obj instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) obj;
            String file = (String) m.get("file");
            int line = m.get("line") instanceof Number ? ((Number) m.get("line")).intValue() : 0;
            String method = (String) m.get("method");

            if (file == null) continue;

            // 找对应的源文件
            Path sourceFile = findSourceFile(sourceRoot, file);
            if (sourceFile == null || !Files.exists(sourceFile)) continue;

            String uniqueKey = file + ":" + line;
            if (slicedKeys.contains(uniqueKey)) continue;
            slicedKeys.add(uniqueKey);

            CodeSlice slice;
            if (method != null && !method.isEmpty()) {
                slice = sliceMethod(sourceFile, method);
            } else {
                slice = sliceByLineRange(sourceFile, line, 5);
            }

            // 添加静态分析上下文
            Map<String, Object> ctx = slice.getContext() != null ? slice.getContext() : new LinkedHashMap<String, Object>();
            ctx.put("issueRule", m.get("rule_key"));
            ctx.put("issueMessage", m.get("message"));
            ctx.put("issueSeverity", m.get("severity"));
            slice.setContext(ctx);

            slices.add(slice);
        }

        return slices;
    }

    /**
     * 生成 RAG 可用的 Context 文档
     */
    public Map<String, Object> buildRagContext(Path sourceRoot, Map<String, Object> analysisResult) throws IOException {
        Map<String, Object> ragCtx = new LinkedHashMap<>();
        List<CodeSlice> slices = sliceIssues(sourceRoot, analysisResult);

        long totalTokens = 0;
        List<Map<String, Object>> sliceMaps = new ArrayList<>();
        for (CodeSlice s : slices) {
            sliceMaps.add(s.toMap());
            totalTokens += s.estimateTokenCount();
        }

        ragCtx.put("totalSlices", slices.size());
        ragCtx.put("totalEstimatedTokens", totalTokens);
        ragCtx.put("slices", sliceMaps);
        ragCtx.put("staticAnalysisSummary", extractSummary(analysisResult));

        return ragCtx;
    }

    // ========== 内部方法 ==========

    private String getFullClassName(CompilationUnit cu, String shortName) {
        if (cu.getPackageDeclaration().isPresent()) {
            return cu.getPackageDeclaration().get().getNameAsString() + "." + shortName;
        }
        return shortName;
    }

    @SuppressWarnings("unchecked")
    private Optional<MethodDeclaration> findMethod(TypeDeclaration<?> type, String name) {
        if (type instanceof NodeWithMembers) {
            NodeWithMembers<?> nwm = (NodeWithMembers<?>) type;
            return nwm.getMethods().stream()
                    .filter(m -> m.getNameAsString().equals(name))
                    .findFirst();
        }
        return Optional.empty();
    }

    private List<String> extractDependencies(TypeDeclaration<?> type) {
        Set<String> deps = new LinkedHashSet<>();
        if (type instanceof NodeWithMembers) {
            NodeWithMembers<?> nwm = (NodeWithMembers<?>) type;
            for (MethodDeclaration method : nwm.getMethods()) {
                deps.addAll(extractMethodDependencies(method));
            }
        }
        return new ArrayList<>(deps);
    }

    private List<String> extractMethodDependencies(MethodDeclaration method) {
        Set<String> deps = new LinkedHashSet<>();
        for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
            String scope = call.getScope().map(s -> s.toString()).orElse("");
            String name = call.getNameAsString();
            if (!scope.isEmpty() && !isJavaLang(scope)) {
                deps.add(scope + "." + name + "()");
            }
        }
        return new ArrayList<>(deps);
    }

    private boolean isJavaLang(String name) {
        return name.startsWith("System.") || name.startsWith("String.") ||
               name.startsWith("Integer.") || name.startsWith("Long.") ||
               name.startsWith("Boolean.") || name.startsWith("Object.") ||
               name.startsWith("Math.") || name.startsWith("Arrays.") ||
               name.startsWith("Collections.") || name.startsWith("Optional.");
    }

    private Map<String, Object> buildClassContext(TypeDeclaration<?> type) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("type", type.getClass().getSimpleName());
        if (type instanceof NodeWithMembers) {
            NodeWithMembers<?> nwm = (NodeWithMembers<?>) type;
            ctx.put("methodCount", nwm.getMethods().size());
            ctx.put("fieldCount", nwm.getFields().size());
        }
        return ctx;
    }

    private Map<String, Object> buildMethodContext(MethodDeclaration method) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("parameters", method.getParameters().size());
        ctx.put("callsCount", method.findAll(MethodCallExpr.class).size());
        return ctx;
    }

    private CodeSlice createErrorSlice(String filePath) {
        CodeSlice s = new CodeSlice();
        s.setFilePath(filePath);
        s.setCode("// ERROR: Failed to parse file");
        s.setType(CodeSlice.SliceType.METHOD);
        return s;
    }

    private CodeSlice createNotFoundSlice(String filePath, String target) {
        CodeSlice s = new CodeSlice();
        s.setFilePath(filePath);
        s.setCode("// NOT FOUND: " + target);
        s.setType(CodeSlice.SliceType.METHOD);
        return s;
    }

    private Path findSourceFile(Path sourceRoot, String filePath) throws IOException {
        // Try exact path first
        Path p = Paths.get(filePath);
        if (Files.exists(p)) return p;

        // Try relative to sourceRoot
        if (filePath.startsWith("/")) {
            int idx = filePath.indexOf("/src/main/java/");
            if (idx > 0) {
                String relative = filePath.substring(idx + 1);
                p = sourceRoot.resolve(relative);
                if (Files.exists(p)) return p;
            }
        }

        // Search by filename
        String fileName = Paths.get(filePath).getFileName().toString();
        try (java.util.stream.Stream<Path> stream = Files.walk(sourceRoot, 20)) {
            return stream.filter(f -> f.getFileName().toString().equals(fileName))
                    .filter(f -> f.toString().contains("/src/main/java/"))
                    .findFirst().orElse(null);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractSummary(Map<String, Object> analysisResult) {
        Map<String, Object> summary = new LinkedHashMap<>();
        Object qs = analysisResult.get("quality_summary");
        if (qs instanceof Map) summary.putAll((Map<String, Object>) qs);
        Object cm = analysisResult.get("code_metrics");
        if (cm instanceof Map) summary.put("codeMetrics", cm);
        return summary;
    }

    /**
     * 命令行入口
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java CodeSlicer <sourceRoot> <jsonFile> <outputJson>");
            System.out.println("  sourceRoot  - Java source root directory");
            System.out.println("  jsonFile    - Static analysis result JSON");
            System.out.println("  outputJson  - Output RAG context JSON");
            return;
        }

        try {
            Path sourceRoot = Paths.get(args[0]);
            String jsonContent = new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> analysisResult = parseJson(jsonContent);

            CodeSlicer slicer = new CodeSlicer();
            Map<String, Object> ragContext = slicer.buildRagContext(sourceRoot, analysisResult);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String output = gson.toJson(ragContext);
            try (PrintWriter pw = new PrintWriter(args[2], "UTF-8")) { pw.write(output); }

            System.out.println("OK: " + args[2]);
            System.out.println("Slices: " + ragContext.get("totalSlices"));
            System.out.println("Estimated tokens: " + ragContext.get("totalEstimatedTokens"));

        } catch (Exception e) {
            logger.warning("Slicing failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJson(String json) {
        return new com.google.gson.Gson().fromJson(json, Map.class);
    }
}
