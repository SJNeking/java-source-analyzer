package cn.dolphinmind.glossary.java.analyze.extractor;

import cn.dolphinmind.glossary.java.analyze.ScannerContext;
import cn.dolphinmind.glossary.java.analyze.translate.CommentAnalysisService;
import cn.dolphinmind.glossary.java.analyze.translate.SemanticEnrichmentService;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
 */
public class JavaAssetExtractor {

    private final CommentAnalysisService commentService;
    private final SemanticEnrichmentService semanticService;
    private final MethodCallAnalyzer methodAnalyzer;

    // Shared counters from SourceUniversePro
    private final AtomicInteger classCount;
    private final AtomicInteger methodCount;
    private final AtomicInteger fieldCount;

    // Legacy bridge: still calls some static methods in SourceUniversePro
    // These will be fully migrated in a future phase
    private final LegacyBridge legacyBridge;

    public JavaAssetExtractor(CommentAnalysisService commentService,
                               SemanticEnrichmentService semanticService,
                               MethodCallAnalyzer methodAnalyzer,
                               AtomicInteger classCount,
                               AtomicInteger methodCount,
                               AtomicInteger fieldCount,
                               LegacyBridge legacyBridge) {
        this.commentService = commentService;
        this.semanticService = semanticService;
        this.methodAnalyzer = methodAnalyzer;
        this.classCount = classCount;
        this.methodCount = methodCount;
        this.fieldCount = fieldCount;
        this.legacyBridge = legacyBridge;
    }

    /**
     * Process a type declaration and extract all asset metadata.
     * Replaces SourceUniversePro.processTypeEnhanced().
     */
    public Map<String, Object> processType(TypeDeclaration<?> type, String pkg, String parentAddr,
                                           List<String> fileLines, ScannerContext ctx,
                                           List<Map<String, String>> globalDeps) {
        Map<String, Object> node = new LinkedHashMap<>();
        String address = (parentAddr == null) ? (pkg + "." + type.getNameAsString()) : (parentAddr + "$" + type.getNameAsString());

        node.put("address", address);
        node.put("kind", legacyBridge.getKind(type));

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
        node.put("modifiers", methodAnalyzer.resolveMods(type.getModifiers()));
        node.put("class_generics", methodAnalyzer.resolveTypeParameters(type));

        // Semantic enrichment via SemanticEnrichmentService
        Set<String> compRoles = semanticService.extractComponentRole(type);
        node.put("component_tags", compRoles);
        node.put("semantic_profile", semanticService.extractSemanticProfile(type, fileLines));
        node.put("reasoning_results", semanticService.performLogicalInference(type, fileLines));
        node.put("arch_tags", semanticService.resolveBilingualTags(semanticService.extractArchTags(type, fileLines)));
        node.put("domain_context", semanticService.extractDomainContext(pkg));
        node.put("call_graph_summary", legacyBridge.extractCallGraphSummary(type));

        // Track unrecognized class suffixes for dictionary evolution
        if (compRoles.isEmpty()) {
            String className = type.getNameAsString();
            int dotIndex = className.lastIndexOf('.');
            String simpleName = dotIndex > -1 ? className.substring(dotIndex + 1) : className;
            String suffix = simpleName.replaceAll("^[A-Z]+", "");
            if (suffix.length() > 2) {
                legacyBridge.trackUnrecognizedSuffix(suffix);
            }
        }

        // Method and field extraction (legacy bridge for now)
        node.put("methods_full", legacyBridge.resolveMethodsSemanticEnhanced(type, fileLines));
        node.put("methods_intent", legacyBridge.resolveMethodsEnhanced(type, fileLines));
        node.put("fields", legacyBridge.resolveFieldsEnhanced(type));
        node.put("enhancement", legacyBridge.getEnhancementData(address));
        node.put("ai_guidance", semanticService.extractAIGuidance(type, fileLines));
        node.put("ai_evolution", new LinkedHashMap<>());

        // Insight summary
        String rawDesc = commentService.bruteForceComment(fileLines, type);
        node.put("insight_summary", semanticService.translateAndSummarize(rawDesc));

        // Hierarchy
        if (type instanceof ClassOrInterfaceDeclaration) {
            node.put("hierarchy", methodAnalyzer.resolveHierarchySemantic((ClassOrInterfaceDeclaration) type));
        }

        node.put("annotations", legacyBridge.extractAnnos(type, address));

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
                fMeta.put("modifiers", methodAnalyzer.resolveMods(f.getModifiers()));
                segments.get(ctx.classify(fullType)).add(fMeta);
            });
        });
        node.put("field_segments", segments);

        // Field matrix and metrics
        node.put("fields_matrix", legacyBridge.resolveFieldsMatrix(fileLines, type));
        node.put("lines_of_code", legacyBridge.calculateClassLOC(type, fileLines));
        node.put("comment_lines", commentService.countCommentLines(fileLines, type));
        node.put("cyclomatic_complexity", legacyBridge.calculateClassComplexity(type, fileLines));
        node.put("inheritance_depth", legacyBridge.calculateInheritanceDepth(type));

        // Method extraction
        List<Map<String, Object>> methods = new ArrayList<>();
        type.getConstructors().forEach(c -> {
            methodCount.incrementAndGet();
            methods.add(legacyBridge.extractMethodEnhanced(c, address + "#<init>", c.getParameters(), fileLines, address, globalDeps));
        });
        type.getMethods().forEach(m -> {
            methodCount.incrementAndGet();
            legacyBridge.trackMethodName(m.getNameAsString());
            methods.add(legacyBridge.extractMethodEnhanced(m, address + "#" + m.getNameAsString(), m.getParameters(), fileLines, address, globalDeps));
        });
        node.put("methods", methods);

        // Method/constructor matrices
        node.put("constructor_matrix", legacyBridge.resolveConstructorsAligned(fileLines, type, address));
        node.put("method_matrix", legacyBridge.resolveMethodsAligned(fileLines, type, address));

        // Inner classes (recursive)
        List<Map<String, Object>> innerClasses = new ArrayList<>();
        type.getMembers().stream().filter(m -> m instanceof TypeDeclaration)
                .forEach(m -> {
                    Map<String, Object> innerClass = processType((TypeDeclaration<?>) m, pkg, address, fileLines, ctx, globalDeps);
                    if (innerClass != null) {
                        classCount.incrementAndGet();
                        innerClasses.add(innerClass);
                    }
                });
        node.put("inner_classes", innerClasses);

        return node;
    }

    /**
     * Legacy bridge: methods still in SourceUniversePro that haven't been fully migrated.
     * These will be removed once all dependencies are extracted.
     */
    public interface LegacyBridge {
        String getKind(TypeDeclaration<?> type);
        Map<String, Object> extractMethodEnhanced(CallableDeclaration<?> d, String baseAddr,
                                                   com.github.javaparser.ast.NodeList<com.github.javaparser.ast.body.Parameter> params,
                                                   List<String> fileLines, String classAddr,
                                                   List<Map<String, String>> globalDeps);
        List<Map<String, Object>> resolveMethodsSemanticEnhanced(TypeDeclaration<?> type, List<String> fileLines);
        List<Map<String, Object>> resolveMethodsEnhanced(TypeDeclaration<?> type, List<String> fileLines);
        List<Map<String, Object>> resolveFieldsEnhanced(TypeDeclaration<?> type);
        List<Map<String, Object>> resolveFieldsMatrix(List<String> lines, TypeDeclaration<?> t);
        List<Map<String, Object>> resolveConstructorsAligned(List<String> lines, TypeDeclaration<?> t, String addr);
        List<Map<String, Object>> resolveMethodsAligned(List<String> lines, TypeDeclaration<?> t, String addr);
        Map<String, Object> getEnhancementData(String address);
        Map<String, Object> extractCallGraphSummary(TypeDeclaration<?> type);
        int calculateClassLOC(TypeDeclaration<?> type, List<String> fileLines);
        int calculateClassComplexity(TypeDeclaration<?> type, List<String> fileLines);
        int calculateInheritanceDepth(TypeDeclaration<?> type);
        List<Map<String, Object>> extractAnnos(TypeDeclaration<?> type, String address);
        void trackUnrecognizedSuffix(String suffix);
        void trackMethodName(String name);
    }
}
