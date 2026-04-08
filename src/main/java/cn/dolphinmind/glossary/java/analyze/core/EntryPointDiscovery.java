package cn.dolphinmind.glossary.java.analyze.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.NodeList;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Core Feature 1: Entry Point Discovery
 *
 * Automatically finds all entry points in a Java project:
 * - main() methods
 * - @RequestMapping / @GetMapping / @PostMapping / etc.
 * - @EventListener
 * - @Scheduled
 * - @PostConstruct
 * - @Scheduled
 * - JMS/Kafka consumers
 * - Quartz jobs
 *
 * This is the starting point for understanding any Java project.
 */
public class EntryPointDiscovery {

    public enum EntryPointType {
        MAIN_METHOD,           // public static void main(String[])
        REST_CONTROLLER,       // @RequestMapping + method
        WEB_SOCKET,            // @OnMessage
        EVENT_LISTENER,        // @EventListener
        SCHEDULED_TASK,        // @Scheduled
        POST_CONSTRUCT,        // @PostConstruct
        MESSAGE_CONSUMER,      // @JmsListener / @KafkaListener / @RabbitListener
        COMMAND_HANDLER,       // @CommandHandler
        SERVLET,               // HttpServlet.doGet/doPost
        JSP_SERVLET,           // extends HttpServlet
        FILTER,                // implements Filter
        STARTUP_RUNNER,        // ApplicationRunner / CommandLineRunner
        UNKNOWN
    }

    public static class EntryPoint {
        private final String className;
        private final String methodName;
        private final String filePath;
        private final EntryPointType type;
        private final String pathOrTopic;  // URL path, topic name, etc.
        private final String httpMethod;   // GET, POST, etc. (for REST)
        private final int line;

        public EntryPoint(String className, String methodName, String filePath,
                          EntryPointType type, String pathOrTopic, String httpMethod, int line) {
            this.className = className;
            this.methodName = methodName;
            this.filePath = filePath;
            this.type = type;
            this.pathOrTopic = pathOrTopic;
            this.httpMethod = httpMethod;
            this.line = line;
        }

        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public String getFilePath() { return filePath; }
        public EntryPointType getType() { return type; }
        public String getPathOrTopic() { return pathOrTopic; }
        public String getHttpMethod() { return httpMethod; }
        public int getLine() { return line; }

        public String getSummary() {
            switch (type) {
                case REST_CONTROLLER:
                    return httpMethod + " " + pathOrTopic + " → " + className + "#" + methodName;
                case MAIN_METHOD:
                    return className + ".main()";
                case EVENT_LISTENER:
                    return "Event → " + className + "#" + methodName;
                case SCHEDULED_TASK:
                    return "Scheduled → " + className + "#" + methodName + "(" + pathOrTopic + ")";
                case MESSAGE_CONSUMER:
                    return "Message[" + pathOrTopic + "] → " + className + "#" + methodName;
                default:
                    return className + "#" + methodName + " (" + type + ")";
            }
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("class", className);
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

    /**
     * Discover all entry points in a project.
     */
    public List<EntryPoint> discover(Path projectRoot) throws IOException {
        List<EntryPoint> entries = new ArrayList<>();

        Files.walk(projectRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("src"))
                .filter(p -> !p.toString().contains("test"))
                .filter(p -> !p.toString().contains("target"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String className = extractClassName(cu);
                        String filePath = projectRoot.relativize(path).toString();

                        cu.findAll(MethodDeclaration.class).forEach(method -> {
                            List<AnnotationExpr> annotations = method.getAnnotations();
                            String methodSig = annotations.toString();

                            // REST Controller
                            if (methodSig.contains("RequestMapping") || methodSig.contains("GetMapping") ||
                                methodSig.contains("PostMapping") || methodSig.contains("PutMapping") ||
                                methodSig.contains("DeleteMapping") || methodSig.contains("PatchMapping")) {
                                String httpMethod = extractHttpMethod(annotations);
                                String urlPath = extractRequestMappingPath(annotations);
                                entries.add(new EntryPoint(className, method.getNameAsString(), filePath,
                                        EntryPointType.REST_CONTROLLER, urlPath, httpMethod,
                                        method.getBegin().map(p -> p.line).orElse(0)));
                            }
                            // Event Listener
                            else if (methodSig.contains("EventListener")) {
                                entries.add(new EntryPoint(className, method.getNameAsString(), filePath,
                                        EntryPointType.EVENT_LISTENER, "", "",
                                        method.getBegin().map(p -> p.line).orElse(0)));
                            }
                            // Scheduled Task
                            else if (methodSig.contains("Scheduled")) {
                                String cron = extractCronExpression(annotations);
                                entries.add(new EntryPoint(className, method.getNameAsString(), filePath,
                                        EntryPointType.SCHEDULED_TASK, cron, "",
                                        method.getBegin().map(p -> p.line).orElse(0)));
                            }
                            // PostConstruct
                            else if (methodSig.contains("PostConstruct")) {
                                entries.add(new EntryPoint(className, method.getNameAsString(), filePath,
                                        EntryPointType.POST_CONSTRUCT, "", "",
                                        method.getBegin().map(p -> p.line).orElse(0)));
                            }
                            // Message Consumer
                            else if (methodSig.contains("JmsListener") || methodSig.contains("KafkaListener") ||
                                     methodSig.contains("RabbitListener") || methodSig.contains("StreamListener")) {
                                String topic = extractTopic(annotations);
                                entries.add(new EntryPoint(className, method.getNameAsString(), filePath,
                                        EntryPointType.MESSAGE_CONSUMER, topic, "",
                                        method.getBegin().map(p -> p.line).orElse(0)));
                            }
                        });

                        // main() method
                        cu.findAll(MethodDeclaration.class).stream()
                                .filter(m -> m.getNameAsString().equals("main"))
                                .filter(m -> m.isPublic() && m.isStatic())
                                .filter(m -> m.getParameters().size() == 1)
                                .forEach(m -> entries.add(new EntryPoint(className, "main", filePath,
                                        EntryPointType.MAIN_METHOD, "", "",
                                        m.getBegin().map(p -> p.line).orElse(0))));

                    } catch (Exception e) {
                        // ignore parse errors
                    }
                });

        return entries;
    }

    private String extractClassName(CompilationUnit cu) {
        return cu.getPrimaryTypeName().orElse("Unknown");
    }

    private String extractHttpMethod(List<AnnotationExpr> annotations) {
        for (AnnotationExpr ann : annotations) {
            String name = ann.getNameAsString();
            if ("GetMapping".equals(name)) return "GET";
            if ("PostMapping".equals(name)) return "POST";
            if ("PutMapping".equals(name)) return "PUT";
            if ("DeleteMapping".equals(name)) return "DELETE";
            if ("PatchMapping".equals(name)) return "PATCH";
            if ("RequestMapping".equals(name)) {
                // Try to extract method from @RequestMapping(method = RequestMethod.GET)
                if (ann instanceof NormalAnnotationExpr) {
                    NodeList<MemberValuePair> pairs = ((NormalAnnotationExpr) ann).getPairs();
                    for (MemberValuePair pair : pairs) {
                        if ("method".equals(pair.getNameAsString())) {
                            String val = pair.getValue().toString();
                            if (val.contains("GET")) return "GET";
                            if (val.contains("POST")) return "POST";
                            if (val.contains("PUT")) return "PUT";
                            if (val.contains("DELETE")) return "DELETE";
                        }
                    }
                }
                return "ANY";
            }
        }
        return "";
    }

    private String extractRequestMappingPath(List<AnnotationExpr> annotations) {
        for (AnnotationExpr ann : annotations) {
            String name = ann.getNameAsString();
            if (name.endsWith("Mapping")) {
                if (ann instanceof NormalAnnotationExpr) {
                    NodeList<MemberValuePair> pairs = ((NormalAnnotationExpr) ann).getPairs();
                    for (MemberValuePair pair : pairs) {
                        if ("value".equals(pair.getNameAsString()) || "path".equals(pair.getNameAsString())) {
                            return extractStringLiteral(pair.getValue());
                        }
                    }
                } else if (ann instanceof SingleMemberAnnotationExpr) {
                    return extractStringLiteral(((SingleMemberAnnotationExpr) ann).getMemberValue());
                }
            }
        }
        return "";
    }

    private String extractCronExpression(List<AnnotationExpr> annotations) {
        for (AnnotationExpr ann : annotations) {
            if ("Scheduled".equals(ann.getNameAsString())) {
                if (ann instanceof SingleMemberAnnotationExpr) {
                    return extractStringLiteral(((SingleMemberAnnotationExpr) ann).getMemberValue());
                } else if (ann instanceof NormalAnnotationExpr) {
                    NodeList<MemberValuePair> pairs = ((NormalAnnotationExpr) ann).getPairs();
                    for (MemberValuePair pair : pairs) {
                        if ("cron".equals(pair.getNameAsString())) {
                            return extractStringLiteral(pair.getValue());
                        }
                    }
                }
            }
        }
        return "";
    }

    private String extractTopic(List<AnnotationExpr> annotations) {
        for (AnnotationExpr ann : annotations) {
            String name = ann.getNameAsString();
            if (name.contains("Listener")) {
                if (ann instanceof SingleMemberAnnotationExpr) {
                    return extractStringLiteral(((SingleMemberAnnotationExpr) ann).getMemberValue());
                } else if (ann instanceof NormalAnnotationExpr) {
                    NodeList<MemberValuePair> pairs = ((NormalAnnotationExpr) ann).getPairs();
                    for (MemberValuePair pair : pairs) {
                        if ("value".equals(pair.getNameAsString()) || "topic".equals(pair.getNameAsString()) ||
                            "queues".equals(pair.getNameAsString())) {
                            return extractStringLiteral(pair.getValue());
                        }
                    }
                }
            }
        }
        return "";
    }

    private String extractStringLiteral(Expression expr) {
        if (expr instanceof StringLiteralExpr) {
            return ((StringLiteralExpr) expr).getValue();
        }
        if (expr instanceof FieldAccessExpr) {
            return expr.toString();
        }
        return expr.toString();
    }

    /**
     * Group entry points by type for summary.
     */
    public Map<EntryPointType, List<EntryPoint>> groupByType(List<EntryPoint> entries) {
        Map<EntryPointType, List<EntryPoint>> grouped = new LinkedHashMap<>();
        for (EntryPoint ep : entries) {
            grouped.computeIfAbsent(ep.getType(), k -> new ArrayList<>()).add(ep);
        }
        return grouped;
    }

    /**
     * Print entry points in a readable format.
     */
    public void printSummary(List<EntryPoint> entries) {
        Map<EntryPointType, List<EntryPoint>> grouped = groupByType(entries);
        System.out.println("\n=== 项目入口点发现 ===");
        System.out.println("共发现 " + entries.size() + " 个入口点\n");

        for (Map.Entry<EntryPointType, List<EntryPoint>> group : grouped.entrySet()) {
            System.out.println("【" + group.getKey().name() + "】(" + group.getValue().size() + " 个)");
            for (EntryPoint ep : group.getValue()) {
                System.out.println("  " + ep.getSummary());
            }
            System.out.println();
        }
    }
}
