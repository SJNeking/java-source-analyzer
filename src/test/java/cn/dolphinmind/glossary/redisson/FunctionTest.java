package cn.dolphinmind.glossary.redisson;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author mingxilv
 * @date 2026/04/04
 * @description S1-S10 最终闭环：集成物理注释注入、解剖流打印、兼容 Java 8
 */
public class FunctionTest {

    private static final String SRC_ROOT = "/Users/mingxilv/WebDevelopment/gitcode/dev-proj/framework_source/redisson/redisson/src/main/java";
    private static final String OUTPUT_PATH = "src/test/resources/Redisson_Step10_Matrix.json";

    // 统计计数器
    private final AtomicInteger classCount = new AtomicInteger(0);
    private final AtomicInteger methodCount = new AtomicInteger(0);
    private final AtomicInteger fieldCount = new AtomicInteger(0);
    private final AtomicInteger commentFound = new AtomicInteger(0);

    @Before
    public void setup() {
        CombinedTypeSolver solver = new CombinedTypeSolver();
        solver.add(new JavaParserTypeSolver(new File(SRC_ROOT)));
        solver.add(new ReflectionTypeSolver());
        StaticJavaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(solver));

        printLine('=', 80);
        System.out.println("🚀 REDISSON ASSET DECODER | 数字化资产解剖引擎启动");
        printLine('=', 80);
    }

    @Test
    public void executeUltimateScan() throws Exception {
        // 1. 提取版本号（从 pom.xml 自动抓取）
        String version = extractVersion();
        System.out.println("📦 识别到 Redisson 版本: " + version);

        // 2. 创建根容器，注入元数据
        Map<String, Object> rootContainer = new LinkedHashMap<>();
        rootContainer.put("framework", "Redisson");
        rootContainer.put("version", version);
        rootContainer.put("scan_date", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        // 原来的资产列表现在是 rootContainer 的一个字段
        List<Map<String, Object>> digitalAssets = new ArrayList<>();

        Files.walk(Paths.get(SRC_ROOT + "/org/redisson"))
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        List<String> fileLines = Files.readAllLines(path, StandardCharsets.UTF_8);
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("default");

                        cu.getTypes().forEach(type -> {
                            classCount.incrementAndGet();
                            Map<String, Object> classAsset = new LinkedHashMap<>();
                            String className = type.getNameAsString();
                            String classAddress = pkg + "." + className;
                            String description = bruteForceComment(fileLines, type);

                            // --- 实时打印层 ---
                            if (!description.isEmpty()) commentFound.incrementAndGet();
                            System.out.printf("[%04d] 💎 %-35s | Doc: %s\n",
                                    classCount.get(),
                                    className.length() > 35 ? ".." + className.substring(className.length()-33) : className,
                                    description.isEmpty() ? "\033[31m[EMPTY]\033[0m" : "\033[32m[CAPTURED]\033[0m"
                            );

                            classAsset.put("address", classAddress);
                            classAsset.put("description", description);
                            classAsset.put("kind", resolveTypeKind(type));
                            classAsset.put("modifiers", resolveMods(type.getModifiers()));
                            classAsset.put("class_generics", resolveTypeParameters(type));

                            if (type instanceof ClassOrInterfaceDeclaration) {
                                classAsset.put("hierarchy", resolveHierarchySemantic((ClassOrInterfaceDeclaration) type));
                            }

                            classAsset.put("fields_matrix", resolveFieldsMatrix(fileLines, type));
                            classAsset.put("constructor_matrix", resolveConstructorsAligned(fileLines, type, classAddress));
                            classAsset.put("method_matrix", resolveMethodsAligned(fileLines, type, classAddress));

                            digitalAssets.add(classAsset);
                        });
                    } catch (Exception ignored) {}
                });

        // 3. 组装并保存
        rootContainer.put("assets", digitalAssets);
        saveMatrix(rootContainer); // 注意这里改为保存 rootContainer
        printReport();
    }

    /**
     * 自动从 Redisson 项目根目录的 pom.xml 抓取版本号
     */
    private String extractVersion() {
        try {
            // 获取当前源码对应的 pom 路径
            Path currentPom = Paths.get(SRC_ROOT).getParent().getParent().getParent().resolve("pom.xml");
            // 获取项目根目录的 pom 路径 (通常在 redisson/pom.xml)
            Path rootPom = currentPom.getParent().getParent().resolve("pom.xml");

            // 优先尝试根目录，因为那里的版本最准
            String version = fetchVersionFromFile(rootPom);
            if (version == null || version.contains("$")) {
                version = fetchVersionFromFile(currentPom);
            }

            return (version != null) ? version : "3.x.x-manual";
        } catch (Exception e) {
            return "3.x.x-fallback";
        }
    }

    private String fetchVersionFromFile(Path path) throws Exception {
        if (!Files.exists(path)) return null;

        String content = new String(Files.readAllLines(path, StandardCharsets.UTF_8).toString());
        // 重点：我们只匹配数字开头的版本号，过滤掉 ${project.version} 这种干扰项
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("<version>(\\d+\\.\\d+\\..*?)</version>");
        java.util.regex.Matcher m = p.matcher(content);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    // ======================== 辅助打印 ========================
    private void printLine(char c, int count) {
        for (int i = 0; i < count; i++) System.out.print(c);
        System.out.println();
    }

    private void printReport() {
        printLine('=', 80);
        System.out.println("📊 资产解剖报告 (Summary Report)");
        printLine('-', 80);
        System.out.printf("🏛️  类资产总计: %d\n", classCount.get());
        System.out.printf("⚙️  行为(方法): %d\n", methodCount.get());
        System.out.printf("📦 状态(字段): %d\n", fieldCount.get());
        System.out.printf("📝 标注覆盖率: %.2f%%\n", (commentFound.get() * 100.0 / classCount.get()));
        printLine('-', 80);
        System.out.println("💾 矩阵文件已更新: " + OUTPUT_PATH);
        printLine('=', 80);
    }

    // ======================== S1-S10 核心引擎 ========================

    private String bruteForceComment(List<String> lines, Node node) {
        return node.getBegin().map(begin -> {
            int currentLine = begin.line - 1;
            StringBuilder sb = new StringBuilder();
            boolean foundEnd = false;
            for (int i = currentLine - 1; i >= 0 && (currentLine - i) < 15; i--) {
                String line = lines.get(i).trim();
                if (line.startsWith("@") && !line.contains("/*")) continue;
                if (line.isEmpty()) continue;
                if (line.endsWith("*/")) foundEnd = true;
                if (foundEnd) {
                    sb.insert(0, line + " ");
                    if (line.startsWith("/**") || line.startsWith("/*")) break;
                }
            }
            return cleanText(sb.toString());
        }).orElse("");
    }

    private String cleanText(String text) {
        return text.replaceAll("/\\*\\*|\\*/|\\*|/\\*", "")
                .replaceAll("(?m)^\\s*@.*$", "")
                .replaceAll("<[^>]*>", "")
                .replaceAll("\\s+", " ").trim();
    }

    private List<Map<String, Object>> resolveFieldsMatrix(List<String> lines, TypeDeclaration<?> t) {
        List<Map<String, Object>> fields = new ArrayList<>();
        t.getFields().forEach(f -> {
            fieldCount.incrementAndGet();
            String desc = bruteForceComment(lines, f);
            List<String> mods = resolveMods(f.getModifiers());
            f.getVariables().forEach(v -> {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("name", v.getNameAsString());
                node.put("description", desc);
                node.put("type_path", getSemanticPath(v.getType()));
                node.put("modifiers", mods);
                fields.add(node);
            });
        });
        return fields;
    }

    private List<Map<String, Object>> resolveMethodsAligned(List<String> lines, TypeDeclaration<?> t, String addr) {
        return t.getMethods().stream().map(m -> {
            methodCount.incrementAndGet();
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("address", addr + "#" + m.getNameAsString());
            node.put("description", bruteForceComment(lines, m));
            node.put("modifiers", resolveMods(m.getModifiers()));
            node.put("is_override", checkIsOverride(m));
            node.put("method_generics", m.getTypeParameters().stream().map(tp -> tp.asString()).collect(Collectors.toList()));
            node.put("return_type_path", getSemanticPath(m.getType()));
            node.put("throws_matrix", m.getThrownExceptions().stream().map(this::getSemanticPath).collect(Collectors.toList()));
            node.put("parameters_inventory", resolveParametersInventory(m.getParameters()));
            return node;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> resolveConstructorsAligned(List<String> lines, TypeDeclaration<?> t, String addr) {
        return t.getConstructors().stream().map(c -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("address", addr + "#<init>_" + System.identityHashCode(c));
            node.put("description", bruteForceComment(lines, c));
            node.put("parameters_inventory", resolveParametersInventory(c.getParameters()));
            return node;
        }).collect(Collectors.toList());
    }

    private List<Map<String, String>> resolveParametersInventory(com.github.javaparser.ast.NodeList<Parameter> parameters) {
        return parameters.stream().map(p -> {
            Map<String, String> pMap = new LinkedHashMap<>();
            pMap.put("name", p.getNameAsString());
            pMap.put("type_path", getSemanticPath(p.getType()));
            return pMap;
        }).collect(Collectors.toList());
    }

    private String getSemanticPath(Type type) {
        try { return type.resolve().describe(); } catch (Exception e) { return type.asString(); }
    }

    private boolean checkIsOverride(MethodDeclaration m) {
        try { return m.resolve().getQualifiedSignature().contains("Override") || m.getAnnotationByClass(Override.class).isPresent(); }
        catch (Exception e) { return m.getAnnotationByClass(Override.class).isPresent(); }
    }

    private Map<String, List<String>> resolveHierarchySemantic(ClassOrInterfaceDeclaration cid) {
        Map<String, List<String>> h = new LinkedHashMap<>();
        h.put("extends", cid.getExtendedTypes().stream().map(this::getSemanticPath).collect(Collectors.toList()));
        h.put("implements", cid.getImplementedTypes().stream().map(this::getSemanticPath).collect(Collectors.toList()));
        return h;
    }

    private List<String> resolveMods(com.github.javaparser.ast.NodeList<com.github.javaparser.ast.Modifier> modifiers) {
        return modifiers.stream().map(m -> m.getKeyword().asString()).collect(Collectors.toList());
    }

    private String resolveTypeKind(TypeDeclaration<?> t) {
        if (t instanceof ClassOrInterfaceDeclaration) return ((ClassOrInterfaceDeclaration) t).isInterface() ? "INTERFACE" : "CLASS";
        return t.getClass().getSimpleName().replace("Declaration", "").toUpperCase();
    }

    private List<String> resolveTypeParameters(TypeDeclaration<?> t) {
        if (t instanceof ClassOrInterfaceDeclaration) {
            return ((ClassOrInterfaceDeclaration) t).getTypeParameters().stream()
                    .map(tp -> tp.asString()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private void saveMatrix(Object data) throws Exception {
        new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(data, new FileWriter(new File(OUTPUT_PATH)));
    }
}