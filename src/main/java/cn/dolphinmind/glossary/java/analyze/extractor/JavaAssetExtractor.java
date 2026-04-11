package cn.dolphinmind.glossary.java.analyze.extractor;

import cn.dolphinmind.glossary.java.analyze.ScannerContext;
import cn.dolphinmind.glossary.java.analyze.translate.CommentAnalysisService;
import cn.dolphinmind.glossary.java.analyze.translate.SemanticEnrichmentService;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Extracts Java type assets (classes, interfaces, enums) from AST nodes.
 *
 * Split from SourceUniversePro.processTypeEnhanced() to separate asset extraction
 * from the main analysis pipeline.
 *
 * Uses:
 * - CommentAnalysisService for comment/Javadoc extraction
 * - SemanticEnrichmentService for semantic analysis
 * - MethodCallAnalyzer for method body and call graph analysis
 * - TypeAssetHelper for pure utility functions
 */
public class JavaAssetExtractor {

    private final CommentAnalysisService commentService;
    private final SemanticEnrichmentService semanticService;
    private final MethodCallAnalyzer methodAnalyzer;

    // Shared counters from SourceUniversePro
    private final AtomicInteger classCount;
    private final AtomicInteger methodCount;
    private final AtomicInteger fieldCount;

    // Shared dependency tracking set
    private final Set<String> seenDependencies;

    public JavaAssetExtractor(CommentAnalysisService commentService,
                               SemanticEnrichmentService semanticService,
                               MethodCallAnalyzer methodAnalyzer,
                               AtomicInteger classCount,
                               AtomicInteger methodCount,
                               AtomicInteger fieldCount,
                               Set<String> seenDependencies) {
        this.commentService = commentService;
        this.semanticService = semanticService;
        this.methodAnalyzer = methodAnalyzer;
        this.classCount = classCount;
        this.methodCount = methodCount;
        this.fieldCount = fieldCount;
        this.seenDependencies = seenDependencies;
    }

    /**
     * Process a type declaration and extract all asset metadata.
     * Replaces SourceUniversePro.processTypeEnhanced().
     */
    public Map<String, Object> processType(TypeDeclaration<?> type, String pkg, String parentAddr,
                                           List<String> fileLines, ScannerContext ctx,
                                           List<Map<String, String>> globalDeps,
                                           Map<String, Integer> unrecognizedClassSuffixes) {
        Map<String, Object> node = new LinkedHashMap<>();
        String address = (parentAddr == null) ? (pkg + "." + type.getNameAsString()) : (parentAddr + "$" + type.getNameAsString());

        node.put("address", address);
        node.put("kind", TypeAssetHelper.getKind(type));

        // JArchitect-style type markers
        if (type instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) type;
            node.put("is_interface", cid.isInterface());
            node.put("is_abstract", cid.isAbstract());
            node.put("extends_count", cid.getExtendedTypes().size());
            node.put("implements_count", cid.getImplementedTypes().size());
        }
        if (type instanceof EnumDeclaration) {
            node.put("is_enum", true);
        }

        // Comment extraction via CommentAnalysisService
        Map<String, Object> classCommentDetails = commentService.extractCommentDetails(fileLines, type);
        node.put("description", classCommentDetails.getOrDefault("summary", ""));
        node.put("comment_details", classCommentDetails);

        node.put("source_file", type.findCompilationUnit()
                .flatMap(CompilationUnit::getStorage)
                .map(s -> s.getPath().toString())
                .orElse(""));
        node.put("modifiers", TypeAssetHelper.resolveMods(type.getModifiers()));
        node.put("class_generics", TypeAssetHelper.resolveTypeParameters(type));

        // Semantic enrichment via SemanticEnrichmentService
        Set<String> compRoles = semanticService.extractComponentRole(type);
        node.put("component_tags", compRoles);
        node.put("semantic_profile", semanticService.extractSemanticProfile(type, fileLines));
        node.put("reasoning_results", semanticService.performLogicalInference(type, fileLines));
        node.put("arch_tags", semanticService.resolveBilingualTags(semanticService.extractArchTags(type, fileLines)));
        node.put("domain_context", semanticService.extractDomainContext(pkg));
        node.put("call_graph_summary", TypeAssetHelper.extractCallGraphSummary(type));

        // Track unrecognized class suffixes for dictionary evolution
        if (compRoles.isEmpty()) {
            String className = type.getNameAsString();
            int dotIndex = className.lastIndexOf('.');
            String simpleName = dotIndex > -1 ? className.substring(dotIndex + 1) : className;
            String suffix = simpleName.replaceAll("^[A-Z]+", "");
            if (suffix.length() > 2) {
                unrecognizedClassSuffixes.merge(suffix, 1, Integer::sum);
            }
        }

        // Method and field extraction
        node.put("methods_full", resolveMethodsSemanticEnhanced(type, fileLines, address));
        node.put("methods_intent", resolveMethodsEnhanced(type, fileLines));
        node.put("fields", resolveFieldsEnhanced(type));
        node.put("enhancement", getEnhancementData(address));
        node.put("ai_guidance", semanticService.extractAIGuidance(type, fileLines));
        node.put("ai_evolution", new LinkedHashMap<>());

        // Insight summary
        String rawDesc = commentService.bruteForceComment(fileLines, type);
        node.put("insight_summary", semanticService.translateAndSummarize(rawDesc));

        // Hierarchy
        if (type instanceof ClassOrInterfaceDeclaration) {
            node.put("hierarchy", TypeAssetHelper.resolveHierarchySemantic((ClassOrInterfaceDeclaration) type));
        }

        node.put("annotations", TypeAssetHelper.extractAnnos(type, address));

        // Field role-based extraction
        Map<String, List<Map<String, Object>>> segments = new LinkedHashMap<>();
        segments.put("INTERNAL_STATE", new ArrayList<>());
        segments.put("INTERNAL_COMPONENT", new ArrayList<>());
        segments.put("EXTERNAL_SERVICE", new ArrayList<>());

        type.getFields().forEach(f -> {
            fieldCount.incrementAndGet();
            f.getVariables().forEach(v -> {
                Map<String, Object> fMeta = new LinkedHashMap<>();
                String fullType = v.getType().asString();
                fMeta.put("name", v.getNameAsString());
                fMeta.put("address", address + "." + v.getNameAsString());
                fMeta.put("type_path", fullType);
                fMeta.put("description", commentService.bruteForceComment(fileLines, f));
                fMeta.put("modifiers", TypeAssetHelper.resolveMods(f.getModifiers()));
                segments.get(ctx.classify(fullType)).add(fMeta);
            });
        });
        node.put("field_segments", segments);

        // Field matrix and metrics
        node.put("fields_matrix", resolveFieldsMatrix(fileLines, type));
        node.put("lines_of_code", TypeAssetHelper.calculateClassLOC(type, fileLines));
        node.put("comment_lines", commentService.countCommentLines(fileLines, type));
        node.put("cyclomatic_complexity", TypeAssetHelper.calculateClassComplexity(type, fileLines));
        node.put("inheritance_depth", TypeAssetHelper.calculateInheritanceDepth(type));

        // Method extraction
        List<Map<String, Object>> methods = new ArrayList<>();
        type.getConstructors().forEach(c -> {
            methodCount.incrementAndGet();
            methods.add(extractMethodEnhanced(c, address + "#<init>", c.getParameters(), fileLines, address, globalDeps));
        });
        type.getMethods().forEach(m -> {
            methodCount.incrementAndGet();
            methods.add(extractMethodEnhanced(m, address + "#" + m.getNameAsString(), m.getParameters(), fileLines, address, globalDeps));
        });
        node.put("methods", methods);

        // Method/constructor matrices
        node.put("constructor_matrix", resolveConstructorsAligned(fileLines, type, address));
        node.put("method_matrix", resolveMethodsAligned(fileLines, type, address));

        // Inner classes (recursive)
        List<Map<String, Object>> innerClasses = new ArrayList<>();
        type.getMembers().stream().filter(m -> m instanceof TypeDeclaration)
                .forEach(m -> {
                    Map<String, Object> innerClass = processType((TypeDeclaration<?>) m, pkg, address, fileLines, ctx, globalDeps, unrecognizedClassSuffixes);
                    if (innerClass != null) {
                        classCount.incrementAndGet();
                        innerClasses.add(innerClass);
                    }
                });
        node.put("inner_classes", innerClasses);

        return node;
    }

    /**
     * Extract method asset with full metadata.
     */
    public Map<String, Object> extractMethodEnhanced(CallableDeclaration<?> d, String baseAddr,
                                                      NodeList<Parameter> params, List<String> fileLines,
                                                      String classAddr, List<Map<String, String>> globalDeps) {
        Map<String, Object> m = new LinkedHashMap<>();
        String fullAddr = baseAddr + "(" + params.stream().map(p -> p.getType().asString()).collect(Collectors.joining(",")) + ")";

        m.put("address", fullAddr);
        m.put("name", d.getNameAsString());

        Map<String, Object> commentDetails = commentService.extractCommentDetails(fileLines, d);
        m.put("description", commentDetails.getOrDefault("summary", ""));
        m.put("comment_details", commentDetails);

        m.put("modifiers", TypeAssetHelper.resolveMods(d.getModifiers()));
        m.put("line_start", d.getBegin().map(p -> p.line).orElse(0));
        m.put("line_end", d.getEnd().map(p -> p.line).orElse(0));
        m.put("signature", d.getDeclarationAsString(false, false, false));

        m.put("source_code", TypeAssetHelper.extractNodeSource(fileLines, d, true));
        m.put("body_code", methodAnalyzer.extractCallableBody(d));
        m.put("code_summary", methodAnalyzer.summarizeMethodBody(d));
        m.put("key_statements", methodAnalyzer.extractKeyStatements(d));
        m.put("line_count", methodAnalyzer.calculateLineCount(d));

        m.put("tags", TypeAssetHelper.extractMethodTags(d.getNameAsString(),
                d instanceof MethodDeclaration ? ((MethodDeclaration) d).getType().asString() : "void"));

        if (d instanceof MethodDeclaration) {
            MethodDeclaration md = (MethodDeclaration) d;
            m.put("is_override", TypeAssetHelper.checkIsOverride(md));
            m.put("method_generics", md.getTypeParameters().stream().map(tp -> tp.asString()).collect(Collectors.toList()));
            m.put("return_type_path", TypeAssetHelper.getSemanticPath(md.getType()));
            m.put("throws_matrix", md.getThrownExceptions().stream().map(TypeAssetHelper::getSemanticPath).collect(Collectors.toList()));
        }

        m.put("internal_throws", d.findAll(ThrowStmt.class).stream().map(t -> t.getExpression().toString()).distinct().collect(Collectors.toList()));
        m.put("parameters_inventory", TypeAssetHelper.resolveParametersInventory(params));

        methodAnalyzer.extractMethodCalls(d, classAddr, fullAddr, globalDeps, seenDependencies);

        return m;
    }

    /**
     * Resolve methods with semantic enhancement.
     */
    public List<Map<String, Object>> resolveMethodsSemanticEnhanced(TypeDeclaration<?> type, List<String> fileLines, String address) {
        List<Map<String, Object>> methods = new ArrayList<>();

        List<Map<String, String>> globalDeps = new ArrayList<>();
        type.getMethods().forEach(method -> {
            methodCount.incrementAndGet();
            Map<String, Object> m = extractMethodEnhanced(method, address + "#" + method.getNameAsString(),
                    method.getParameters(), fileLines, address, globalDeps);

            Set<String> semanticTags = new HashSet<>();
            int complexity = 1;
            complexity += method.findAll(com.github.javaparser.ast.stmt.IfStmt.class).size();
            complexity += method.findAll(com.github.javaparser.ast.stmt.ForEachStmt.class).size();
            complexity += method.findAll(com.github.javaparser.ast.stmt.WhileStmt.class).size();
            complexity += method.findAll(com.github.javaparser.ast.stmt.CatchClause.class).size();
            if (complexity > 10) semanticTags.add("HighComplexity");
            if (!method.findAll(MethodCallExpr.class).isEmpty()) semanticTags.add("InteractionHeavy");
            if (method.getModifiers().contains(Modifier.synchronizedModifier())) semanticTags.add("ThreadSafe");

            m.put("semantic_tags", semanticService.resolveBilingualTags(semanticTags));
            m.put("complexity_score", complexity);
            methods.add(m);
        });
        return methods;
    }

    /**
     * Enhanced method resolution with intent tags.
     */
    public List<Map<String, Object>> resolveMethodsEnhanced(TypeDeclaration<?> type, List<String> fileLines) {
        List<Map<String, Object>> methods = new ArrayList<>();
        type.getMethods().forEach(method -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", method.getNameAsString());
            m.put("return_type", method.getTypeAsString());
            m.put("modifiers", TypeAssetHelper.resolveMods(method.getModifiers()));
            m.put("parameters", TypeAssetHelper.resolveParametersInventory(method.getParameters()));
            m.put("line_start", method.getBegin().map(p -> p.line).orElse(0));
            m.put("description", semanticService.translateAndSummarize(commentService.bruteForceComment(fileLines, method)));
            methods.add(m);
        });
        return methods;
    }

    /**
     * Enhanced field resolution with semantic tags.
     */
    public List<Map<String, Object>> resolveFieldsEnhanced(TypeDeclaration<?> type) {
        List<Map<String, Object>> fields = new ArrayList<>();
        type.getFields().forEach(f -> {
            for (VariableDeclarator var : f.getVariables()) {
                Map<String, Object> field = new LinkedHashMap<>();
                field.put("name", var.getNameAsString());
                field.put("type", var.getTypeAsString());
                field.put("modifiers", TypeAssetHelper.resolveMods(f.getModifiers()));
                fields.add(field);
            }
        });
        return fields;
    }

    /**
     * Field matrix with metadata.
     */
    public List<Map<String, Object>> resolveFieldsMatrix(List<String> lines, TypeDeclaration<?> t) {
        List<Map<String, Object>> fields = new ArrayList<>();
        t.getFields().forEach(f -> {
            f.getVariables().forEach(v -> {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("name", v.getNameAsString());
                node.put("type", v.getType().asString());
                node.put("modifiers", TypeAssetHelper.resolveMods(f.getModifiers()));
                node.put("description", commentService.bruteForceComment(lines, f));
                fields.add(node);
            });
        });
        return fields;
    }

    /**
     * Constructor alignment.
     */
    public List<Map<String, Object>> resolveConstructorsAligned(List<String> lines, TypeDeclaration<?> t, String addr) {
        List<Map<String, Object>> matrix = new ArrayList<>();
        t.getConstructors().forEach(c -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("name", "<init>");
            node.put("modifiers", TypeAssetHelper.resolveMods(c.getModifiers()));
            node.put("line", c.getBegin().map(p -> p.line).orElse(0));
            node.put("description", commentService.bruteForceComment(lines, c));
            matrix.add(node);
        });
        return matrix;
    }

    /**
     * Method alignment.
     */
    public List<Map<String, Object>> resolveMethodsAligned(List<String> lines, TypeDeclaration<?> t, String addr) {
        List<Map<String, Object>> matrix = new ArrayList<>();
        t.getMethods().forEach(m -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("name", m.getNameAsString());
            node.put("modifiers", TypeAssetHelper.resolveMods(m.getModifiers()));
            node.put("line", m.getBegin().map(p -> p.line).orElse(0));
            node.put("description", commentService.bruteForceComment(lines, m));
            matrix.add(node);
        });
        return matrix;
    }

    /**
     * Enhancement data (placeholder for future AI-driven best practices).
     */
    public Map<String, Object> getEnhancementData(String address) {
        Map<String, Object> enhancement = new LinkedHashMap<>();
        enhancement.put("scenario_case", "待 AI Agent 补充");
        enhancement.put("best_practice", "待分析");
        enhancement.put("related_concepts", "[]");
        return enhancement;
    }
}
