package cn.dolphinmind.glossary.java.analyze.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Core Feature 1: Entry Point Discovery (JavaParser-based, not regex)
 *
 * Uses JavaParser AST to accurately find:
 * - main() methods
 * - @RequestMapping / @GetMapping / @PostMapping etc.
 * - @EventListener, @Scheduled, @PostConstruct
 * - @KafkaListener, @JmsListener, @RabbitListener
 */
public class EntryPointDiscovery {

    public enum EntryPointType {
        MAIN_METHOD, REST_CONTROLLER, EVENT_LISTENER,
        SCHEDULED_TASK, POST_CONSTRUCT, MESSAGE_CONSUMER
    }

    public static class EntryPoint {
        private final String className;
        private final String packageName;
        private final String methodName;
        private final String filePath;
        private final EntryPointType type;
        private final String pathOrTopic;
        private final String httpMethod;
        private final int line;

        public EntryPoint(String className, String packageName, String methodName, String filePath,
                          EntryPointType type, String pathOrTopic, String httpMethod, int line) {
            this.className = className;
            this.packageName = packageName;
            this.methodName = methodName;
            this.filePath = filePath;
            this.type = type;
            this.pathOrTopic = pathOrTopic;
            this.httpMethod = httpMethod;
            this.line = line;
        }

        public String getClassName() { return className; }
        public String getPackageName() { return packageName; }
        public String getMethodName() { return methodName; }
        public String getFilePath() { return filePath; }
        public EntryPointType getType() { return type; }
        public String getPathOrTopic() { return pathOrTopic; }
        public String getHttpMethod() { return httpMethod; }
        public int getLine() { return line; }
        public String getFullClassName() {
            return (packageName.isEmpty() ? "" : packageName + ".") + className;
        }

        public String getSummary() {
            switch (type) {
                case REST_CONTROLLER: return httpMethod + " " + pathOrTopic + " → " + getFullClassName() + "#" + methodName;
                case MAIN_METHOD: return getFullClassName() + ".main()";
                case EVENT_LISTENER: return "Event → " + getFullClassName() + "#" + methodName;
                case SCHEDULED_TASK: return "Scheduled → " + getFullClassName() + "#" + methodName + "(" + pathOrTopic + ")";
                case MESSAGE_CONSUMER: return "Message[" + pathOrTopic + "] → " + getFullClassName() + "#" + methodName;
                default: return getFullClassName() + "#" + methodName + " (" + type + ")";
            }
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("class", getFullClassName());
            map.put("method", methodName);
            map.put("file", filePath);
            map.put("type", type.name());
            map.put("path_or_topic", pathOrTopic);
            map.put("http_method", httpMethod);
            map.put("line", line);
            map.put("summary", getSummary());
            return map;
        }
    }

    public List<EntryPoint> discover(Path projectRoot) throws IOException {
        List<EntryPoint> entries = new ArrayList<>();

        Files.walk(projectRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("src"))
                .filter(p -> !p.toString().contains("test") && !p.toString().contains("target"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String packageName = cu.getPackageDeclaration()
                                .map(pd -> pd.getNameAsString()).orElse("");
                        String filePath = projectRoot.relativize(path).toString();

                        for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                            String className = classDecl.getNameAsString();

                            for (MethodDeclaration method : classDecl.getMethods()) {
                                String mName = method.getNameAsString();
                                List<AnnotationExpr> anns = method.getAnnotations();
                                String annStr = anns.toString();
                                int line = method.getBegin().map(p -> p.line).orElse(0);

                                // REST Controller
                                if (hasAnnotation(anns, "RequestMapping", "GetMapping", "PostMapping",
                                        "PutMapping", "DeleteMapping", "PatchMapping")) {
                                    String http = extractHttpMethod(anns);
                                    String urlPath = extractPathValue(anns);
                                    entries.add(new EntryPoint(className, packageName, mName, filePath,
                                            EntryPointType.REST_CONTROLLER, urlPath, http, line));
                                }
                                // Event Listener
                                else if (hasAnnotation(anns, "EventListener")) {
                                    entries.add(new EntryPoint(className, packageName, mName, filePath,
                                            EntryPointType.EVENT_LISTENER, "", "", line));
                                }
                                // Scheduled
                                else if (hasAnnotation(anns, "Scheduled")) {
                                    String cron = extractCron(anns);
                                    entries.add(new EntryPoint(className, packageName, mName, filePath,
                                            EntryPointType.SCHEDULED_TASK, cron, "", line));
                                }
                                // PostConstruct
                                else if (hasAnnotation(anns, "PostConstruct")) {
                                    entries.add(new EntryPoint(className, packageName, mName, filePath,
                                            EntryPointType.POST_CONSTRUCT, "", "", line));
                                }
                                // Message Consumer
                                else if (hasAnnotation(anns, "JmsListener", "KafkaListener", "RabbitListener", "StreamListener")) {
                                    String topic = extractTopic(anns);
                                    entries.add(new EntryPoint(className, packageName, mName, filePath,
                                            EntryPointType.MESSAGE_CONSUMER, topic, "", line));
                                }
                            }

                            // main() method
                            for (MethodDeclaration m : classDecl.getMethods()) {
                                if ("main".equals(m.getNameAsString()) &&
                                    m.isPublic() && m.isStatic() &&
                                    m.getParameters().size() == 1 &&
                                    m.getParameter(0).getTypeAsString().equals("String[]")) {
                                    entries.add(new EntryPoint(className, packageName, "main", filePath,
                                            EntryPointType.MAIN_METHOD, "", "",
                                            m.getBegin().map(p -> p.line).orElse(0)));
                                }
                            }
                        }
                    } catch (Exception e) {
                        // ignore parse errors
                    }
                });

        return entries;
    }

    private boolean hasAnnotation(List<AnnotationExpr> anns, String... names) {
        Set<String> nameSet = new HashSet<>(Arrays.asList(names));
        for (AnnotationExpr ann : anns) {
            if (nameSet.contains(ann.getNameAsString())) return true;
        }
        return false;
    }

    private String extractHttpMethod(List<AnnotationExpr> anns) {
        for (AnnotationExpr ann : anns) {
            String n = ann.getNameAsString();
            if ("GetMapping".equals(n)) return "GET";
            if ("PostMapping".equals(n)) return "POST";
            if ("PutMapping".equals(n)) return "PUT";
            if ("DeleteMapping".equals(n)) return "DELETE";
            if ("PatchMapping".equals(n)) return "PATCH";
            if ("RequestMapping".equals(n)) {
                if (ann instanceof NormalAnnotationExpr) {
                    for (MemberValuePair pair : ((NormalAnnotationExpr) ann).getPairs()) {
                        if ("method".equals(pair.getNameAsString())) {
                            String v = pair.getValue().toString();
                            if (v.contains("GET")) return "GET";
                            if (v.contains("POST")) return "POST";
                            if (v.contains("PUT")) return "PUT";
                            if (v.contains("DELETE")) return "DELETE";
                        }
                    }
                }
                return "ANY";
            }
        }
        return "";
    }

    private String extractPathValue(List<AnnotationExpr> anns) {
        for (AnnotationExpr ann : anns) {
            String n = ann.getNameAsString();
            if (n.endsWith("Mapping")) {
                Expression val = null;
                if (ann instanceof SingleMemberAnnotationExpr) {
                    val = ((SingleMemberAnnotationExpr) ann).getMemberValue();
                } else if (ann instanceof NormalAnnotationExpr) {
                    for (MemberValuePair pair : ((NormalAnnotationExpr) ann).getPairs()) {
                        if ("value".equals(pair.getNameAsString()) || "path".equals(pair.getNameAsString())) {
                            val = pair.getValue();
                            break;
                        }
                    }
                }
                if (val != null) return extractString(val);
            }
        }
        return "";
    }

    private String extractCron(List<AnnotationExpr> anns) {
        for (AnnotationExpr ann : anns) {
            if ("Scheduled".equals(ann.getNameAsString())) {
                Expression val = null;
                if (ann instanceof SingleMemberAnnotationExpr) {
                    val = ((SingleMemberAnnotationExpr) ann).getMemberValue();
                } else if (ann instanceof NormalAnnotationExpr) {
                    for (MemberValuePair pair : ((NormalAnnotationExpr) ann).getPairs()) {
                        if ("cron".equals(pair.getNameAsString())) { val = pair.getValue(); break; }
                    }
                }
                if (val != null) return extractString(val);
            }
        }
        return "";
    }

    private String extractTopic(List<AnnotationExpr> anns) {
        for (AnnotationExpr ann : anns) {
            String n = ann.getNameAsString();
            if (n.contains("Listener")) {
                Expression val = null;
                if (ann instanceof SingleMemberAnnotationExpr) {
                    val = ((SingleMemberAnnotationExpr) ann).getMemberValue();
                } else if (ann instanceof NormalAnnotationExpr) {
                    for (MemberValuePair pair : ((NormalAnnotationExpr) ann).getPairs()) {
                        String pn = pair.getNameAsString();
                        if ("value".equals(pn) || "topic".equals(pn) || "queues".equals(pn)) {
                            val = pair.getValue(); break;
                        }
                    }
                }
                if (val != null) return extractString(val);
            }
        }
        return "";
    }

    private String extractString(Expression expr) {
        if (expr instanceof StringLiteralExpr) return ((StringLiteralExpr) expr).getValue();
        return expr.toString();
    }

    public Map<EntryPointType, List<EntryPoint>> groupByType(List<EntryPoint> entries) {
        Map<EntryPointType, List<EntryPoint>> grouped = new LinkedHashMap<>();
        for (EntryPoint ep : entries) {
            grouped.computeIfAbsent(ep.getType(), k -> new ArrayList<>()).add(ep);
        }
        return grouped;
    }

    public void printSummary(List<EntryPoint> entries) {
        System.out.println("\n=== 入口点发现 (" + entries.size() + " 个) ===");
        groupByType(entries).forEach((type, eps) -> {
            System.out.println("\n【" + type.name() + "】" + eps.size() + " 个");
            for (EntryPoint ep : eps) System.out.println("  " + ep.getSummary());
        });
    }
}
