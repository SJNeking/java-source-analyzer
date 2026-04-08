package cn.dolphinmind.glossary.java.analyze;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Java框架拆解
 */
@DisplayName("Redisson 源码数字化任务矩阵")

/**
 * @author mingxilv
 * @date 2026/4/4
 * @description 组件外部拆分
 */

public class ComponentTest {

    private static final String SRC_ROOT = "/Users/mingxilv/WebDevelopment/gitcode/dev-proj/framework_source/redisson/redisson/src/main/java/org/redisson";
    private static String FILE_NAME = "Redisson_Step1_Nodes.json";


    /** step-01
     * 维度 (Kind)	架构角色	识别逻辑 (MVP 代码实现)
     * INTERFACE	协议层：定义“做什么”，不涉及实现。	cid.isInterface()
     * ABSTRACT_CLASS	模板层：承载公共逻辑，作为具体实现的基石。	cid.isAbstract()
     * CLASS	执行层：具体的业务逻辑实现，可实例化。	!cid.isInterface() && !cid.isAbstract()
     * ENUM	状态层：定义有限的状态机、配置项或常量池。	t.isEnumDeclaration()
     * ANNOTATION	元数据层：标记组件特性（如异步、实体映射）。	t.isAnnotationDeclaration()
     * 📊 === Redisson 架构组件分布报告 ===
     * 总计组件数: 1446
     * --------------------------------
     * [ABSTRACT_CLASS]: 39 个 (2.70%)
     * [ENUM]: 45 个 (3.11%)
     * [INTERFACE]: 560 个 (38.73%)
     * [ANNOTATION]: 10 个 (0.69%)
     * [CLASS]: 792 个 (54.77%)
     * @throws Exception
     */
    @Test
    public void scanBasicNodes() throws Exception {

        FILE_NAME = "Redisson_Step1_Nodes.json";
        // 使用辅助函数获取输出目标
        File outputFile = getTestResourceFile(FILE_NAME);
        List<Map<String, String>> nodeList = new ArrayList<>();

        Files.walk(Paths.get(SRC_ROOT))
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String packageName = cu.getPackageDeclaration()
                                .map(pd -> pd.getNameAsString()).orElse("default");

                        cu.getTypes().forEach(type -> {
                            Map<String, String> node = new LinkedHashMap<>();
                            node.put("address", packageName + "." + type.getNameAsString());
                            node.put("kind", getKind(type));
                            nodeList.add(node);
                        });
                    } catch (Exception ignored) {}
                });

        saveToJson(nodeList, outputFile);
        System.out.println("✅ 扫描完成，资产已存至: " + outputFile.getPath());

        analyzeNodeDistribution();
    }

    @DisplayName("统计：分析已扫描节点的类型分布 (Java 8 兼容版)")
    public void analyzeNodeDistribution() throws Exception {
        File inputFile = getTestResourceFile(FILE_NAME);
        if (!inputFile.exists()) {
            System.err.println("❌ 错误：找不到基础节点文件，请先运行 scanBasicNodes()");
            return;
        }

        // --- 适配 Java 8 的读取逻辑 ---
        // 使用 readAllBytes 并转换为 String
        byte[] bytes = Files.readAllBytes(inputFile.toPath());
        String jsonContent = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        // 后续逻辑保持不变
        Gson gson = new Gson();
        // 注意：GSON 反序列化泛型需要配合 TypeToken，或者直接转为 List
        List<Map<String, String>> nodes = gson.fromJson(jsonContent, List.class);

        Map<String, Long> distribution = nodes.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        node -> node.get("kind"),
                        java.util.stream.Collectors.counting()
                ));

        System.out.println("\n📊 === Redisson 架构组件分布报告 ===");
        System.out.println("总计组件数: " + nodes.size());
        System.out.println("--------------------------------");
        distribution.forEach((kind, count) -> {
            double percentage = (count.doubleValue() / nodes.size()) * 100;
            System.out.printf("[%s]: %d 个 (%.2f%%)\n", kind, count, percentage);
        });
    }




    private File getTestResourceFile(String fileName) throws IOException {
        Path resourcePath = Paths.get("src", "test", "resources");
        if (!Files.exists(resourcePath)) {
            Files.createDirectories(resourcePath);
        }
        return resourcePath.resolve(fileName).toFile();
    }
    private String getKind(TypeDeclaration<?> t) {
        // 1. 核心判断：类与接口
        if (t instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) t;
            if (cid.isInterface()) {
                return "INTERFACE"; // 纯协议
            }
            // 🚀 核心改进：通过修饰符判断是否为抽象类
            if (cid.isAbstract()) {
                return "ABSTRACT_CLASS"; // 模板/基类
            }
            return "CLASS"; // 具体实现类
        }

        // 2. 状态：枚举
        if (t.isEnumDeclaration()) {
            return "ENUM";
        }

        // 3. 元数据：注解声明
        if (t.isAnnotationDeclaration()) {
            return "ANNOTATION";
        }

        // 4. 现代特性：Record (Java 16+)
        if (t.getClass().getSimpleName().equals("RecordDeclaration")) {
            return "RECORD";
        }

        // 5. 无法识别的顶级结构
        return "UNKNOWN";
    }

    private void saveToJson(Object data, File file) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        }
    }


    /**
     * step-02
     * 🧬 === Redisson 泛型组件资产报告 ===
     * 总组件数: 1446
     * 带有泛型的组件数: 451
     * 泛型普及率: 31.19%
     * --------------------------------
     * 📑 典型泛型组件抽样 (Top 10):
     *  - [CLASS] org.redisson.RedissonMultimapCacheNative [K]
     *  - [CLASS] org.redisson.misc.CompositeIterable [T]
     *  - [CLASS] org.redisson.misc.Tuple [T1, T2]
     *  - [CLASS] org.redisson.misc.CompositeAsyncIterator [T]
     *  - [CLASS] org.redisson.misc.IdentityValue [T]
     *  - [CLASS] org.redisson.misc.CompositeIterator [T]
     *  - [CLASS] org.redisson.misc.FastRemovalQueue [E]
     *  - [CLASS] org.redisson.misc.CompletableFutureWrapper [V]
     *  - [CLASS] org.redisson.RedissonMultimapCache [K]
     *  - [CLASS] org.redisson.transaction.operation.bucket.BucketSetOperation [V]
     * @throws Exception
     */
    @Test
    @DisplayName("Step-02: 扫描泛型元数据（不破坏 Step 1 资产）")
    public void scanNodesWithGenerics() throws Exception {
        String STEP2_FILE = "Redisson_Step2_1_Generics.json";
        File outputFile = getTestResourceFile(STEP2_FILE);

        // 使用 Object 以支持 List 结构
        List<Map<String, Object>> nodeList = new ArrayList<>();

        Files.walk(Paths.get(SRC_ROOT))
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String packageName = cu.getPackageDeclaration()
                                .map(pd -> pd.getNameAsString()).orElse("default");

                        cu.getTypes().forEach(type -> {
                            Map<String, Object> node = new LinkedHashMap<>();
                            // 1. 继承 Step 1 的核心字段
                            node.put("address", packageName + "." + type.getNameAsString());
                            node.put("kind", getKind(type));

                            // 2. 🚀 Step 2 增量：只增加泛型信息
                            node.put("generics", getGenerics(type));

                            nodeList.add(node);
                        });
                    } catch (Exception ignored) {}
                });

        saveToJson(nodeList, outputFile);
        System.out.println("✅ Step 2 扫描完成，新资产已存至: " + outputFile.getPath());

        analyzeGenericsDistribution();
    }
    private List<String> getGenerics(TypeDeclaration<?> t) {
        // 仅针对类和接口（ClassOrInterfaceDeclaration）提取
        if (t instanceof ClassOrInterfaceDeclaration) {
            return ((ClassOrInterfaceDeclaration) t).getTypeParameters().stream()
                    .map(tp -> tp.asString())
                    .collect(java.util.stream.Collectors.toList());
        }
        return Collections.emptyList();
    }

    @DisplayName("Step-02 统计：分析泛型组件的分布")
    public void analyzeGenericsDistribution() throws Exception {
        String STEP2_FILE = "Redisson_Step2_1_Generics.json";
        File inputFile = getTestResourceFile(STEP2_FILE);

        if (!inputFile.exists()) {
            System.err.println("❌ 错误：请先运行 scanNodesWithGenerics()");
            return;
        }

        byte[] bytes = Files.readAllBytes(inputFile.toPath());
        String jsonContent = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        Gson gson = new Gson();

        // 解析为 List<Map>
        List<Map<String, Object>> nodes = gson.fromJson(jsonContent, List.class);

        // 1. 过滤出带有泛型的组件
        List<Map<String, Object>> genericNodes = nodes.stream()
                .filter(node -> {
                    List<?> generics = (List<?>) node.get("generics");
                    return generics != null && !generics.isEmpty();
                })
                .collect(java.util.stream.Collectors.toList());

        System.out.println("\n🧬 === Redisson 泛型组件资产报告 ===");
        System.out.println("总组件数: " + nodes.size());
        System.out.println("带有泛型的组件数: " + genericNodes.size());
        System.out.println("泛型普及率: " + String.format("%.2f%%", (genericNodes.size() * 100.0 / nodes.size())));
        System.out.println("--------------------------------");

        // 2. 打印前 10 个典型范例，用于肉眼观察校验
        System.out.println("📑 典型泛型组件抽样 (Top 10):");
        genericNodes.stream().limit(10).forEach(node -> {
            System.out.printf(" - [%s] %s %s\n",
                    node.get("kind"),
                    node.get("address"),
                    node.get("generics"));
        });
    }



    private List<String> getModifiers(TypeDeclaration<?> t) {
        return t.getModifiers().stream()
                .map(m -> m.getKeyword().asString())
                .collect(java.util.stream.Collectors.toList());
    }



    /**
     * Step-02-B: 增量开发 - 扫描元数据（泛型 + 修饰符）
     * 保持 Step-01 资产不动，生成更高维度的 Step-02 资产
     */
    @Test
    @DisplayName("Step-02-B: 增量扫描 - 捕获泛型与修饰符")
    public void scanNodesWithFullMetadata() throws Exception {
        // 定义新的资产文件，实现版本迭代
        String STEP2_B_FILE = "Redisson_Step2_2_FullMetadata.json";
        File outputFile = getTestResourceFile(STEP2_B_FILE);

        List<Map<String, Object>> nodeList = new ArrayList<>();

        Files.walk(Paths.get(SRC_ROOT))
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String packageName = cu.getPackageDeclaration()
                                .map(pd -> pd.getNameAsString()).orElse("default");

                        cu.getTypes().forEach(type -> {
                            Map<String, Object> node = new LinkedHashMap<>();
                            // 1. 存量基础字段 (From Step 1)
                            node.put("address", packageName + "." + type.getNameAsString());
                            node.put("kind", getKind(type));

                            // 2. 增量元数据 🚀 (New Dimensions)
                            node.put("generics", getGenerics(type));
                            node.put("modifiers", getModifiers(type));

                            nodeList.add(node);
                        });
                    } catch (Exception ignored) {}
                });

        saveToJson(nodeList, outputFile);
        System.out.println("✅ Step-02-B 增量扫描完成，资产已存至: " + outputFile.getPath());

        // 立即执行新维度的统计分析
        analyzeFullMetadataDistribution(STEP2_B_FILE);
    }

    @DisplayName("分析：多维度元数据分布统计")
    private void analyzeFullMetadataDistribution(String fileName) throws Exception {
        File inputFile = getTestResourceFile(fileName);
        byte[] bytes = Files.readAllBytes(inputFile.toPath());
        String jsonContent = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        Gson gson = new Gson();
        List<Map<String, Object>> nodes = gson.fromJson(jsonContent, List.class);

        // 统计 Public 组件
        long publicCount = nodes.stream()
                .filter(n -> ((List<String>) n.get("modifiers")).contains("public"))
                .count();

        // 统计 Final 组件 (通常代表不可变或禁止继承的策略)
        long finalCount = nodes.stream()
                .filter(n -> ((List<String>) n.get("modifiers")).contains("final"))
                .count();

        // 统计 带有泛型的接口 (核心协议层)
        long genericInterfaceCount = nodes.stream()
                .filter(n -> "INTERFACE".equals(n.get("kind")))
                .filter(n -> !((List<?>) n.get("generics")).isEmpty())
                .count();

        System.out.println("\n📈 === Redisson 深度元数据报告 ===");
        System.out.println("资产规模: " + nodes.size());
        System.out.println("--------------------------------");
        System.out.printf("🔓 [Public 暴露率]: %d 个 (%.2f%%)\n", publicCount, (publicCount * 100.0 / nodes.size()));
        System.out.printf("🛡️ [Final 约束数]: %d 个\n", finalCount);
        System.out.printf("🧬 [泛型协议层]: %d 个 (带有泛型的接口)\n", genericInterfaceCount);

        // 抽样展示，验证 Modifiers 是否正确捕获
        System.out.println("\n📑 随机抽样验证 (Name | Modifiers | Generics):");
        nodes.stream().filter(n -> !((List<?>) n.get("generics")).isEmpty()).limit(5).forEach(n -> {
            System.out.printf(" - %s | %s | %s\n",
                    n.get("address"), n.get("modifiers"), n.get("generics"));
        });
    }


    /**
     * Step-03: 拓扑关系数字化 (独立增量版本)
     * 目标：在不丢失 Kind, Generics, Modifiers 的前提下，识别 Extends 和 Implements
     * 🕸️ === Redisson 拓扑关系数字化报告 (Step-03) ===
     * 总资产节点: 1446
     * --------------------------------------------------
     * [ABSTRACT_CLASS] org.redisson.RedissonBaseLock
     *    ├── Meta: [public, abstract] | []
     *    ├── Extends: RedissonExpirable
     *    └── Implements: [RLock]
     *
     * [CLASS] org.redisson.BooleanSlotCallback
     *    ├── Meta: [public] | []
     *    ├── Extends: null
     *    └── Implements: [SlotCallback]
     */
    @Test
    @DisplayName("Step-03: 增量扫描 - 捕获组件血缘关系")
    public void scanRelationships() throws Exception {
        String STEP3_FILE = "Redisson_Step3_Relationships.json";
        File outputFile = getTestResourceFile(STEP3_FILE);
        List<Map<String, Object>> nodeList = new ArrayList<>();

        Files.walk(Paths.get(SRC_ROOT))
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String packageName = cu.getPackageDeclaration()
                                .map(pd -> pd.getNameAsString()).orElse("default");

                        cu.getTypes().forEach(type -> {
                            Map<String, Object> node = new LinkedHashMap<>();

                            // --- 1. 继承 Step-01 & Step-02 的资产 ---
                            node.put("address", packageName + "." + type.getNameAsString());
                            node.put("kind", getKind(type));
                            node.put("generics", getGenerics(type));
                            node.put("modifiers", getModifiers(type));

                            // --- 2. 核心增量：Step-03 血缘识别 ---
                            if (type instanceof ClassOrInterfaceDeclaration) {
                                ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) type;

                                // 提取继承关系 (含接口继承多个接口的情况)
                                List<String> extendsList = cid.getExtendedTypes().stream()
                                        .map(et -> et.getNameWithScope())
                                        .collect(java.util.stream.Collectors.toList());
                                // 逻辑：类存 String，接口存 List (支持多继承)
                                node.put("extends", cid.isInterface() ? extendsList : (extendsList.isEmpty() ? null : extendsList.get(0)));

                                // 提取实现关系 (多实现)
                                List<String> implementsList = cid.getImplementedTypes().stream()
                                        .map(it -> it.getNameWithScope())
                                        .collect(java.util.stream.Collectors.toList());
                                node.put("implements", implementsList);
                            } else {
                                node.put("extends", null);
                                node.put("implements", Collections.emptyList());
                            }

                            nodeList.add(node);
                        });
                    } catch (Exception ignored) {}
                });

        saveToJson(nodeList, outputFile);

        // --- 3. 打印分析结果：确保系统状态可见 ---
        printStep3Report(nodeList);
    }

    /**
     * Step-03 专用报告打印
     */
    private void printStep3Report(List<Map<String, Object>> nodes) {
        System.out.println("\n🕸️ === Redisson 拓扑关系数字化报告 (Step-03) ===");
        System.out.println("总资产节点: " + nodes.size());
        System.out.println("--------------------------------------------------");

        nodes.stream()
                // 优先展示有血缘关系的复杂组件
                .filter(n -> n.get("extends") != null || !((List<?>) n.get("implements")).isEmpty())
                .limit(10)
                .forEach(node -> {
                    System.out.printf("[%s] %s\n", node.get("kind"), node.get("address"));
                    System.out.printf("   ├── Meta: %s | %s\n", node.get("modifiers"), node.get("generics"));
                    System.out.printf("   ├── Extends: %s\n", node.get("extends"));
                    System.out.printf("   └── Implements: %s\n", node.get("implements"));
                    System.out.println();
                });
    }


    /**
     * Step-04: 数字化矩阵 - 全路径溯源版本
     * 增量逻辑：不再只存类型名，而是通过 Import 列表解析出继承/实现的完整包路径
     */
    @Test
    @DisplayName("Step-04: 全路径扫描 - 捕获组件的完整血缘地址")
    public void scanFullUniverseWithQualifiedNames() throws Exception {
        String STEP4_FULL_PATH_FILE = "Redisson_Step4_Full_Paths.json";
        File outputFile = getTestResourceFile(STEP4_FULL_PATH_FILE);
        List<Map<String, Object>> nodeList = new ArrayList<>();

        Files.walk(Paths.get(SRC_ROOT))
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String packageName = cu.getPackageDeclaration()
                                .map(pd -> pd.getNameAsString()).orElse("default");

                        // 🚀 核心：构建地址簿 (Import Map)
                        // key: 类简名 (Map), value: 全路径 (java.util.Map)
                        Map<String, String> importMap = cu.getImports().stream()
                                .collect(java.util.stream.Collectors.toMap(
                                        im -> im.getName().getIdentifier(),
                                        im -> im.getNameAsString(),
                                        (existing, replacement) -> existing // 避免重复
                                ));

                        cu.getTypes().forEach(type -> {
                            Map<String, Object> node = new LinkedHashMap<>();

                            // 1. S1 & S2: 身份与元数据
                            node.put("address", packageName + "." + type.getNameAsString());
                            node.put("kind", getKind(type));
                            node.put("generics", getGenerics(type));
                            node.put("modifiers", getModifiers(type));

                            // 2. S3 & S4: 拓扑与全路径解析 🚀
                            if (type instanceof ClassOrInterfaceDeclaration) {
                                ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) type;
                                node.put("hierarchy", analyzeQualifiedHierarchy(cid, importMap, packageName));
                            }

                            nodeList.add(node);
                        });
                    } catch (Exception ignored) {}
                });

        saveToJson(nodeList, outputFile);
        printFullQualifiedReport(nodeList);
    }

    private Map<String, Object> analyzeQualifiedHierarchy(ClassOrInterfaceDeclaration cid, Map<String, String> importMap, String currentPkg) {
        Map<String, Object> hierarchy = new LinkedHashMap<>();

        // 解析 Extends 的全路径
        List<String> extendsPaths = cid.getExtendedTypes().stream()
                .map(et -> resolveFullQualifiedName(et.getNameAsString(), importMap, currentPkg))
                .collect(java.util.stream.Collectors.toList());
        hierarchy.put("extends", extendsPaths);

        // 解析 Implements 的全路径
        List<String> implementsPaths = cid.getImplementedTypes().stream()
                .map(it -> resolveFullQualifiedName(it.getNameAsString(), importMap, currentPkg))
                .collect(java.util.stream.Collectors.toList());
        hierarchy.put("implements", implementsPaths);

        return hierarchy;
    }

    /**
     * 全路径解析算法：
     * 1. 查 Import 表：如果类名在 Import 里，直接返回全名
     * 2. 查同包：尝试拼接当前包名 + 类名
     * 3. 查 java.lang：如果属于常用标准类，标记为 java.lang.*
     */
    private String resolveFullQualifiedName(String typeName, Map<String, String> importMap, String currentPkg) {
        // 1. 处理带泛型的名称 (RMap<K, V> -> RMap)
        String baseName = typeName.contains("<") ? typeName.substring(0, typeName.indexOf("<")) : typeName;

        // 2. 匹配 Import
        if (importMap.containsKey(baseName)) {
            return importMap.get(baseName);
        }

        // 3. 匹配 Java 标准库 (java.lang 下的类不需要 import)
        List<String> javaLangTypes = Arrays.asList("Serializable", "Comparable", "Cloneable", "Runnable", "Object", "String", "Exception", "Error");
        if (javaLangTypes.contains(baseName)) {
            return "java.lang." + baseName;
        }

        // 4. 默认为同包引用 (Internal)
        return currentPkg + "." + baseName;
    }

    private void printFullQualifiedReport(List<Map<String, Object>> nodes) {
        System.out.println("\n🗺️ === Redisson 全路径数字化矩阵报告 (Step-04) ===");
        System.out.println("资产规模: " + nodes.size());
        System.out.println("--------------------------------------------------");

        nodes.stream().limit(10).forEach(node -> {
            System.out.printf("[%s] %s\n", node.get("kind"), node.get("address"));
            Map<String, Object> hierarchy = (Map<String, Object>) node.get("hierarchy");
            if (hierarchy != null) {
                System.out.printf("   ├── Extends: %s\n", hierarchy.get("extends"));
                System.out.printf("   └── Implements: %s\n", hierarchy.get("implements"));
            }
            System.out.println();
        });
    }
    @Test
    @DisplayName("Step-05: 全量整合 - 身份+元数据+全路径血缘+方法异常契约")
    public void scanFullUniverseWithContract() throws Exception {
        String STEP5_FILE = "Redisson_Step5_Full_Matrix.json";
        File outputFile = getTestResourceFile(STEP5_FILE);
        List<Map<String, Object>> nodeList = new ArrayList<>();

        Files.walk(Paths.get(SRC_ROOT))
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("default");

                        // S4: 全路径地址簿
                        Map<String, String> importMap = cu.getImports().stream()
                                .collect(java.util.stream.Collectors.toMap(
                                        im -> im.getName().getIdentifier(), im -> im.getNameAsString(), (e, r) -> e));

                        cu.getTypes().forEach(type -> {
                            Map<String, Object> node = new LinkedHashMap<>();

                            // S1 & S2: 身份标识与元数据
                            node.put("address", pkg + "." + type.getNameAsString());
                            node.put("kind", getKind(type));
                            node.put("generics", getGenerics(type));
                            node.put("modifiers", getModifiers(type));

                            // S3 & S4: 全路径血缘拓扑
                            Map<String, List<String>> hierarchy = new LinkedHashMap<>();
                            if (type instanceof ClassOrInterfaceDeclaration) {
                                ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) type;
                                hierarchy.put("extends", cid.getExtendedTypes().stream()
                                        .map(et -> resolveFullQualifiedName(et.getNameAsString(), importMap, pkg))
                                        .collect(java.util.stream.Collectors.toList()));
                                hierarchy.put("implements", cid.getImplementedTypes().stream()
                                        .map(it -> resolveFullQualifiedName(it.getNameAsString(), importMap, pkg))
                                        .collect(java.util.stream.Collectors.toList()));
                            }
                            node.put("hierarchy", hierarchy);

                            // 🚀 Step-05: 方法级外部异常契约 (核心增量)
                            Set<String> externalThrows = new HashSet<>();
                            type.getMethods().forEach(m -> {
                                m.getThrownExceptions().forEach(te -> {
                                    String fullName = resolveFullQualifiedName(te.asString(), importMap, pkg);
                                    // 仅保留 EXTERNAL 路径
                                    if (!fullName.startsWith("org.redisson")) {
                                        externalThrows.add(fullName);
                                    }
                                });
                            });
                            node.put("method_throws_contract", new ArrayList<>(externalThrows));

                            nodeList.add(node);
                        });
                    } catch (Exception ignored) {}
                });

        saveToJson(nodeList, outputFile);

        // 🚀 这里的过滤打印能让你看到“异常”到底在哪里
        printStep5ExceptionFocusedReport(nodeList);
    }

    private void printStep5ExceptionFocusedReport(List<Map<String, Object>> nodes) {
        System.out.println("\n🚦 === Redisson 异常契约全路径报告 (S1-S5 整合) ===");
        System.out.println("资产规模: " + nodes.size() + " 节点");
        System.out.println("-------------------------------------------------------");

        nodes.stream()
                // 🚀 关键：只过滤出那些确实抛出了外部异常的组件进行打印展示
                .filter(n -> !((List<?>) n.get("method_throws_contract")).isEmpty())
                .limit(15)
                .forEach(node -> {
                    System.out.printf("[%s] %s\n", node.get("kind"), node.get("address"));
                    System.out.printf("   ├── Meta: %s | %s\n", node.get("modifiers"), node.get("generics"));

                    Map<?, ?> h = (Map<?, ?>) node.get("hierarchy");
                    if (h != null && (!((List<?>)h.get("extends")).isEmpty() || !((List<?>)h.get("implements")).isEmpty())) {
                        System.out.printf("   ├── Hierarchy: Extends=%s, Implements=%s\n", h.get("extends"), h.get("implements"));
                    }

                    // 异常全路径输出
                    System.out.printf("   └── ⚠️ External Throws: %s\n", node.get("method_throws_contract"));
                    System.out.println();
                });
    }
}
