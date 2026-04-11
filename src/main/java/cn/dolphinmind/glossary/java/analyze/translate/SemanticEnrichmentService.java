package cn.dolphinmind.glossary.java.analyze.translate;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;

import java.util.*;
import java.util.regex.*;

/**
 * Semantic enrichment service for Java AST nodes.
 *
 * Extracted from SourceUniversePro to decouple semantic analysis from the main pipeline.
 * Handles:
 * - Identifier translation (camelCase/Snake_Case to Chinese)
 * - Logical inference based on naming patterns and structure
 * - AI guidance generation (teaching hints, constraints, scenarios)
 * - Architecture tag extraction
 * - Component role detection
 * - Domain context extraction
 * - Semantic profile extraction (inheritance, complexity, methods)
 */
public class SemanticEnrichmentService {

    private final SemanticTranslator translator;
    private Map<String, String> namingTags;
    private Map<String, String> codeExamples;

    public SemanticEnrichmentService(SemanticTranslator translator) {
        this.translator = translator;
        this.namingTags = new HashMap<>();
        this.codeExamples = new HashMap<>();
    }

    public void setNamingTags(Map<String, String> tags) { this.namingTags = tags; }
    public void setCodeExamples(Map<String, String> examples) { this.codeExamples = examples; }

    // =====================================================================
    // Translation & Terminology
    // =====================================================================

    /**
     * Translate an identifier name to Chinese using dictionary lookup and suffix inference.
     */
    public String translateIdentifier(String name) {
        if (name == null || name.isEmpty()) return name;
        // Split camelCase or snake_case
        String[] tokens = name.split("(?<=[a-z])(?=[A-Z])|_");
        List<String> translations = new ArrayList<>();
        for (String token : tokens) {
            String term = lookupTerm(token);
            if (term != null && !term.isEmpty()) {
                translations.add(term);
            } else {
                String suffixResult = inferBySuffix(token);
                if (suffixResult != null) {
                    translations.add(suffixResult);
                } else {
                    translations.add(token);
                }
            }
        }
        return String.join("", translations);
    }

    /**
     * Look up a term in the translator's dictionaries.
     */
    private String lookupTerm(String term) {
        if (term == null || term.isEmpty()) return null;
        String key = term.toLowerCase();
        // Check naming tags first
        if (namingTags.containsKey(key)) {
            return namingTags.get(key);
        }
        // Then use translator
        if (translator != null) {
            return translator.translateIdentifier(key);
        }
        return null;
    }

    /**
     * Infer meaning by common suffix patterns.
     */
    private String inferBySuffix(String token) {
        if (token == null) return null;
        String lower = token.toLowerCase();
        if (lower.endsWith("er") || lower.endsWith("or")) return "处理者";
        if (lower.endsWith("tion") || lower.endsWith("sion")) return "操作";
        if (lower.endsWith("ment")) return "结果";
        if (lower.endsWith("ness")) return "性质";
        if (lower.endsWith("ity")) return "属性";
        if (lower.endsWith("able")) return "可操作的";
        if (lower.endsWith("ful")) return "充满的";
        if (lower.endsWith("less")) return "无的";
        if (lower.endsWith("ing")) return "进行中";
        if (lower.endsWith("ed")) return "已完成的";
        if (lower.endsWith("y")) return "相关的";
        return null;
    }

    // =====================================================================
    // Component Role Detection
    // =====================================================================

    /**
     * Detect component roles based on annotations and naming patterns.
     */
    public Set<String> extractComponentRole(TypeDeclaration<?> type) {
        Set<String> roles = new LinkedHashSet<>();
        String name = type.getNameAsString();

        // Annotation-based detection
        if (type.getAnnotations() != null) {
            for (com.github.javaparser.ast.expr.AnnotationExpr ann : type.getAnnotations()) {
                String annName = ann.getNameAsString();
                if (annName.contains("Controller") || annName.contains("Rest")) roles.add("WEB_LAYER");
                if (annName.contains("Service")) roles.add("BUSINESS_LAYER");
                if (annName.contains("Repository") || annName.contains("Mapper")) roles.add("DATA_LAYER");
                if (annName.contains("Component")) roles.add("COMPONENT");
                if (annName.contains("Configuration")) roles.add("CONFIG");
                if (annName.contains("Entity") || annName.contains("Table")) roles.add("ENTITY");
            }
        }

        // Naming-based detection
        if (name.endsWith("Controller") || name.endsWith("Handler")) roles.add("WEB_LAYER");
        else if (name.endsWith("Service") || name.endsWith("Biz")) roles.add("BUSINESS_LAYER");
        else if (name.endsWith("Repository") || name.endsWith("DAO") || name.endsWith("Mapper")) roles.add("DATA_LAYER");
        else if (name.endsWith("Entity") || name.endsWith("Model") || name.endsWith("DTO") || name.endsWith("VO")) roles.add("ENTITY");
        else if (name.endsWith("Util") || name.endsWith("Helper")) roles.add("UTILITY");
        else if (name.endsWith("Config") || name.endsWith("Configuration")) roles.add("CONFIG");
        else if (name.endsWith("Exception") || name.endsWith("Error")) roles.add("EXCEPTION");

        return roles;
    }

    // =====================================================================
    // Architecture Tags
    // =====================================================================

    /**
     * Extract architecture tags based on naming patterns and structure.
     */
    public Set<String> extractArchTags(TypeDeclaration<?> type, List<String> fileLines) {
        Set<String> tags = new LinkedHashSet<>();
        String name = type.getNameAsString();

        // Layer tags
        if (name.endsWith("Controller") || name.endsWith("Resource") || name.endsWith("Endpoint")) {
            tags.add("layer:presentation");
        }
        if (name.endsWith("Service") || name.endsWith("Manager") || name.endsWith("Orchestrator")) {
            tags.add("layer:business");
        }
        if (name.endsWith("Repository") || name.endsWith("DAO") || name.endsWith("Mapper")) {
            tags.add("layer:data");
        }
        if (name.endsWith("Entity") || name.endsWith("Model") || name.endsWith("POJO")) {
            tags.add("layer:domain");
        }
        if (name.endsWith("Util") || name.endsWith("Helper")) {
            tags.add("pattern:utility");
        }
        if (name.endsWith("Factory") || name.endsWith("Builder")) {
            tags.add("pattern:creational");
        }
        if (name.endsWith("Strategy") || name.endsWith("Template")) {
            tags.add("pattern:behavioral");
        }
        if (name.endsWith("Adapter") || name.endsWith("Facade")) {
            tags.add("pattern:structural");
        }

        // Complexity hints
        int methodCount = type.getMethods().size();
        if (methodCount > 30) tags.add("complexity:high");
        else if (methodCount > 15) tags.add("complexity:medium");
        else tags.add("complexity:low");

        return tags;
    }

    // =====================================================================
    // Translation & Summarization
    // =====================================================================

    /**
     * Translate and summarize a comment text.
     */
    public String translateAndSummarize(String text) {
        if (text == null || text.isEmpty()) return "";
        // Use translator if available
        if (translator != null) {
            return translator.translateIdentifier(text);
        }
        return text;
    }

    // =====================================================================
    // Domain Context
    // =====================================================================

    /**
     * Extract domain context from package name.
     */
    public String extractDomainContext(String pkg) {
        if (pkg == null || pkg.isEmpty()) return "unknown";
        // Extract meaningful domain parts
        String[] parts = pkg.split("\\.");
        Set<String> domains = new LinkedHashSet<>();
        for (String part : parts) {
            if (part.equals("java") || part.equals("javax") || part.equals("com") ||
                part.equals("org") || part.equals("io") || part.equals("net") ||
                part.equals("util") || part.equals("lang") || part.equals("spring") ||
                part.equals("apache") || part.equals("google") || part.equals("fasterxml")) {
                continue;
            }
            domains.add(part);
        }
        return domains.isEmpty() ? "unknown" : String.join(".", domains);
    }

    // =====================================================================
    // Logical Inference
    // =====================================================================

    /**
     * Perform logical inference on a type based on naming patterns and structure.
     */
    public Map<String, Object> performLogicalInference(TypeDeclaration<?> type, List<String> fileLines) {
        Map<String, Object> results = new LinkedHashMap<>();
        List<Map<String, Object>> evidenceList = new ArrayList<>();
        String name = type.getNameAsString();

        // Singleton inference
        if (type instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) type;
            if (cid.isFinal() && cid.getConstructors().stream().allMatch(c -> c.isPrivate())) {
                Map<String, Object> ev = new LinkedHashMap<>();
                ev.put("inference", "可能为单例模式");
                ev.put("evidence", "final class + 私有构造器");
                evidenceList.add(ev);
            }
        }

        // Builder pattern inference
        if (name.endsWith("Builder") || name.endsWith("Director")) {
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("inference", "构建器模式");
            ev.put("evidence", "类名以 Builder/Director 结尾");
            evidenceList.add(ev);
        }

        // Factory pattern inference
        if (name.endsWith("Factory")) {
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("inference", "工厂模式");
            ev.put("evidence", "类名以 Factory 结尾");
            evidenceList.add(ev);
        }

        // Strategy pattern inference
        if (type instanceof ClassOrInterfaceDeclaration && ((ClassOrInterfaceDeclaration) type).isInterface()) {
            if (name.endsWith("Strategy") || name.endsWith("Policy")) {
                Map<String, Object> ev = new LinkedHashMap<>();
                ev.put("inference", "策略模式");
                ev.put("evidence", "接口名以 Strategy/Policy 结尾");
                evidenceList.add(ev);
            }
        }

        if (!evidenceList.isEmpty()) {
            results.put("inferences", evidenceList);
        }

        return results;
    }

    // =====================================================================
    // AI Guidance
    // =====================================================================

    /**
     * Extract AI teaching guidance for a type.
     */
    public Map<String, Object> extractAIGuidance(TypeDeclaration<?> type, List<String> fileLines) {
        Map<String, Object> guidance = new LinkedHashMap<>();

        // Key concepts
        List<String> concepts = new ArrayList<>();
        String name = type.getNameAsString();
        concepts.add("理解 " + name + " 的职责和边界");
        if (type instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) type;
            if (cid.getExtendedTypes().size() > 0) {
                concepts.add("理解继承层次和重写的方法");
            }
            if (cid.getImplementedTypes().size() > 0) {
                concepts.add("理解实现的接口契约");
            }
        }

        guidance.put("key_concepts", concepts);

        // Constraints
        List<String> constraints = new ArrayList<>();
        constraints.add("修改时需保持向后兼容");
        if (type.getMethods().size() > 20) {
            constraints.add("方法较多，建议拆分职责");
        }
        guidance.put("constraints", constraints);

        // Scenarios
        List<String> scenarios = new ArrayList<>();
        scenarios.add("重构时如何保持依赖方不受影响");
        scenarios.add("如何为新场景扩展此类");
        guidance.put("scenarios", scenarios);

        return guidance;
    }

    // =====================================================================
    // Semantic Profile
    // =====================================================================

    /**
     * Extract semantic profile: inheritance, complexity, method signatures.
     */
    public Map<String, Object> extractSemanticProfile(TypeDeclaration<?> type, List<String> fileLines) {
        Map<String, Object> profile = new LinkedHashMap<>();

        // Inheritance info
        if (type instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) type;
            List<String> extendsList = new ArrayList<>();
            cid.getExtendedTypes().forEach(t -> extendsList.add(t.getNameAsString()));
            profile.put("extends", extendsList);

            List<String> implementsList = new ArrayList<>();
            cid.getImplementedTypes().forEach(t -> implementsList.add(t.getNameAsString()));
            profile.put("implements", implementsList);
        }

        // Method signatures
        List<Map<String, Object>> methodProfiles = new ArrayList<>();
        for (com.github.javaparser.ast.body.MethodDeclaration method : type.getMethods()) {
            Map<String, Object> mp = new LinkedHashMap<>();
            mp.put("name", method.getNameAsString());
            mp.put("return_type", method.getType().asString());
            mp.put("params", method.getParameters().size());
            mp.put("modifiers", method.getModifiers().toString());
            methodProfiles.add(mp);
        }
        profile.put("methods", methodProfiles);

        return profile;
    }

    // =====================================================================
    // Bilingual Tag Resolution
    // =====================================================================

    /**
     * Resolve bilingual tag IDs to display strings.
     */
    public List<Map<String, String>> resolveBilingualTags(Set<String> tagIds) {
        List<Map<String, String>> result = new ArrayList<>();
        for (String tagId : tagIds) {
            Map<String, String> tag = new LinkedHashMap<>();
            tag.put("id", tagId);
            // Look up translation
            String translated = lookupTerm(tagId);
            tag.put("zh", translated != null ? translated : tagId);
            tag.put("en", tagId);
            result.add(tag);
        }
        return result;
    }
}
