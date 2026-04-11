package cn.dolphinmind.glossary.java.analyze.spring;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring Framework analyzer: extracts API endpoints and bean wiring from Spring annotations.
 *
 * Supports:
 * - @RestController, @Controller, @RequestMapping, @GetMapping, @PostMapping, etc.
 * - @Autowired, @Inject, @Resource, @Bean, @Component, @Service, @Repository
 */
public class SpringAnalyzer {

    // Spring annotation constants
    private static final Set<String> CONTROLLER_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "RestController", "Controller"
    ));

    private static final Set<String> REQUEST_MAPPING_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "RequestMapping", "GetMapping", "PostMapping", "PutMapping",
            "DeleteMapping", "PatchMapping"
    ));

    private static final Map<String, String> MAPPING_TO_HTTP_METHOD = new HashMap<>();
    static {
        MAPPING_TO_HTTP_METHOD.put("GetMapping", "GET");
        MAPPING_TO_HTTP_METHOD.put("PostMapping", "POST");
        MAPPING_TO_HTTP_METHOD.put("PutMapping", "PUT");
        MAPPING_TO_HTTP_METHOD.put("DeleteMapping", "DELETE");
        MAPPING_TO_HTTP_METHOD.put("PatchMapping", "PATCH");
        MAPPING_TO_HTTP_METHOD.put("RequestMapping", "ANY");
    }

    private static final Set<String> BEAN_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "Component", "Service", "Repository", "Controller", "RestController",
            "Configuration", "Bean"
    ));

    private static final Set<String> INJECT_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "Autowired", "Inject", "Resource", "Value"
    ));

    /**
     * Analyze a project root directory for Spring endpoints and bean dependencies.
     *
     * @param sourceRoot Path to the Java source root
     * @return Map with "endpoints" and "beanDependencies" lists
     */
    public Map<String, Object> analyze(String sourceRoot) throws Exception {
        List<ApiEndpoint> endpoints = new ArrayList<>();
        List<SpringBeanDependency> beanDependencies = new ArrayList<>();
        Set<String> springBeans = new HashSet<>();

        Path root = Paths.get(sourceRoot);

        // Find all Java files
        List<Path> javaFiles = new ArrayList<>();
        try {
            Files.walk(root)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        String s = p.toString();
                        return (s.contains("java" + File.separator) || s.contains("src")) &&
                               !s.contains("test") && !s.contains("target");
                    })
                    .forEach(javaFiles::add);
        } catch (Exception e) {
            System.err.println("⚠️ Error walking source tree: " + e.getMessage());
        }

        System.out.println("🔍 正在分析 Spring 注解: " + javaFiles.size() + " 个文件");

        int fileCount = 0;
        for (Path file : javaFiles) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(file);
                String pkg = cu.getPackageDeclaration()
                        .map(pd -> pd.getNameAsString())
                        .orElse("default");

                // Analyze types
                for (TypeDeclaration<?> type : cu.getTypes()) {
                    if (type instanceof ClassOrInterfaceDeclaration) {
                        ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) type;
                        String className = pkg + "." + clazz.getNameAsString();

                        // Check if it's a Spring bean
                        if (hasAnyAnnotation(clazz, BEAN_ANNOTATIONS)) {
                            springBeans.add(className);
                        }

                        // Check if it's a controller
                        if (hasAnyAnnotation(clazz, CONTROLLER_ANNOTATIONS)) {
                            String classLevelPath = extractRequestMappingPath(clazz);

                            // Analyze methods for endpoints
                            for (MethodDeclaration method : clazz.getMethods()) {
                                ApiEndpoint endpoint = extractEndpoint(method, pkg, className, classLevelPath, file);
                                if (endpoint != null) {
                                    endpoints.add(endpoint);
                                }
                            }
                        }

                        // Analyze bean dependencies
                        List<SpringBeanDependency> deps = extractBeanDependencies(clazz, className, file, pkg);
                        beanDependencies.addAll(deps);
                    }
                }

                fileCount++;
                if (fileCount % 100 == 0) {
                    System.out.println("  📄 已分析 " + fileCount + "/" + javaFiles.size() + " 个文件");
                }
            } catch (Exception e) {
                // Skip files that fail to parse
            }
        }

        System.out.println("✅ Spring 分析完成:");
        System.out.println("  🔗 API 端点: " + endpoints.size());
        System.out.println("  📦 Spring Beans: " + springBeans.size());
        System.out.println("  🔀 Bean 依赖: " + beanDependencies.size());

        // Build result
        Map<String, Object> result = new LinkedHashMap<>();

        // Endpoints
        List<Map<String, Object>> endpointMaps = endpoints.stream()
                .map(ApiEndpoint::toMap)
                .collect(Collectors.toList());
        result.put("endpoints", endpointMaps);

        // Bean dependencies
        List<Map<String, Object>> depMaps = beanDependencies.stream()
                .map(SpringBeanDependency::toMap)
                .collect(Collectors.toList());
        result.put("beanDependencies", depMaps);

        // Spring beans list
        result.put("springBeans", new ArrayList<>(springBeans));

        // Summary
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalEndpoints", endpoints.size());
        summary.put("totalBeans", springBeans.size());
        summary.put("totalBeanDependencies", beanDependencies.size());

        // HTTP method breakdown
        Map<String, Long> methodBreakdown = endpoints.stream()
                .collect(Collectors.groupingBy(ApiEndpoint::getHttpMethod, Collectors.counting()));
        summary.put("httpMethodBreakdown", methodBreakdown);

        result.put("summary", summary);

        return result;
    }

    /**
     * Check if a type has any of the specified annotations.
     */
    private boolean hasAnyAnnotation(NodeWithAnnotations<?> node, Set<String> annotationNames) {
        for (String annName : annotationNames) {
            if (node.getAnnotationByName(annName).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract the path prefix from a class-level @RequestMapping.
     */
    private String extractRequestMappingPath(ClassOrInterfaceDeclaration clazz) {
        // Check @RequestMapping on the class
        Optional<AnnotationExpr> requestMapping = clazz.getAnnotationByName("RequestMapping");
        if (requestMapping.isPresent()) {
            return extractPathFromAnnotation(requestMapping.get());
        }
        return "";
    }

    /**
     * Extract endpoint info from a method.
     */
    private ApiEndpoint extractEndpoint(MethodDeclaration method, String pkg,
            String className, String classPath, Path file) {

        // Find any mapping annotation on the method
        Optional<AnnotationExpr> mappingAnnotation = Optional.empty();
        String httpMethod = null;

        for (String mappingAnn : REQUEST_MAPPING_ANNOTATIONS) {
            Optional<AnnotationExpr> ann = method.getAnnotationByName(mappingAnn);
            if (ann.isPresent()) {
                mappingAnnotation = ann;
                httpMethod = MAPPING_TO_HTTP_METHOD.getOrDefault(mappingAnn, "ANY");
                break;
            }
        }

        if (!mappingAnnotation.isPresent()) {
            return null; // Not an endpoint
        }

        ApiEndpoint endpoint = new ApiEndpoint();
        endpoint.setClassName(className);
        endpoint.setMethodName(method.getNameAsString());
        endpoint.setAddress(className + "#" + method.getNameAsString() + formatParams(method.getParameters()));
        endpoint.setHttpMethod(httpMethod);
        endpoint.setLine(method.getRange().map(r -> r.begin.line).orElse(0));
        endpoint.setModule("");

        // Extract path
        String methodPath = extractPathFromAnnotation(mappingAnnotation.get());
        String fullPath = joinPaths(classPath, methodPath);
        endpoint.setPath(fullPath.isEmpty() ? "/" : fullPath);

        // Extract produces/consumes
        endpoint.setProduces(extractAttributeValue(mappingAnnotation.get(), "produces"));
        endpoint.setConsumes(extractAttributeValue(mappingAnnotation.get(), "consumes"));

        // Extract description from Javadoc
        method.getJavadoc().ifPresent(javadoc -> {
            String summary = javadoc.getDescription().toText();
            if (!summary.isEmpty()) {
                endpoint.setDescription(summary.split("\\.")[0].trim());
            }
        });

        // Extract parameters
        List<String> params = new ArrayList<>();
        method.getParameters().forEach(p -> {
            String paramDesc = p.getNameAsString() + ": " + p.getType().asString();
            // Check for @PathVariable, @RequestParam, @RequestBody
            if (p.getAnnotationByName("PathVariable").isPresent()) {
                paramDesc += " (@PathVariable)";
            } else if (p.getAnnotationByName("RequestParam").isPresent()) {
                paramDesc += " (@RequestParam)";
            } else if (p.getAnnotationByName("RequestBody").isPresent()) {
                paramDesc += " (@RequestBody)";
            }
            params.add(paramDesc);
        });
        endpoint.setParameters(params);

        // Extract return type
        endpoint.setReturnType(method.getType().asString());

        return endpoint;
    }

    /**
     * Extract bean dependencies from a class (field injection, constructor injection, setter injection).
     */
    private List<SpringBeanDependency> extractBeanDependencies(ClassOrInterfaceDeclaration clazz,
            String className, Path file, String pkg) {
        List<SpringBeanDependency> deps = new ArrayList<>();

        // Field injection: @Autowired, @Inject, @Resource on fields
        for (FieldDeclaration field : clazz.getFields()) {
            if (hasAnyAnnotation(field, INJECT_ANNOTATIONS)) {
                String fieldType = field.getElementType().asString();
                String fieldName = field.getVariables().get(0).getNameAsString();

                SpringBeanDependency dep = new SpringBeanDependency();
                dep.setSourceBean(className);
                dep.setTargetBean(fieldType);
                dep.setInjectionType("FIELD");
                dep.setFieldName(fieldName);
                dep.setLine(field.getRange().map(r -> r.begin.line).orElse(0));
                dep.setModule("");
                deps.add(dep);
            }
        }

        // Constructor injection: parameters annotated with @Autowired or @Qualifier
        for (ConstructorDeclaration ctor : clazz.getConstructors()) {
            for (com.github.javaparser.ast.body.Parameter param : ctor.getParameters()) {
                if (hasAnyAnnotation(param, INJECT_ANNOTATIONS) || param.getAnnotationByName("Qualifier").isPresent()) {
                    String paramType = param.getType().asString();

                    SpringBeanDependency dep = new SpringBeanDependency();
                    dep.setSourceBean(className);
                    dep.setTargetBean(paramType);
                    dep.setInjectionType("CONSTRUCTOR");
                    dep.setFieldName(param.getNameAsString());
                    dep.setLine(param.getRange().map(r -> r.begin.line).orElse(0));
                    dep.setModule("");
                    deps.add(dep);
                }
            }
        }

        // Method injection: @Autowired on setter methods
        for (MethodDeclaration method : clazz.getMethods()) {
            if (hasAnyAnnotation(method, INJECT_ANNOTATIONS) && method.getParameters().size() > 0) {
                com.github.javaparser.ast.body.Parameter param = method.getParameter(0);
                String paramType = param.getType().asString();

                SpringBeanDependency dep = new SpringBeanDependency();
                dep.setSourceBean(className);
                dep.setTargetBean(paramType);
                dep.setInjectionType("METHOD");
                dep.setFieldName(param.getNameAsString());
                dep.setLine(method.getRange().map(r -> r.begin.line).orElse(0));
                dep.setModule("");
                deps.add(dep);
            }
        }

        return deps;
    }

    /**
     * Extract path value from a mapping annotation.
     */
    private String extractPathFromAnnotation(AnnotationExpr annotation) {
        // Handle @RequestMapping(value = "/path") or @GetMapping("/path")
        for (MemberValuePair pair : annotation.getChildNodesByType(MemberValuePair.class)) {
            if (pair.getNameAsString().equals("value") || pair.getNameAsString().equals("path")) {
                return extractStringValue(pair.getValue());
            }
        }

        // Handle @GetMapping("/path") - single string value without key
        for (Expression arg : annotation.getChildNodesByType(Expression.class)) {
            if (arg instanceof StringLiteralExpr || arg instanceof SingleMemberAnnotationExpr) {
                return extractStringValue(arg);
            }
        }

        return "";
    }

    /**
     * Extract string value from an expression.
     */
    private String extractStringValue(Expression expr) {
        if (expr instanceof StringLiteralExpr) {
            return ((StringLiteralExpr) expr).getValue();
        }
        if (expr instanceof SingleMemberAnnotationExpr) {
            return extractStringValue(((SingleMemberAnnotationExpr) expr).getMemberValue());
        }
        if (expr instanceof ArrayInitializerExpr) {
            ArrayInitializerExpr array = (ArrayInitializerExpr) expr;
            if (array.getValues().size() > 0) {
                return extractStringValue(array.getValues().get(0));
            }
        }
        return "";
    }

    /**
     * Extract a specific attribute value from an annotation.
     */
    private String extractAttributeValue(AnnotationExpr annotation, String attributeName) {
        for (MemberValuePair pair : annotation.getChildNodesByType(MemberValuePair.class)) {
            if (pair.getNameAsString().equals(attributeName)) {
                return extractStringValue(pair.getValue());
            }
        }
        return "";
    }

    /**
     * Join two path segments, handling leading/trailing slashes.
     */
    private String joinPaths(String... segments) {
        StringBuilder sb = new StringBuilder();
        for (String segment : segments) {
            if (segment == null || segment.isEmpty()) continue;
            if (!segment.startsWith("/")) {
                sb.append("/");
            }
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '/' && segment.startsWith("/")) {
                sb.append(segment.substring(1));
            } else {
                sb.append(segment);
            }
        }
        return sb.toString();
    }

    /**
     * Format method parameters for address string.
     */
    private String formatParams(java.util.List<com.github.javaparser.ast.body.Parameter> params) {
        if (params.isEmpty()) return "()";
        String joined = params.stream()
                .map(p -> p.getType().asString())
                .collect(Collectors.joining(","));
        return "(" + joined + ")";
    }
}
