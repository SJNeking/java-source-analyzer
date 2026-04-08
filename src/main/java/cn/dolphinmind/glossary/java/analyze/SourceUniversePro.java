package cn.dolphinmind.glossary.java.analyze;

import cn.dolphinmind.glossary.java.analyze.config.RulesConfig;
import cn.dolphinmind.glossary.java.analyze.orchestrate.AnalysisConfig;
import cn.dolphinmind.glossary.java.analyze.orchestrate.AnalysisOrchestrator;
import cn.dolphinmind.glossary.java.analyze.translate.SemanticTranslator;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;

import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SourceUniversePro {

    private static final AtomicInteger classCount = new AtomicInteger(0);
    private static final AtomicInteger methodCount = new AtomicInteger(0);
    private static final AtomicInteger fieldCount = new AtomicInteger(0);
    private static final AtomicInteger commentFound = new AtomicInteger(0);

    // 🚀 用于去重的依赖集合
    private static final java.util.Set<String> seenDependencies = Collections.synchronizedSet(new HashSet<>());

    // 🚀 用于模式分析的统计数据集
    private static final List<String> allClassNames = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> allMethodNames = Collections.synchronizedList(new ArrayList<>());

    // 🚀 动态标签库
    private static JsonObject tagDictionary;
    // 🚀 技术词汇中英映射表 (从 tech-instruction-set.json 加载)
    private static Map<String, String> techInstructionSet = new HashMap<>();
    // 🚀 全局基础字典 (从 cleaned-english-chinese-mapping.json 加载)
    private static Map<String, String> globalBaseDictionary = new HashMap<>();

    /**
     * 加载动态标签字典与技术指令集
     */
    private static void loadTagDictionary() {
        try {
            java.net.URL resource = SourceUniversePro.class.getClassLoader().getResource("tag-dictionary.json");
            if (resource != null) {
                com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(
                        new java.io.InputStreamReader(resource.openStream(), StandardCharsets.UTF_8));
                reader.setStrictness(com.google.gson.Strictness.LENIENT);
                tagDictionary = JsonParser.parseReader(reader).getAsJsonObject();
                System.out.println("✅ 动态标签字典加载成功: " + tagDictionary.getAsJsonObject("tags").size() + " 个标签");
            }
        } catch (Exception e) {
            System.err.println("⚠️ 动态标签字典加载失败: " + e.getMessage());
        }

        // 加载技术指令集 (Tech Instruction Set)
        try {
            java.net.URL resource = SourceUniversePro.class.getClassLoader().getResource("tech-instruction-set.json");
            if (resource != null) {
                JsonObject dictObj = JsonParser.parseReader(
                        new java.io.InputStreamReader(resource.openStream(), StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : dictObj.keySet()) {
                    techInstructionSet.put(key.toLowerCase(), dictObj.get(key).getAsString());
                }
                System.out.println("✅ 技术指令集加载成功: " + techInstructionSet.size() + " 个专业词条");
            }
        } catch (Exception e) {
            System.err.println("⚠️ 技术指令集加载失败: " + e.getMessage());
        }

        // 加载全局基础字典 (Global Base Dictionary)
        try {
            java.net.URL resource = SourceUniversePro.class.getClassLoader().getResource("cleaned-english-chinese-mapping.json");
            if (resource != null) {
                JsonObject dictObj = JsonParser.parseReader(
                        new java.io.InputStreamReader(resource.openStream(), StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : dictObj.keySet()) {
                    globalBaseDictionary.put(key.toLowerCase(), dictObj.get(key).getAsString());
                }
                System.out.println("✅ 全局基础字典加载成功: " + globalBaseDictionary.size() + " 个通用词条");
            } else {
                System.err.println("⚠️ cleaned-english-chinese-mapping.json not found on classpath");
            }
        } catch (Exception e) {
            System.err.println("⚠️ 全局基础字典加载失败: " + e.getMessage());
        }
    }

    // 🚀 标签库：ID -> {CN, EN, Description} (内存缓存)
    private static final Map<String, String[]> TAG_LIBRARY = new HashMap<>();
    
    /**
     * 初始化内存标签缓存
     */
    private static void initTagLibrary() {
        if (tagDictionary == null || !tagDictionary.has("tags")) return;
        JsonObject tags = tagDictionary.getAsJsonObject("tags");
        for (String id : tags.keySet()) {
            JsonObject tag = tags.getAsJsonObject(id);
            TAG_LIBRARY.put(id, new String[]{
                tag.get("cn").getAsString(),
                tag.get("en").getAsString(),
                tag.get("desc").getAsString()
            });
        }
    }

    // 🚀 高频技术术语中英映射表 (AI Agent 专用)
    private static final Map<String, String> TECH_TERM_MAP = new HashMap<>();
    static {
        TECH_TERM_MAP.put("Asynchronous", "异步(Asynchronous)");
        TECH_TERM_MAP.put("Synchronization", "同步(Synchronization)");
        TECH_TERM_MAP.put("Serialization", "序列化(Serialization)");
        TECH_TERM_MAP.put("Concurrency", "并发(Concurrency)");
        TECH_TERM_MAP.put("Transaction", "事务(Transaction)");
        TECH_TERM_MAP.put("Configuration", "配置(Configuration)");
        TECH_TERM_MAP.put("Exception", "异常(Exception)");
        TECH_TERM_MAP.put("Implementation", "实现(Implementation)");
        TECH_TERM_MAP.put("Abstract", "抽象(Abstract)");
        TECH_TERM_MAP.put("Interface", "接口(Interface)");
        TECH_TERM_MAP.put("Factory", "工厂(Factory)");
        TECH_TERM_MAP.put("Proxy", "代理(Proxy)");
        TECH_TERM_MAP.put("Adapter", "适配器(Adapter)");
        TECH_TERM_MAP.put("Strategy", "策略(Strategy)");
        TECH_TERM_MAP.put("Observer", "观察者(Observer)");
        TECH_TERM_MAP.put("Singleton", "单例(Singleton)");
        TECH_TERM_MAP.put("Registry", "注册中心(Registry)");
        TECH_TERM_MAP.put("Protocol", "协议(Protocol)");
        TECH_TERM_MAP.put("Transport", "传输层(Transport)");
        TECH_TERM_MAP.put("Endpoint", "端点(Endpoint)");
        TECH_TERM_MAP.put("Channel", "通道(Channel)");
        TECH_TERM_MAP.put("Pipeline", "管道(Pipeline)");
        TECH_TERM_MAP.put("Handler", "处理器(Handler)");
        TECH_TERM_MAP.put("Codec", "编解码器(Codec)");
        TECH_TERM_MAP.put("Persistence", "持久化(Persistence)");
        TECH_TERM_MAP.put("Repository", "仓储(Repository)");
        TECH_TERM_MAP.put("Connection Pool", "连接池(Connection Pool)");
        TECH_TERM_MAP.put("Cache", "缓存(Cache)");
        TECH_TERM_MAP.put("Eviction", "驱逐(Eviction)");
        TECH_TERM_MAP.put("Sharding", "分片(Sharding)");
        TECH_TERM_MAP.put("Authentication", "认证(Authentication)");
        TECH_TERM_MAP.put("Authorization", "授权(Authorization)");
        TECH_TERM_MAP.put("Encryption", "加密(Encryption)");
        TECH_TERM_MAP.put("Validation", "校验(Validation)");
        TECH_TERM_MAP.put("Initialization", "初始化(Initialization)");
        TECH_TERM_MAP.put("Invocation", "调用(Invocation)");
        TECH_TERM_MAP.put("Reflection", "反射(Reflection)");
        TECH_TERM_MAP.put("Annotation", "注解(Annotation)");
        
        // 🚀 新增：HikariCP 及常见技术语境校准
        TECH_TERM_MAP.put("Metrics", "指标(Metrics)");
        TECH_TERM_MAP.put("Tracker", "追踪器(Tracker)");
        TECH_TERM_MAP.put("Elf", "辅助线程(Elf)");
        TECH_TERM_MAP.put("Bag", "并发容器(Bag)");
        TECH_TERM_MAP.put("Pool", "池(Pool)");
        TECH_TERM_MAP.put("DataSource", "数据源(DataSource)");
        TECH_TERM_MAP.put("Statement", "语句(Statement)");
        TECH_TERM_MAP.put("ResultSet", "结果集(ResultSet)");
        TECH_TERM_MAP.put("Callable", "可调用(Callable)");
        TECH_TERM_MAP.put("PreparedStatement", "预编译语句(PreparedStatement)");
        TECH_TERM_MAP.put("MetaData", "元数据(MetaData)");
        TECH_TERM_MAP.put("Histogram", "直方图(Histogram)");
        TECH_TERM_MAP.put("Prometheus", "普罗米修斯监控(Prometheus)");
        TECH_TERM_MAP.put("Micrometer", "微计量(Micrometer)");
        TECH_TERM_MAP.put("JNDI", "Java命名与目录接口(JNDI)");
        TECH_TERM_MAP.put("MXBean", "管理扩展Bean(MXBean)");
        TECH_TERM_MAP.put("Concurrent", "并发的(Concurrent)");
        TECH_TERM_MAP.put("FastList", "快速列表(FastList)");
        TECH_TERM_MAP.put("Isolation", "隔离(Isolation)");
        TECH_TERM_MAP.put("Leak", "泄漏(Leak)");
        TECH_TERM_MAP.put("Suspend", "挂起(Suspend)");
        TECH_TERM_MAP.put("Resume", "恢复(Resume)");
        TECH_TERM_MAP.put("Override", "重写/覆盖(Override)");
        TECH_TERM_MAP.put("Util", "工具(Util)");
        TECH_TERM_MAP.put("Provider", "提供者(Provider)");
        TECH_TERM_MAP.put("Stats", "统计信息(Stats)");
        TECH_TERM_MAP.put("Clock", "时钟(Clock)");
        TECH_TERM_MAP.put("Source", "源(Source)");
        TECH_TERM_MAP.put("Property", "属性(Property)");
    }

    // 🚀 标签引擎配置
    private static JsonObject namingTags;
    private static JsonObject frameworkTags;
    private static JsonObject codeExamples;
    
    // 🚀 迭代进化：未识别模式收集器
    private static Map<String, Integer> unrecognizedMethodPrefixes = new HashMap<>();
    private static Map<String, Integer> unrecognizedClassSuffixes = new HashMap<>();

    // 🚀 项目专属词汇表 (Project-Specific Glossary)
    private static Map<String, String> projectGlossary = new HashMap<>();
    private static String currentProjectRoot = "";

    /**
     * 智能标识符翻译引擎 (MVP Core - Corrected)
     * 逻辑: 整体匹配 -> 驼峰拆分(过滤短词) -> 后缀推理 -> 保留原文
     */
    private static String translateIdentifier(String name) {
        if (name == null || name.isEmpty()) return "";
        
        // 1. 尝试直接匹配整个词 (针对缩写或专有名词)
        String directMatch = lookupTerm(name);
        if (directMatch != null) return directMatch;

        // 2. 驼峰拆分并逐词翻译
        List<String> tokens = splitCamelCase(name);
        StringBuilder result = new StringBuilder();
        
        for (String token : tokens) {
            // 过滤掉无意义的单字母（除非是 I/O 等常见缩写）
            if (token.length() <= 1 && !token.matches("[IOA]")) {
                result.append(token);
                continue;
            }

            String cn = lookupTerm(token);
            if (cn == null) {
                cn = inferBySuffix(token);
            }
            // 如果还是查不到，保留英文原词，避免乱翻译
            result.append(cn != null ? cn : token);
        }
        
        return result.toString();
    }

    /**
     * 三级字典查询逻辑 (优化版)
     */
    private static String lookupTerm(String term) {
        if (term.isEmpty()) return null;
        String key = term.toLowerCase();
        
        // Level 1: 项目专属 (最高优先级)
        if (projectGlossary.containsKey(key)) {
            return projectGlossary.get(key);
        }
        
        // Level 2: 技术指令集 (精准技术词 - 555个)
        if (techInstructionSet.containsKey(key)) {
            return techInstructionSet.get(key);
        }
        
        // Level 3: 仅在技术词根表中才查全局字典，否则返回 null 避免噪音
        if (isTechRootWord(term)) {
            if (globalBaseDictionary.containsKey(key)) {
                return globalBaseDictionary.get(key);
            }
        }
        
        return null;
    }

    /**
     * 简单判断是否为常见技术词根 (防止通用字典噪音)
     */
    private static boolean isTechRootWord(String word) {
        String w = word.toLowerCase();
        return w.length() > 3 && (w.contains("net") || w.contains("sys") || w.contains("data") || 
                                  w.contains("code") || w.contains("log") || w.contains("file") ||
                                  w.contains("time") || w.contains("user") || w.contains("config"));
    }

    /**
     * 驼峰命名拆分器
     */
    private static List<String> splitCamelCase(String input) {
        if (input == null || input.isEmpty()) return Collections.emptyList();
        String[] parts = input.split("(?=[A-Z])");
        return Arrays.stream(parts).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    /**
     * 常见技术后缀推理
     */
    private static String inferBySuffix(String token) {
        if (token.endsWith("Impl")) return "实现";
        if (token.endsWith("Utils") || token.endsWith("Helper")) return "工具";
        if (token.endsWith("Factory")) return "工厂";
        if (token.endsWith("Manager")) return "管理器";
        if (token.endsWith("Handler")) return "处理器";
        if (token.endsWith("Listener")) return "监听器";
        if (token.endsWith("Exception")) return "异常";
        if (token.endsWith("Service")) return "服务";
        if (token.endsWith("Controller")) return "控制器";
        if (token.endsWith("Repository") || token.endsWith("Dao")) return "仓储";
        if (token.endsWith("Config")) return "配置";
        if (token.endsWith("Proxy")) return "代理";
        if (token.endsWith("Adapter")) return "适配器";
        return null;
    }

    /**
     * 加载项目专属字典
     */
    private static void loadProjectGlossary(String projectRoot) {
        currentProjectRoot = projectRoot;
        File glossaryFile = new File(projectRoot + "/.universe/tech-glossary.json");
        if (glossaryFile.exists()) {
            try {
                JsonObject obj = JsonParser.parseReader(new FileReader(glossaryFile)).getAsJsonObject();
                for (String key : obj.keySet()) {
                    projectGlossary.put(key.toLowerCase(), obj.get(key).getAsString());
                }
                System.out.println("✅ 加载项目专属字典: " + projectGlossary.size() + " 个词条");
            } catch (Exception e) {
                System.err.println("⚠️ 加载项目字典失败: " + e.getMessage());
            }
        } else {
            System.out.println("🆕 未发现项目专属字典，将创建新的学习记录。");
        }
    }

    /**
     * 保存项目专属字典 (增量更新)
     */
    private static void saveProjectGlossary() {
        if (projectGlossary.isEmpty()) return;
        try {
            File dir = new File(currentProjectRoot + "/.universe");
            if (!dir.exists()) dir.mkdirs();
            
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, String> entry : projectGlossary.entrySet()) {
                obj.addProperty(entry.getKey(), entry.getValue());
            }
            
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            try (FileWriter fw = new FileWriter(currentProjectRoot + "/.universe/tech-glossary.json")) {
                gson.toJson(obj, fw);
            }
            System.out.println("💾 已保存项目专属字典至: .universe/tech-glossary.json");
        } catch (Exception e) {
            System.err.println("⚠️ 保存项目字典失败: " + e.getMessage());
        }
    }

    /**
     * Main entry point: parse CLI args and delegate to orchestrator.
     */
    public static void main(String[] args) throws Exception {
        // 1. Parse CLI arguments into config
        AnalysisConfig config = parseCliArgs(args);
        if (config == null) return; // --help was shown

        // 2. Create services
        SemanticTranslator translator = new SemanticTranslator();

        // 3. Create and run orchestrator
        AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(config, translator);
        orchestrator.execute();
    }

    /**
     * Parse CLI arguments into AnalysisConfig.
     * Returns null if --help was shown.
     */
    private static AnalysisConfig parseCliArgs(String[] args) {
        AnalysisConfig config = new AnalysisConfig();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--sourceRoot":
                    if (i + 1 < args.length) config.setSourceRoot(args[++i]);
                    break;
                case "--outputDir":
                    if (i + 1 < args.length) config.setOutputDir(args[++i]);
                    break;
                case "--artifactName":
                    if (i + 1 < args.length) config.setArtifactName(args[++i]);
                    break;
                case "--version":
                    if (i + 1 < args.length) config.setVersion(args[++i]);
                    break;
                case "--internalPkgPrefix":
                    if (i + 1 < args.length) config.setInternalPkgPrefix(args[++i]);
                    break;
                case "--rules-config":
                    if (i + 1 < args.length) config.setRulesConfigPath(args[++i]);
                    break;
                case "--help":
                    printUsage();
                    return null;
                default:
                    if (config.getSourceRoot() == null) {
                        config.setSourceRoot(args[i]);
                    }
                    break;
            }
        }

        // Apply defaults and show warnings
        if (config.getSourceRoot() == null) {
            config.setSourceRoot("/Users/mingxilv/WebDevelopment/gitcode/dev-proj/s-pay-mall/s-pay-mall-ddd/source-proj/jdk8-src");
            System.out.println("⚠️  未指定 --sourceRoot，使用默认路径: " + config.getSourceRoot());
        }

        return config;
    }

    /**
     * Run the full analysis pipeline.
     */
    public static void runAnalysis(AnalysisConfig config, SemanticTranslator translator) throws Exception {
        // Load dictionaries
        loadTagDictionary();
        initTagLibrary();
        loadNamingTags();
        loadCodeExamples();

        RulesConfig rulesConfig = RulesConfig.load(config.getRulesConfigPath());
        translator.loadProjectGlossary(config.getSourceRoot());

        // Configure JavaParser Symbol Solver with full Maven classpath
        cn.dolphinmind.glossary.java.analyze.core.ClasspathResolver cpResolver = null;
        try {
            cpResolver = new cn.dolphinmind.glossary.java.analyze.core.ClasspathResolver();
        } catch (Exception e) {}

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new JavaParserTypeSolver(new java.io.File(config.getSourceRoot())));
        try { registerSubModules(typeSolver, config.getSourceRoot()); } catch (Exception ignored) {}

        // Add Maven dependencies to type solver for accurate symbol resolution
        if (cpResolver != null) {
            try {
                CombinedTypeSolver fullSolver = cn.dolphinmind.glossary.java.analyze.core.ClasspathResolver.create(
                        Paths.get(config.getSourceRoot()));
                // Merge: add all type solvers from the full resolver
                typeSolver = fullSolver;
                System.out.println("📚 Maven classpath loaded for symbol resolution");
            } catch (Exception e) {
                System.out.println("⚠️  Maven classpath not available: " + e.getMessage());
            }
        }

        com.github.javaparser.ParserConfiguration parserConfig = new com.github.javaparser.ParserConfiguration();
        parserConfig.setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_8);
        parserConfig.setSymbolResolver(new com.github.javaparser.symbolsolver.JavaSymbolSolver(typeSolver));
        StaticJavaParser.setConfiguration(parserConfig);

        Path outputDir = config.getOutputDirPath();
        Files.createDirectories(outputDir);

        String frameworkName = extractFrameworkName(config.getSourceRoot());
        String detectedVersion = config.getVersion() != null ? config.getVersion() : extractVersion(config.getSourceRoot());
        config.setFrameworkName(frameworkName);
        config.setDetectedVersion(detectedVersion);

        // Print startup banner
        printLine('=', 80);
        System.out.println("🚀 " + frameworkName.toUpperCase() + " SEMANTIC DICTIONARY BUILDER | 语义字典构建引擎启动");
        printLine('=', 80);
        System.out.println("📦 识别到 " + frameworkName + " 版本: " + detectedVersion);

        // Create ScannerContext
        ScannerContext ctx = new ScannerContext(
                config.getSourceRoot(),
                config.getInternalPkgPrefix(),
                config.getEffectiveVersion()
        );

        // Create root container
        Map<String, Object> rootContainer = new LinkedHashMap<>();
        rootContainer.put("framework", frameworkName);
        rootContainer.put("version", detectedVersion);
        rootContainer.put("scan_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        List<Map<String, Object>> globalLibrary = new ArrayList<>();
        Map<String, List<Map<String, Object>>> moduleLibrary = new LinkedHashMap<>();
        List<Map<String, String>> globalDependencies = new ArrayList<>();
        Map<String, Object> projectAssets = new LinkedHashMap<>();

        // Parallel Java source scanning
        System.out.println("🚀 正在全量遍历代码，提取语义词汇...");

        cn.dolphinmind.glossary.java.analyze.scanner.ProjectScanner ps =
                new cn.dolphinmind.glossary.java.analyze.scanner.ProjectScanner(Paths.get(config.getSourceRoot()));
        List<Path> modules = ps.detectModules();
        System.out.println("📦 检测到 " + modules.size() + " 个模块");

        java.util.concurrent.ForkJoinPool forkJoinPool = new java.util.concurrent.ForkJoinPool(
                Math.min(Runtime.getRuntime().availableProcessors(), 8));

        java.util.concurrent.atomic.AtomicInteger filesScanned = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger filesFailed = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger filesSkipped = new java.util.concurrent.atomic.AtomicInteger(0);

        // Load incremental cache with version checking
        Path cacheDir = config.getCacheDir();
        cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.CacheData cache =
                cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.load(cacheDir,
                        SemanticTranslator.getAnalyzerVersion());

        for (Path moduleRoot : modules) {
            String moduleName = moduleRoot.getFileName().toString();
            System.out.println("  🔍 扫描 Java 模块: " + moduleName);

            try {
                List<Path> javaFiles = new java.util.ArrayList<>();
                Files.walk(moduleRoot)
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> {
                            String pathStr = p.toString();
                            return (pathStr.contains("java" + File.separator) || pathStr.contains("src")) &&
                                   !pathStr.contains("test") && !pathStr.contains("target");
                        })
                        .forEach(javaFiles::add);

                System.out.println("    📄 找到 " + javaFiles.size() + " 个 Java 文件");

                List<Path> changedFiles = cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.findChangedFiles(
                        moduleRoot, cache, javaFiles);
                filesSkipped.addAndGet(javaFiles.size() - changedFiles.size());

                if (changedFiles.isEmpty()) {
                    System.out.println("    ✅ 无变更，跳过 " + javaFiles.size() + " 个文件");
                    continue;
                }

                System.out.println("    📄 变更文件: " + changedFiles.size() + ", 跳过: " + (javaFiles.size() - changedFiles.size()));

                forkJoinPool.submit(() ->
                    changedFiles.parallelStream().forEach(path -> {
                        try {
                            filesScanned.incrementAndGet();
                            List<String> fileLines = Files.readAllLines(path, StandardCharsets.UTF_8);
                            CompilationUnit cu = StaticJavaParser.parse(path);
                            String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("default");

                            cu.getTypes().forEach(type -> {
                                classCount.incrementAndGet();
                                String className = type.getNameAsString();
                                allClassNames.add(className);

                                String cnName = translator.translateIdentifier(className);
                                projectGlossary.put(className.toLowerCase(), cnName);

                                Map<String, Object> classAsset = processTypeEnhanced(type, pkg, null, fileLines, ctx, globalDependencies);
                                if (!classAsset.getOrDefault("description", "").toString().isEmpty()) commentFound.incrementAndGet();

                                classAsset.put("module", moduleName);
                                classAsset.put("import_dependencies", extractImportDependencies(cu));
                                classAsset.put("annotation_params", extractAnnotationParams(type));

                                synchronized (globalLibrary) {
                                    globalLibrary.add(classAsset);
                                }
                                synchronized (moduleLibrary) {
                                    moduleLibrary.computeIfAbsent(moduleName, k -> new ArrayList<>()).add(classAsset);
                                }

                                extractDependencies((String) classAsset.get("address"), classAsset, globalDependencies);

                                // Update incremental cache
                                try {
                                    String relPath = moduleRoot.relativize(path).toString();
                                    String hash = cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.computeFileHash(path);
                                    cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.FileEntry entry =
                                            new cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.FileEntry(hash);
                                    synchronized (cache) {
                                        cache.getFiles().put(relPath, entry);
                                    }
                                } catch (Exception cacheEx) {
                                    // ignore cache update errors
                                }
                            });
                        } catch (Exception e) {
                            filesFailed.incrementAndGet();
                            System.err.println("⚠️ 忽略解析失败文件: " + path.getFileName() + " | 错误: " + e.getMessage());
                        }
                    })
                ).get();
            } catch (IOException e) {
                System.err.println("⚠️ 模块扫描失败: " + moduleName);
            }
        }

        forkJoinPool.shutdown();
        System.out.println("✅ 扫描完成: " + filesScanned.get() + " 文件, " + filesFailed.get() + " 失败");

        // Save incremental cache
        try {
            cache.setScanDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
            cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.save(cache, cacheDir);
            System.out.println("💾 增量缓存已保存: " + cache.getFiles().size() + " 文件记录");
        } catch (Exception e) {
            System.err.println("⚠️ 保存增量缓存失败: " + e.getMessage());
        }

        // Scan non-Java assets
        projectAssets = scanProjectFiles(Paths.get(config.getSourceRoot()));

        // Cross-file relations
        cn.dolphinmind.glossary.java.analyze.relation.RelationEngine relationEngine =
                new cn.dolphinmind.glossary.java.analyze.relation.RelationEngine();
        Map<String, Object> relationData = new LinkedHashMap<>();
        relationData.put("assets", globalLibrary);
        List<cn.dolphinmind.glossary.java.analyze.relation.AssetRelation> relations =
                relationEngine.discoverRelations(relationData, projectAssets);
        rootContainer.put("cross_file_relations", relationEngine.toMap());

        // Quality analysis
        System.out.println("\n🔍 正在执行静态代码质量分析...");
        List<Map<String, Object>> qualityIssues = runQualityAnalysis(globalLibrary, rulesConfig, config.getSourceRoot(), globalDependencies);
        rootContainer.put("quality_issues", qualityIssues);
        Map<String, Object> qualitySummary = qualityIssues.isEmpty() ? Collections.emptyMap() : buildQualitySummary(qualityIssues);
        rootContainer.put("quality_summary", qualitySummary);

        // Project type detection
        cn.dolphinmind.glossary.java.analyze.scanner.ProjectTypeDetector typeDetector =
                new cn.dolphinmind.glossary.java.analyze.scanner.ProjectTypeDetector();
        Map<String, Object> projectTypeInfo = typeDetector.detect(
                Paths.get(config.getSourceRoot()), projectAssets, relationData);
        rootContainer.put("project_type", projectTypeInfo);

        // Assemble output
        rootContainer.put("assets", globalLibrary);
        rootContainer.put("dependencies", globalDependencies);
        rootContainer.put("project_assets", projectAssets);
        String safeVersion = detectedVersion.replaceAll("[^a-zA-Z0-9.-]", "_");
        String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());

        // HTML Dashboard
        try {
            String htmlPath = outputDir.resolve(String.format("%s_v%s_report_%s.html", frameworkName, safeVersion, dateStr)).toString();
            cn.dolphinmind.glossary.java.analyze.report.HtmlReportGenerator htmlGen =
                    new cn.dolphinmind.glossary.java.analyze.report.HtmlReportGenerator(rootContainer, htmlPath);
            htmlGen.generate();
            System.out.println("✅ HTML Dashboard: " + htmlPath);
        } catch (Exception e) {
            System.err.println("⚠️ HTML Dashboard generation failed: " + e.getMessage());
        }

        // SARIF
        try {
            String sarifPath = outputDir.resolve(String.format("%s_v%s_%s.sarif", frameworkName, safeVersion, dateStr)).toString();
            cn.dolphinmind.glossary.java.analyze.report.SarifGenerator sarifGen =
                    new cn.dolphinmind.glossary.java.analyze.report.SarifGenerator(rootContainer);
            sarifGen.generate(sarifPath);
            System.out.println("✅ SARIF Report: " + sarifPath);
        } catch (Exception e) {
            System.err.println("⚠️ SARIF generation failed: " + e.getMessage());
        }

        // Technical Debt
        Map<String, Object> debtEstimate = null;
        try {
            cn.dolphinmind.glossary.java.analyze.report.TechnicalDebtEstimator debtEst =
                    new cn.dolphinmind.glossary.java.analyze.report.TechnicalDebtEstimator();
            debtEstimate = debtEst.estimate(rootContainer);
            rootContainer.put("technical_debt", debtEstimate);
            System.out.println("✅ Technical Debt: " + debtEstimate.get("rating") + " | " +
                    debtEstimate.get("total_remediation_hours") + "h | " +
                    debtEstimate.get("technical_debt_ratio_pct") + "%");
        } catch (Exception e) {
            System.err.println("⚠️ Technical debt estimation failed: " + e.getMessage());
        }

        // Quality Gate
        try {
            cn.dolphinmind.glossary.java.analyze.report.QualityGate gate =
                    new cn.dolphinmind.glossary.java.analyze.report.QualityGate();
            cn.dolphinmind.glossary.java.analyze.report.QualityGate.GateResult gateResult = gate.evaluate(rootContainer, debtEstimate);
            rootContainer.put("quality_gate", new LinkedHashMap<String, Object>() {{
                put("passed", gateResult.isPassed());
                put("reasons", gateResult.getReasons());
                put("metrics", gateResult.getMetrics());
            }});
            System.out.println("✅ Quality Gate: " + (gateResult.isPassed() ? "PASSED ✅" : "FAILED ❌"));
            if (!gateResult.isPassed()) {
                for (String reason : gateResult.getReasons()) {
                    System.out.println("   ❌ " + reason);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Quality gate evaluation failed: " + e.getMessage());
        }

        // Filter baseline-marked issues
        cn.dolphinmind.glossary.java.analyze.baseline.BaselineManager.BaselineData baseline =
                cn.dolphinmind.glossary.java.analyze.baseline.BaselineManager.load(Paths.get(config.getSourceRoot()));
        qualityIssues = cn.dolphinmind.glossary.java.analyze.baseline.BaselineManager.filterBaseline(qualityIssues, baseline);
        int baselineCount = qualityIssues.size() - qualityIssues.size();
        if (baselineCount > 0) {
            System.out.println("📌 基线过滤: " + baselineCount + " 个问题已标记（误报/不修复）");
        }

        if (!qualityIssues.isEmpty()) {
            System.out.println("✅ 发现 " + qualityIssues.size() + " 个代码质量问题");
        } else {
            System.out.println("✅ 未发现代码质量问题");
        }

        // 🚀 核心分析引擎：入口点发现 + 调用链追踪 + 包结构地图 + 类型定义导航 + 数据流追踪
        System.out.println("\n=== 核心分析引擎 ===");
        try {
            cn.dolphinmind.glossary.java.analyze.core.CoreAnalysisEngine coreEngine =
                    new cn.dolphinmind.glossary.java.analyze.core.CoreAnalysisEngine();
            Map<String, Object> coreResult = coreEngine.analyze(Paths.get(config.getSourceRoot()));
            rootContainer.put("core_analysis", coreResult);
        } catch (Exception e) {
            System.err.println("⚠️ 核心分析失败: " + e.getMessage());
            e.printStackTrace();
        }

        // Save glossary
        translator.saveProjectGlossary();

        // Generate compressed summary
        try {
            Map<String, Object> compressedSummary = new LinkedHashMap<>();
            compressedSummary.put("framework", frameworkName);
            compressedSummary.put("version", detectedVersion);
            compressedSummary.put("scan_date", rootContainer.get("scan_date"));
            compressedSummary.put("total_classes", globalLibrary.size());
            compressedSummary.put("total_methods", globalLibrary.stream().mapToInt(a -> ((List<?>) a.getOrDefault("methods_full", Collections.emptyList())).size()).sum());
            compressedSummary.put("total_fields", globalLibrary.stream().mapToInt(a -> ((List<?>) a.getOrDefault("fields_matrix", Collections.emptyList())).size()).sum());
            compressedSummary.put("quality_issues", qualityIssues.size());
            compressedSummary.put("technical_debt", debtEstimate);
            compressedSummary.put("quality_gate", rootContainer.get("quality_gate"));
            compressedSummary.put("comment_coverage", rootContainer.get("comment_coverage"));
            compressedSummary.put("project_type", rootContainer.get("project_type"));
            compressedSummary.put("modules", new ArrayList<>(moduleLibrary.keySet()));
            compressedSummary.put("dependencies_count", globalDependencies.size());
            saveAsJson(compressedSummary, outputDir.resolve(String.format("%s_v%s_summary_%s.json", frameworkName, safeVersion, dateStr)).toString());
        } catch (Exception e) {
            System.err.println("⚠️ Summary generation failed: " + e.getMessage());
        }

        // Save full JSON
        saveAsJson(rootContainer, outputDir.resolve(String.format("%s_v%s_full_%s.json", frameworkName, safeVersion, dateStr)).toString());

        // Save glossary raw
        List<Map<String, String>> glossaryRaw = new ArrayList<>();
        for (Map<String, Object> asset : globalLibrary) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("term", (String) asset.get("address"));
            entry.put("kind", (String) asset.get("kind"));
            entry.put("description", (String) asset.getOrDefault("description", ""));
            entry.put("modifiers", String.join(", ", (List<String>) asset.getOrDefault("modifiers", Collections.emptyList())));
            glossaryRaw.add(entry);
        }
        saveAsJson(glossaryRaw, outputDir.resolve(String.format("%s_v%s_glossary_raw_%s.json", frameworkName, safeVersion, dateStr)).toString());

        // Save per-module files
        moduleLibrary.forEach((modName, assets) -> {
            try {
                Map<String, Object> modContainer = new LinkedHashMap<>(rootContainer);
                modContainer.put("module", modName);
                modContainer.put("assets", assets);
                String modFileName = String.format("%s_v%s_%s_%s.json", frameworkName, safeVersion, modName, dateStr);
                saveAsJson(modContainer, outputDir.resolve(modFileName).toString());
            } catch (Exception e) { e.printStackTrace(); }
        });

        // Semantic dictionary
        generateSemanticDictionary(frameworkName, outputDir);

        // Print final report
        printReport();
    }

    /**
     * 提取 import 依赖列表
     */
    private static List<String> extractImportDependencies(CompilationUnit cu) {
        List<String> imports = new ArrayList<>();
        cu.getImports().forEach(importDecl -> imports.add(importDecl.getNameAsString()));
        return imports;
    }

    /**
     * 提取注解参数
     */
    private static List<Map<String, Object>> extractAnnotationParams(TypeDeclaration<?> type) {
        List<Map<String, Object>> annotations = new ArrayList<>();
        type.getAnnotations().forEach(annotation -> {
            Map<String, Object> ann = new LinkedHashMap<>();
            ann.put("name", annotation.getNameAsString());
            List<Map<String, String>> params = new ArrayList<>();
            annotation.getChildNodes().forEach(node -> {
                if (node instanceof com.github.javaparser.ast.expr.MemberValuePair) {
                    com.github.javaparser.ast.expr.MemberValuePair mvp = (com.github.javaparser.ast.expr.MemberValuePair) node;
                    Map<String, String> param = new LinkedHashMap<>();
                    param.put("key", mvp.getNameAsString());
                    param.put("value", mvp.getValue().toString());
                    params.add(param);
                }
            });
            if (!params.isEmpty()) ann.put("parameters", params);
            annotations.add(ann);
        });
        return annotations;
    }

    /**
     * 运行质量规则分析
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> runQualityAnalysis(
            List<Map<String, Object>> globalLibrary, RulesConfig rulesConfig,
            String projectRoot, List<Map<String, String>> globalDependencies) {
        List<Map<String, Object>> issues = new ArrayList<>();
        cn.dolphinmind.glossary.java.analyze.quality.RuleEngine engine =
                new cn.dolphinmind.glossary.java.analyze.quality.RuleEngine();
        java.util.function.Consumer<cn.dolphinmind.glossary.java.analyze.quality.QualityRule> reg = rule -> {
            if (rulesConfig.isRuleEnabled(rule.getRuleKey())) engine.registerRule(rule);
        };

        // Register all rules (BUG, CODE_SMELL, SECURITY, CFG, Taint)
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.EmptyCatchBlock());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.StringLiteralEquality());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.IdenticalOperand());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.ThreadRunDirect());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.WaitNotifyNoSync());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.MutableMembersReturned());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.FinalizerUsed());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.MissingSerialVersionUID());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.NullDereference());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.DeadStore());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.AssertSideEffect());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.LoopBranchUpdate());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.PublicStaticMutableField());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.DeprecatedUsage());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.EqualsOnArrays());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.BigDecimalDouble());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.ToStringReturnsNull());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.ClassLoaderMisuse());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.ExceptionRethrown());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.UncheckedCatch());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.UnclosedResource());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.InterruptedExceptionSwallowed());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.LongToIntCast());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.TooLongMethod());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.TooManyParameters());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.TooManyReturns());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.CyclomaticComplexity());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.PrintStackTrace());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.GodClass());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.MissingJavadoc());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.WildcardImport());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.SystemOutPrintln());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.TooManyConstructors());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.EmptyStatement());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.UnusedLocalVariable());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.TooManyStringLiterals());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.ExceptionIgnored());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.EqualsWithoutHashCode());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.StringConcatInLoop());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.EmptyMethodBody());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.BooleanLiteralInCondition());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.StringEqualsCaseSensitive());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.SensitiveToString());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.BooleanMethodName());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.MethodTooLongName());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.DOMParserXXE());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.ThreadSleepInCode());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.MagicNumber());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.OptionalParameter());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.OptionalGetWithoutCheck());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.StreamNotConsumed());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.OptionalField());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.SSLServerSocket());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.WeakRSAKey());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.AllocationInLoop());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.CatchingError());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.OptionalChaining());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.TLSProtocol());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.AutoboxingPerformance());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.WeakHashFunction());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.JWTWithoutExpiry());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.RegexComplexity());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.RegexLookaround());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.RegexDoS());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.InsecureRandom());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.NullCheckAfterDeref());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.LogInjection());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.HashWithoutSalt());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.InsecureCookie());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.HttpOnlyCookie());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.FilePermissionTooPermissive());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.RedundantCast());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.SerializableField());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.VariableDeclaredFarFromUsage());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.BigDecimalPrecisionLoss());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.ContentTypeSniffing());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.HardcodedPassword());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.SQLInjection());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.HardcodedIP());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.HTTPNotHTTPS());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.InsecureRandomGenerator());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.Deserialization());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.CommandInjection());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.WeakMAC());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.ReflectionOnSensitive());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.HardcodedSecretKey());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.SessionFixation());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.InsecureTempFile());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.LDAPInjection());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.PathTraversal());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.OpenRedirect());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.XXEInTransformerFactory());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.XPathInjection());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.AllRules.CORSMisconfiguration());
        int ccThreshold = rulesConfig.getEffectiveThreshold("RSPEC-3776-CFG", 15);
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.TrueCyclomaticComplexity(ccThreshold));
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.UnreachableCode());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.ResourceLeakPath());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.ExceptionHandlingPath());
        reg.accept(new cn.dolphinmind.glossary.java.analyze.quality.rules.TaintFlowRule());

        // Run rules
        List<cn.dolphinmind.glossary.java.analyze.quality.QualityIssue> ruleIssues = engine.run(globalLibrary);

        // Duplicate code detection
        cn.dolphinmind.glossary.java.analyze.quality.DuplicateCodeDetector duplicateDetector =
                new cn.dolphinmind.glossary.java.analyze.quality.DuplicateCodeDetector();
        ruleIssues.addAll(duplicateDetector.findDuplicates(globalLibrary));

        // Cross-method taint
        if (rulesConfig.isRuleEnabled("RSPEC-2076-XMETHOD")) {
            cn.dolphinmind.glossary.java.analyze.quality.rules.CrossMethodTaintRule crossMethodTaint =
                    new cn.dolphinmind.glossary.java.analyze.quality.rules.CrossMethodTaintRule();
            ruleIssues.addAll(crossMethodTaint.analyzeAll(globalLibrary, globalDependencies));
        }
        if (rulesConfig.isRuleEnabled("RSPEC-5135")) {
            cn.dolphinmind.glossary.java.analyze.quality.rules.SpringBeanAnalysisRule springRule =
                    new cn.dolphinmind.glossary.java.analyze.quality.rules.SpringBeanAnalysisRule();
            ruleIssues.addAll(springRule.analyzeAll(globalLibrary));
        }
        if (rulesConfig.isRuleEnabled("RSPEC-1200")) {
            cn.dolphinmind.glossary.java.analyze.quality.rules.ArchitectureViolationRule archRule =
                    new cn.dolphinmind.glossary.java.analyze.quality.rules.ArchitectureViolationRule();
            ruleIssues.addAll(archRule.analyzeAll(globalLibrary));
        }

        // Convert to Map
        for (cn.dolphinmind.glossary.java.analyze.quality.QualityIssue issue : ruleIssues) {
            issues.add(issue.toMap());
        }

        return issues;
    }

    /**
     * 构建质量分析摘要
     */
    private static Map<String, Object> buildQualitySummary(List<Map<String, Object>> issues) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_issues", issues.size());
        Map<String, Long> bySeverity = new LinkedHashMap<>();
        bySeverity.put("CRITICAL", issues.stream().filter(i -> "CRITICAL".equals(i.get("severity"))).count());
        bySeverity.put("MAJOR", issues.stream().filter(i -> "MAJOR".equals(i.get("severity"))).count());
        bySeverity.put("MINOR", issues.stream().filter(i -> "MINOR".equals(i.get("severity"))).count());
        summary.put("by_severity", bySeverity);
        Map<String, Long> byCategory = new LinkedHashMap<>();
        byCategory.put("BUG", issues.stream().filter(i -> "BUG".equals(i.get("category"))).count());
        byCategory.put("CODE_SMELL", issues.stream().filter(i -> "CODE_SMELL".equals(i.get("category"))).count());
        byCategory.put("SECURITY", issues.stream().filter(i -> "SECURITY".equals(i.get("category"))).count());
        summary.put("by_category", byCategory);
        return summary;
    }

    /**
     * 提取类内部的字段依赖关系
     */
    private static void extractDependencies(String sourceAddr, Map<String, Object> classAsset, List<Map<String, String>> deps) {
        // 1. 从 fields_matrix 中提取类型路径
        List<Map<String, Object>> fields = (List<Map<String, Object>>) classAsset.get("fields_matrix");
        if (fields != null) {
            for (Map<String, Object> field : fields) {
                String typePath = (String) field.get("type_path");
                if (typePath != null && !typePath.startsWith("java.") && !typePath.startsWith("javax.")) {
                    Map<String, String> dep = new LinkedHashMap<>();
                    dep.put("source", sourceAddr);
                    dep.put("target", typePath);
                    dep.put("type", "DEPENDS_ON");
                    deps.add(dep);
                }
            }
        }
        
        // 2. 从 hierarchy 中提取继承/实现关系 (作为特殊的依赖)
        Map<String, List<String>> hierarchy = (Map<String, List<String>>) classAsset.get("hierarchy");
        if (hierarchy != null) {
            if (hierarchy.get("extends") != null) {
                for (String target : hierarchy.get("extends")) {
                    Map<String, String> dep = new LinkedHashMap<>();
                    dep.put("source", sourceAddr);
                    dep.put("target", target);
                    dep.put("type", "EXTENDS");
                    deps.add(dep);
                }
            }
            if (hierarchy.get("implements") != null) {
                for (String target : hierarchy.get("implements")) {
                    Map<String, String> dep = new LinkedHashMap<>();
                    dep.put("source", sourceAddr);
                    dep.put("target", target);
                    dep.put("type", "IMPLEMENTS");
                    deps.add(dep);
                }
            }
        }
    }

    /**
     * 增强版类型处理：整合物理注释、修饰符、泛型、继承关系等完整元数据
     */
    private static Map<String, Object> processTypeEnhanced(TypeDeclaration<?> type, String pkg, String parentAddr, 
                                                            List<String> fileLines, ScannerContext ctx, List<Map<String, String>> globalDeps) {
        Map<String, Object> node = new LinkedHashMap<>();
        String address = (parentAddr == null) ? (pkg + "." + type.getNameAsString()) : (parentAddr + "$" + type.getNameAsString());

        node.put("address", address);
        node.put("kind", getKind(type));

        // 🚀 结构化注释提取 (类级别)
        Map<String, Object> classCommentDetails = extractCommentDetails(fileLines, type);
        node.put("description", classCommentDetails.getOrDefault("summary", ""));
        node.put("comment_details", classCommentDetails);

        node.put("source_file", type.findCompilationUnit().flatMap(CompilationUnit::getStorage).map(s -> s.getPath().toString()).orElse(""));
        node.put("modifiers", resolveMods(type.getModifiers()));
        node.put("class_generics", resolveTypeParameters(type));
        
        // 🚀 注入基于命名的组件角色标签
        java.util.Set<String> compRoles = extractComponentRole(type);
        node.put("component_tags", compRoles);
        
        // 🚀 注入基于 AST 的语义画像 (继承、接口、复杂度)
        node.put("semantic_profile", extractSemanticProfile(type, fileLines));
        
        // 🚀 注入推理引擎结论 (带证据锚定)
        node.put("reasoning_results", performLogicalInference(type, fileLines));
        
        // 🚀 注入架构与行为标签 (AI Agent 核心索引)
        node.put("arch_tags", resolveBilingualTags(extractArchTags(type, fileLines)));
        
        // 🚀 注入方法级语义分析 (完整方法列表，包含源码体)
        node.put("methods_full", resolveMethodsSemanticEnhanced(type, fileLines));

        // 🚀 注入域上下文与调用链 (认知地图的核心)
        node.put("domain_context", extractDomainContext(pkg));
        node.put("call_graph_summary", extractCallGraphSummary(type));

        // 迭代进化：记录未识别的类后缀
        if (compRoles.isEmpty()) {
            String className = type.getNameAsString();
            int dotIndex = className.lastIndexOf('.');
            String simpleName = dotIndex > -1 ? className.substring(dotIndex + 1) : className;
            // 提取最后一个大写字母后的部分作为后缀候选
            String suffix = simpleName.replaceAll("^[A-Z]+", "");
            if (suffix.length() > 2) {
                unrecognizedClassSuffixes.merge(suffix, 1, Integer::sum);
            }
        }

        // 🚀 注入方法意图标签与字段语义标签
        node.put("methods_intent", resolveMethodsEnhanced(type, fileLines));
        node.put("fields", resolveFieldsEnhanced(type));

        // 🚀 注入动态增强层 (场景案例与最佳实践)
        node.put("enhancement", getEnhancementData(address));

        // 🚀 注入 AI 教学指南 (约束与场景)
        node.put("ai_guidance", extractAIGuidance(type, fileLines));

        // 🚀 预留 AI 进化层存储空间
        node.put("ai_evolution", new LinkedHashMap<>());

        // 🚀 注入洞察摘要 (翻译后的核心描述)
        String rawDesc = bruteForceComment(fileLines, type);
        node.put("insight_summary", translateAndSummarize(rawDesc));

        if (type instanceof ClassOrInterfaceDeclaration) {
            node.put("hierarchy", resolveHierarchySemantic((ClassOrInterfaceDeclaration) type));
        }

        node.put("annotations", extractAnnos(type, address));

        // --- 字段角色化提取（业务分类）---
        Map<String, List<Map<String, Object>>> segments = new LinkedHashMap<>();
        segments.put("INTERNAL_STATE", new ArrayList<>());
        segments.put("INTERNAL_COMPONENT", new ArrayList<>());
        segments.put("EXTERNAL_SERVICE", new ArrayList<>());

        // 🚀 修复：使用 getFields() 确保在纯 AST 模式下也能获取到字段
        type.getFields().forEach(f -> {
            fieldCount.incrementAndGet();
            f.getVariables().forEach(v -> {
                Map<String, Object> fMeta = new LinkedHashMap<>();
                String fullType = v.getType().asString(); // 纯 AST 模式直接使用 asString()

                fMeta.put("name", v.getNameAsString());
                fMeta.put("address", address + "." + v.getNameAsString());
                fMeta.put("type_path", fullType);
                fMeta.put("description", bruteForceComment(fileLines, f));
                fMeta.put("modifiers", resolveMods(f.getModifiers()));

                segments.get(ctx.classify(fullType)).add(fMeta);
            });
        });
        node.put("field_segments", segments);
        
        // --- 字段矩阵（详细元数据）---
        node.put("fields_matrix", resolveFieldsMatrix(fileLines, type));

        // --- 方法提取（增强版）---
        List<Map<String, Object>> methods = new ArrayList<>();
        // 🚀 修复：使用 getConstructors() 和 getMethods() 替代 findAll(DIRECT_CHILDREN)
        type.getConstructors().forEach(c -> {
            methodCount.incrementAndGet();
            methods.add(extractMethodEnhanced(c, address + "#<init>", c.getParameters(), fileLines, address, globalDeps));
        });
        type.getMethods().forEach(m -> {
            methodCount.incrementAndGet();
            allMethodNames.add(m.getNameAsString()); // 收集方法名用于分析
            methods.add(extractMethodEnhanced(m, address + "#" + m.getNameAsString(), m.getParameters(), fileLines, address, globalDeps));
        });
        node.put("methods", methods);
        
        // --- 构造函数和方法矩阵（对齐格式）---
        node.put("constructor_matrix", resolveConstructorsAligned(fileLines, type, address));
        node.put("method_matrix", resolveMethodsAligned(fileLines, type, address));

        // 🚀 修复：递归处理内部类并收集结果到返回值的 inner_classes 字段
        List<Map<String, Object>> innerClasses = new ArrayList<>();
        type.getMembers().stream().filter(m -> m instanceof TypeDeclaration)
                .forEach(m -> {
                    Map<String, Object> innerClass = processTypeEnhanced((TypeDeclaration<?>) m, pkg, address, fileLines, ctx, globalDeps);
                    if (innerClass != null) {
                        classCount.incrementAndGet();
                        innerClasses.add(innerClass);
                    }
                });
        node.put("inner_classes", innerClasses);

        return node;
    }

    /**
     * 增强版方法提取：整合物理注释、修饰符、泛型、返回值、异常、调用链等完整信息
     */
    private static Map<String, Object> extractMethodEnhanced(CallableDeclaration<?> d, String baseAddr,
                                                              NodeList<Parameter> params, List<String> fileLines, String classAddr, List<Map<String, String>> globalDeps) {
        Map<String, Object> m = new LinkedHashMap<>();
        String fullAddr = baseAddr + "(" + params.stream().map(p -> p.getType().asString()).collect(Collectors.joining(",")) + ")";

        m.put("address", fullAddr);
        m.put("name", d.getNameAsString());

        // 🚀 结构化注释提取 (替代 bruteForceComment)
        Map<String, Object> commentDetails = extractCommentDetails(fileLines, d);
        m.put("description", commentDetails.getOrDefault("summary", ""));
        m.put("comment_details", commentDetails);

        m.put("modifiers", resolveMods(d.getModifiers()));
        m.put("line_start", d.getBegin().map(p -> p.line).orElse(0));
        m.put("line_end", d.getEnd().map(p -> p.line).orElse(0));
        m.put("signature", d.getDeclarationAsString(false, false, false));

        // 🚀 方法源码体提取
        m.put("source_code", extractNodeSource(fileLines, d, true));
        m.put("body_code", extractCallableBody(d));
        m.put("code_summary", summarizeMethodBody(d));
        m.put("key_statements", extractKeyStatements(d));
        m.put("line_count", calculateLineCount(d));

        // 🚀 注入智能标签
        m.put("tags", extractMethodTags(d.getNameAsString(), d instanceof MethodDeclaration ? ((MethodDeclaration)d).getType().asString() : "void"));

        if (d instanceof MethodDeclaration) {
            MethodDeclaration md = (MethodDeclaration) d;
            m.put("is_override", checkIsOverride(md));
            m.put("method_generics", md.getTypeParameters().stream().map(tp -> tp.asString()).collect(Collectors.toList()));
            m.put("return_type_path", getSemanticPath(md.getType()));
            m.put("throws_matrix", md.getThrownExceptions().stream().map(SourceUniversePro::getSemanticPath).collect(Collectors.toList()));
        }

        m.put("internal_throws", d.findAll(ThrowStmt.class).stream().map(t -> t.getExpression().toString()).distinct().collect(Collectors.toList()));
        m.put("parameters_inventory", resolveParametersInventory(params));

        // 🚀 核心增强：提取方法调用链并注入全局依赖
        extractMethodCalls(d, classAddr, fullAddr, globalDeps);

        return m;
    }

    /**
     * 提取方法内部的所有调用关系，并注入全局依赖列表
     * 🚀 优化：只保留 public/protected 方法的跨类调用，减少噪音
     */
    private static void extractMethodCalls(CallableDeclaration<?> method, String classAddr, String fullAddr, List<Map<String, String>> globalDeps) {
        // 🚩 过滤 1: 只处理 public 或 protected 方法（核心 API）
        boolean isPublic = method.getModifiers().stream()
                .anyMatch(m -> m.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.PUBLIC);
        boolean isProtected = method.getModifiers().stream()
                .anyMatch(m -> m.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.PROTECTED);

        if (!isPublic && !isProtected) {
            return; // 忽略 private/default 方法的内部调用
        }

        method.findAll(MethodCallExpr.class).forEach(call -> {
            try {
                // 利用 JavaSymbolSolver 解析被调用的方法声明
                com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration resolved = call.resolve();
                String targetClass = resolved.declaringType().getQualifiedName();
                String targetMethod = resolved.getName();

                // 🚩 过滤 2: 忽略 JDK、Javax、SLF4J 等外部库
                if (targetClass.startsWith("java.") || targetClass.startsWith("javax.") ||
                    targetClass.startsWith("jdk.") || targetClass.startsWith("org.slf4j") ||
                    targetClass.startsWith("org.apache.log4j")) {
                    return;
                }

                // 🚩 过滤 3: 忽略当前类内部的自调用（聚焦跨组件交互）
                String sourceClass = classAddr.split("#")[0];
                if (targetClass.equals(sourceClass)) {
                    return;
                }

                // 构建目标方法地址
                String targetAddr = targetClass + "#" + targetMethod;

                // 记录 CALLS 依赖到全局列表（去重）
                String depKey = fullAddr + "→" + targetAddr + ":CALLS";
                if (!seenDependencies.contains(depKey)) {
                    seenDependencies.add(depKey);
                    Map<String, String> callDep = new LinkedHashMap<>();
                    callDep.put("source", fullAddr);
                    callDep.put("target", targetAddr);
                    callDep.put("type", "CALLS");
                    globalDeps.add(callDep);
                }
            } catch (Exception ignored) {
                // 忽略无法解析的调用（如动态代理、Lambda、反射等）
            }
        });
    }

    /**
     * 提取方法体的源码文本 (带行号范围)
     */
    private static String extractCallableBody(CallableDeclaration<?> d) {
        if (d instanceof MethodDeclaration) {
            java.util.Optional<com.github.javaparser.ast.stmt.BlockStmt> opt = ((MethodDeclaration) d).getBody();
            if (!opt.isPresent()) return "";
            return opt.get().toString();
        } else if (d instanceof ConstructorDeclaration) {
            com.github.javaparser.ast.stmt.BlockStmt body = ((ConstructorDeclaration) d).getBody();
            if (body == null) return "";
            return body.toString();
        }
        return "";
    }

    /**
     * 计算方法体的行数
     */
    private static int calculateLineCount(CallableDeclaration<?> d) {
        String body = extractCallableBody(d);
        if (body.isEmpty()) return 0;
        return body.split("\n").length;
    }

    /**
     * 获取方法体 BlockStmt (仅供内部方法使用)
     */
    private static com.github.javaparser.ast.stmt.BlockStmt getCallableBody(CallableDeclaration<?> d) {
        if (d instanceof MethodDeclaration) {
            return ((MethodDeclaration) d).getBody().orElse(null);
        }
        if (d instanceof ConstructorDeclaration) {
            return ((ConstructorDeclaration) d).getBody();
        }
        return null;
    }

    /**
     * 提取方法体中的关键语句 (if/throw/return/调用外部服务等)
     */
    private static List<Map<String, String>> extractKeyStatements(CallableDeclaration<?> d) {
        List<Map<String, String>> statements = new ArrayList<>();
        com.github.javaparser.ast.stmt.BlockStmt body = getCallableBody(d);
        if (body == null) return statements;

        // 1. 提取 if 条件分支
        body.findAll(com.github.javaparser.ast.stmt.IfStmt.class).forEach(ifStmt -> {
            Map<String, String> stmt = new LinkedHashMap<>();
            stmt.put("type", "CONDITION");
            stmt.put("condition", ifStmt.getCondition().toString());
            stmt.put("line", ifStmt.getBegin().map(p -> p.line).orElse(0) + "");
            statements.add(stmt);
        });

        // 2. 提取 throw 语句
        body.findAll(ThrowStmt.class).forEach(throwStmt -> {
            Map<String, String> stmt = new LinkedHashMap<>();
            stmt.put("type", "THROW");
            stmt.put("exception", throwStmt.getExpression().toString());
            stmt.put("line", throwStmt.getBegin().map(p -> p.line).orElse(0) + "");
            statements.add(stmt);
        });

        // 3. 提取 return 语句
        body.findAll(com.github.javaparser.ast.stmt.ReturnStmt.class).forEach(retStmt -> {
            Map<String, String> stmt = new LinkedHashMap<>();
            stmt.put("type", "RETURN");
            stmt.put("value", retStmt.getExpression().map(Object::toString).orElse("void"));
            stmt.put("line", retStmt.getBegin().map(p -> p.line).orElse(0) + "");
            statements.add(stmt);
        });

        // 4. 提取外部服务调用 (方法调用)
        body.findAll(MethodCallExpr.class).forEach(call -> {
            try {
                String scope = call.getScope().map(Object::toString).orElse("");
                if (!scope.isEmpty() && !scope.equals("this") && !scope.equals("super")) {
                    Map<String, String> stmt = new LinkedHashMap<>();
                    stmt.put("type", "EXTERNAL_CALL");
                    stmt.put("target", scope + "." + call.getNameAsString());
                    stmt.put("line", call.getBegin().map(p -> p.line).orElse(0) + "");
                    statements.add(stmt);
                }
            } catch (Exception ignored) {
                // 忽略无法解析的调用
            }
        });

        // 5. 提取同步块
        body.findAll(com.github.javaparser.ast.stmt.SynchronizedStmt.class).forEach(syncStmt -> {
            Map<String, String> stmt = new LinkedHashMap<>();
            stmt.put("type", "SYNCHRONIZED");
            stmt.put("expression", syncStmt.getExpression().toString());
            stmt.put("line", syncStmt.getBegin().map(p -> p.line).orElse(0) + "");
            statements.add(stmt);
        });

        return statements;
    }

    /**
     * 方法体摘要：提取方法体的业务语义摘要
     */
    private static String summarizeMethodBody(CallableDeclaration<?> d) {
        com.github.javaparser.ast.stmt.BlockStmt body = getCallableBody(d);
        if (body == null) return "无方法体 (abstract/native)";
        List<String> summaries = new ArrayList<>();

        // 1. 检测异常处理
        if (!body.findAll(com.github.javaparser.ast.stmt.CatchClause.class).isEmpty()) {
            summaries.add("包含异常处理逻辑");
        }

        // 2. 检测条件分支
        int ifCount = body.findAll(com.github.javaparser.ast.stmt.IfStmt.class).size();
        if (ifCount > 0) {
            summaries.add(ifCount + " 个条件分支");
        }

        // 3. 检测循环
        int loopCount = body.findAll(com.github.javaparser.ast.stmt.ForEachStmt.class).size() +
                        body.findAll(com.github.javaparser.ast.stmt.WhileStmt.class).size() +
                        body.findAll(com.github.javaparser.ast.stmt.ForStmt.class).size();
        if (loopCount > 0) {
            summaries.add(loopCount + " 个循环结构");
        }

        // 4. 检测方法调用
        int callCount = body.findAll(MethodCallExpr.class).size();
        if (callCount > 0) {
            summaries.add(callCount + " 次方法调用");
        }

        // 5. 检测同步
        if (!body.findAll(com.github.javaparser.ast.stmt.SynchronizedStmt.class).isEmpty()) {
            summaries.add("使用同步块");
        }

        // 6. 检测返回
        int returnCount = body.findAll(com.github.javaparser.ast.stmt.ReturnStmt.class).size();
        if (returnCount > 0) {
            summaries.add(returnCount + " 个返回点");
        }

        if (summaries.isEmpty()) {
            return "简单方法体";
        }
        return String.join(", ", summaries);
    }

    /**
     * 物理注释提取：从源码行中暴力抓取注释块
     */
    private static String bruteForceComment(List<String> lines, Node node) {
        Map<String, Object> comment = extractCommentDetails(lines, node);
        return Objects.toString(comment.getOrDefault("summary", ""), "");
    }

    /**
     * 清理注释文本：移除标记符号和多余空白
     */
    private static String cleanText(String text) {
        if (text == null) return "";
        return text.replaceAll("/\\*\\*|\\*/|\\*|/\\*", "")
                .replaceAll("<[^>]*>", "")
                .replaceAll("\\r", "")
                .replaceAll("\\s+", " ").trim();
    }

    /**
     * 结构化注释提取：提取 Javadoc 的各个部分
     */
    private static Map<String, Object> extractCommentDetails(List<String> lines, Node node) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "");
        result.put("description", "");
        result.put("params", new ArrayList<Map<String, String>>());
        result.put("return_description", "");
        result.put("throws", new ArrayList<Map<String, String>>());
        result.put("deprecated", "");
        result.put("since", "");
        result.put("author", "");
        result.put("see", new ArrayList<String>());
        result.put("semantic_notes", new ArrayList<String>());
        result.put("raw_comment", "");

        if (node == null) return result;

        // 1. 获取节点关联的注释
        Optional<Comment> commentOpt = node.getComment();
        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();
            String rawComment = comment.getContent();
            result.put("raw_comment", rawComment);

            // 清理并提取主描述
            String cleaned = cleanText(rawComment);
            // 提取第一句作为摘要
            int dotIndex = cleaned.indexOf('.');
            if (dotIndex > 0) {
                result.put("summary", cleaned.substring(0, dotIndex + 1).trim());
            } else {
                result.put("summary", cleaned.length() > 100 ? cleaned.substring(0, 100) + "..." : cleaned);
            }
            result.put("description", cleaned);

            // 提取 Javadoc 标签
            extractJavadocTags(rawComment, result);

            // 提取语义关键词
            extractSemanticNotes(cleaned, result);
        }

        // 2. 如果没有关联注释，尝试向上暴力查找
        if (result.get("raw_comment").toString().isEmpty()) {
            String bfComment = bruteForceCommentFromLines(lines, node);
            if (!bfComment.isEmpty()) {
                result.put("raw_comment", bfComment);
                String cleaned = cleanText(bfComment);
                result.put("summary", cleaned.length() > 100 ? cleaned.substring(0, 100) + "..." : cleaned);
                result.put("description", cleaned);
                extractJavadocTags(bfComment, result);
                extractSemanticNotes(cleaned, result);
            }
        }

        return result;
    }

    /**
     * 从原始注释文本中提取 Javadoc 标签 (@param, @return, @throws, etc.)
     */
    private static void extractJavadocTags(String rawComment, Map<String, Object> result) {
        String[] lines = rawComment.split("\n");
        List<Map<String, String>> params = (List<Map<String, String>>) result.get("params");
        List<Map<String, String>> throwsList = (List<Map<String, String>>) result.get("throws");
        List<String> seeList = (List<String>) result.get("see");

        for (String line : lines) {
            line = line.trim().replaceFirst("^\\*+", "").trim();

            // @param
            if (line.startsWith("@param")) {
                Map<String, String> param = new LinkedHashMap<>();
                String rest = line.substring(6).trim();
                int spaceIdx = rest.indexOf(' ');
                if (spaceIdx > 0) {
                    param.put("name", rest.substring(0, spaceIdx));
                    param.put("description", rest.substring(spaceIdx + 1).trim());
                } else {
                    param.put("name", rest);
                    param.put("description", "");
                }
                params.add(param);
            }
            // @return
            else if (line.startsWith("@return")) {
                result.put("return_description", line.substring(7).trim());
            }
            // @throws / @exception
            else if (line.startsWith("@throws") || line.startsWith("@exception")) {
                Map<String, String> throwsEntry = new LinkedHashMap<>();
                String[] parts = line.split("\\s+", 2);
                if (parts.length > 1) {
                    String rest = parts[1];
                    int spaceIdx = rest.indexOf(' ');
                    if (spaceIdx > 0) {
                        throwsEntry.put("exception", rest.substring(0, spaceIdx));
                        throwsEntry.put("description", rest.substring(spaceIdx + 1).trim());
                    } else {
                        throwsEntry.put("exception", rest);
                        throwsEntry.put("description", "");
                    }
                } else {
                    throwsEntry.put("exception", "");
                    throwsEntry.put("description", "");
                }
                throwsList.add(throwsEntry);
            }
            // @deprecated
            else if (line.startsWith("@deprecated")) {
                result.put("deprecated", line.substring(11).trim());
            }
            // @since
            else if (line.startsWith("@since")) {
                result.put("since", line.substring(6).trim());
            }
            // @author
            else if (line.startsWith("@author")) {
                result.put("author", line.substring(7).trim());
            }
            // @see
            else if (line.startsWith("@see")) {
                seeList.add(line.substring(4).trim());
            }
        }
    }

    /**
     * 从注释中提取语义说明 (线程安全、性能、约束等)
     */
    private static void extractSemanticNotes(String cleanedComment, Map<String, Object> result) {
        List<String> semanticNotes = (List<String>) result.get("semantic_notes");
        String lowerComment = cleanedComment.toLowerCase();

        if (lowerComment.contains("thread-safe") || lowerComment.contains("线程安全")) {
            semanticNotes.add("Thread-Safe");
        }
        if (lowerComment.contains("not thread-safe") || lowerComment.contains("非线程安全")) {
            semanticNotes.add("Not Thread-Safe");
        }
        if (lowerComment.contains("must be closed") || lowerComment.contains("必须关闭")) {
            semanticNotes.add("Must Be Closed");
        }
        if (lowerComment.contains("deprecated") || lowerComment.contains("废弃")) {
            semanticNotes.add("Deprecated");
        }
        if (lowerComment.contains("for internal use only") || lowerComment.contains("仅供内部使用")) {
            semanticNotes.add("Internal Use Only");
        }
        if (lowerComment.contains("do not call") || lowerComment.contains("不要调用")) {
            semanticNotes.add("Do Not Call Directly");
        }
        if (lowerComment.contains("idempotent") || lowerComment.contains("幂等")) {
            semanticNotes.add("Idempotent");
        }
        if (lowerComment.contains("nullable") || lowerComment.contains("可为空")) {
            semanticNotes.add("Nullable");
        }
        if (lowerComment.contains("not null") || lowerComment.contains("不能为空")) {
            semanticNotes.add("Not Null");
        }
        if (lowerComment.contains("synchronized") || lowerComment.contains("同步")) {
            semanticNotes.add("Synchronized");
        }
        if (lowerComment.contains("performance") || lowerComment.contains("性能")) {
            semanticNotes.add("Performance Note");
        }
    }

    /**
     * 从源码行中暴力查找注释块 (向上查找)
     */
    private static String bruteForceCommentFromLines(List<String> lines, Node node) {
        if (node == null || !node.getBegin().isPresent()) return "";
        int startLine = node.getBegin().get().line - 1; // 0-indexed

        StringBuilder sb = new StringBuilder();
        boolean inComment = false;
        boolean found = false;

        // 向上查找 10 行
        for (int i = startLine; i >= Math.max(0, startLine - 10); i--) {
            String line = lines.get(i).trim();
            if (line.startsWith("*/")) {
                inComment = true;
                found = true;
                continue;
            }
            if (found) {
                if (line.startsWith("/**") || line.startsWith("/*")) {
                    sb.insert(0, line);
                    break;
                }
                sb.insert(0, line + "\n");
            }
        }

        return sb.toString();
    }

    /**
     * 从源码行中提取节点的源码文本
     */
    private static String extractNodeSource(List<String> fileLines, Node node, boolean includeBody) {
        if (node == null || !node.getBegin().isPresent() || !node.getEnd().isPresent()) return "";
        int startLine = node.getBegin().get().line - 1; // 0-indexed
        int endLine = node.getEnd().get().line - 1;

        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(0, startLine); i <= Math.min(endLine, fileLines.size() - 1); i++) {
            sb.append(fileLines.get(i)).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 扫描 Java 项目中的非 Java 文件资产 (支持多模块)
     */
    private static Map<String, Object> scanProjectFiles(Path projectRoot) {
        Map<String, Object> assets = new LinkedHashMap<>();

        try {
            cn.dolphinmind.glossary.java.analyze.scanner.ProjectScanner scanner =
                    new cn.dolphinmind.glossary.java.analyze.scanner.ProjectScanner(projectRoot);

            // Detect multi-module structure
            List<Path> modules = scanner.detectModules();
            System.out.println("📦 检测到 " + modules.size() + " 个模块");

            // Scan each module
            for (Path moduleRoot : modules) {
                String moduleName = moduleRoot.getFileName().toString();
                System.out.println("  🔍 扫描模块: " + moduleName);
                scanner.scanModule(moduleRoot);
            }

            // Group by type
            for (Map.Entry<cn.dolphinmind.glossary.java.analyze.parser.FileAsset.AssetType,
                         List<cn.dolphinmind.glossary.java.analyze.parser.FileAsset>> entry :
                    scanner.getAssetsByType().entrySet()) {
                List<Map<String, Object>> typeAssets = new ArrayList<>();
                for (cn.dolphinmind.glossary.java.analyze.parser.FileAsset asset : entry.getValue()) {
                    typeAssets.add(asset.toMap());
                }
                assets.put(entry.getKey().name().toLowerCase(), typeAssets);
            }

            // Add module info and summary
            assets.put("modules", modules.stream().map(Path::toString).collect(java.util.stream.Collectors.toList()));
            assets.put("scan_summary", scanner.getSummary());
            assets.put("errors", scanner.getScanErrors());

        } catch (IOException e) {
            System.err.println("⚠️ 扫描项目文件失败: " + e.getMessage());
        }

        return assets;
    }

    /**
     * 字段矩阵：提取字段的完整元数据
     */
    private static List<Map<String, Object>> resolveFieldsMatrix(List<String> lines, TypeDeclaration<?> t) {
        List<Map<String, Object>> fields = new ArrayList<>();
        t.getFields().forEach(f -> {
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

    /**
     * 方法矩阵：对齐格式的方法元数据
     */
    private static List<Map<String, Object>> resolveMethodsAligned(List<String> lines, TypeDeclaration<?> t, String addr) {
        return t.getMethods().stream().map(m -> {
            methodCount.incrementAndGet();
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("address", addr + "#" + m.getNameAsString());
            node.put("description", bruteForceComment(lines, m));
            node.put("modifiers", resolveMods(m.getModifiers()));
            node.put("is_override", checkIsOverride(m));
            node.put("method_generics", m.getTypeParameters().stream().map(tp -> tp.asString()).collect(Collectors.toList()));
            node.put("return_type_path", getSemanticPath(m.getType()));
            node.put("throws_matrix", m.getThrownExceptions().stream().map(SourceUniversePro::getSemanticPath).collect(Collectors.toList()));
            node.put("parameters_inventory", resolveParametersInventory(m.getParameters()));
            return node;
        }).collect(Collectors.toList());
    }

    /**
     * 构造函数矩阵：对齐格式的构造函数元数据
     */
    private static List<Map<String, Object>> resolveConstructorsAligned(List<String> lines, TypeDeclaration<?> t, String addr) {
        return t.getConstructors().stream().map(c -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("address", addr + "#<init>_" + System.identityHashCode(c));
            node.put("description", bruteForceComment(lines, c));
            node.put("parameters_inventory", resolveParametersInventory(c.getParameters()));
            return node;
        }).collect(Collectors.toList());
    }

    /**
     * 参数清单：提取方法/构造函数的参数信息
     */
    private static List<Map<String, String>> resolveParametersInventory(NodeList<Parameter> parameters) {
        return parameters.stream().map(p -> {
            Map<String, String> pMap = new LinkedHashMap<>();
            pMap.put("name", p.getNameAsString());
            pMap.put("type_path", getSemanticPath(p.getType()));
            return pMap;
        }).collect(Collectors.toList());
    }

    /**
     * 语义路径解析：尝试解析类型的完全限定名
     * 降级机制：当符号解析失败时，回退到 AST 结构的启发式推断
     */
    private static String getSemanticPath(Type type) {
        try {
            return type.resolve().describe();
        } catch (Exception e) {
            // Fallback 1: Try to infer from import statements
            String typeName = type.asString();
            if (!typeName.contains(".") && typeName.matches("[A-Z]\\w*")) {
                // Likely a simple class name - return as-is, will be resolved later
                return typeName;
            }
            // Fallback 2: Use AST structure (e.g., generic types)
            return typeName;
        }
    }

    /**
     * 检查方法是否为重写方法
     */
    private static boolean checkIsOverride(MethodDeclaration m) {
        try { return m.resolve().getQualifiedSignature().contains("Override") || m.getAnnotationByClass(Override.class).isPresent(); }
        catch (Exception e) { return m.getAnnotationByClass(Override.class).isPresent(); }
    }

    /**
     * 继承关系语义：提取 extends 和 implements
     */
    private static Map<String, List<String>> resolveHierarchySemantic(ClassOrInterfaceDeclaration cid) {
        Map<String, List<String>> h = new LinkedHashMap<>();
        h.put("extends", cid.getExtendedTypes().stream().map(SourceUniversePro::getSemanticPath).collect(Collectors.toList()));
        h.put("implements", cid.getImplementedTypes().stream().map(SourceUniversePro::getSemanticPath).collect(Collectors.toList()));
        return h;
    }

    /**
     * 修饰符解析
     */
    private static List<String> resolveMods(NodeList<com.github.javaparser.ast.Modifier> modifiers) {
        return modifiers.stream().map(m -> m.getKeyword().asString()).collect(Collectors.toList());
    }

    /**
     * 泛型参数解析
     */
    private static List<String> resolveTypeParameters(TypeDeclaration<?> t) {
        if (t instanceof ClassOrInterfaceDeclaration) {
            return ((ClassOrInterfaceDeclaration) t).getTypeParameters().stream()
                    .map(tp -> tp.asString()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 从包名提取模块名 (例如: org.springframework.core -> spring-core)
     */
    private static String extractModuleName(String pkg) {
        // 针对 JDK: java.util.concurrent -> java, javax.swing -> javax, jdk.internal -> jdk, sun.misc -> sun, com.sun.tracing -> com-sun
        if (pkg.startsWith("java.") || pkg.startsWith("javax.") || pkg.startsWith("jdk.") || pkg.startsWith("sun.")) {
            int dotIndex = pkg.indexOf('.');
            return dotIndex > 0 ? pkg.substring(0, dotIndex) : pkg;
        }
        if (pkg.startsWith("com.sun.")) {
            return "com-sun";
        }
        // 针对 Tomcat: org.apache.catalina.core -> core, org.apache.catalina.connector -> connector
        if (pkg.startsWith("org.apache.catalina.")) {
            String sub = pkg.substring("org.apache.catalina.".length());
            int idx = sub.indexOf('.');
            return idx != -1 ? sub.substring(0, idx) : sub;
        }
        // 针对 Spring Cloud: org.springframework.cloud.client -> client, org.springframework.cloud.context -> context
        if (pkg.startsWith("org.springframework.cloud.")) {
            String sub = pkg.substring("org.springframework.cloud.".length());
            int idx = sub.indexOf('.');
            return idx != -1 ? sub.substring(0, idx) : sub;
        }
        // 针对 Spring Boot: org.springframework.boot.autoconfigure -> autoconfigure, org.springframework.boot.context -> context
        if (pkg.startsWith("org.springframework.boot.")) {
            String sub = pkg.substring("org.springframework.boot.".length());
            int idx = sub.indexOf('.');
            return idx != -1 ? sub.substring(0, idx) : sub;
        }
        // 针对 Netty: io.netty.buffer -> buffer, io.netty.channel -> channel, io.netty.handler -> handler
        if (pkg.startsWith("io.netty.")) {
            String sub = pkg.substring("io.netty.".length());
            int idx = sub.indexOf('.');
            return idx != -1 ? sub.substring(0, idx) : sub;
        }
        // 针对 Dubbo: org.apache.dubbo.rpc.cluster -> rpc
        if (pkg.startsWith("org.apache.dubbo.")) {
            String sub = pkg.substring("org.apache.dubbo.".length());
            int idx = sub.indexOf('.');
            return idx != -1 ? sub.substring(0, idx) : sub;
        }
        // 针对 RocketMQ: org.apache.rocketmq.remoting.netty -> remoting
        if (pkg.startsWith("org.apache.rocketmq.")) {
            String sub = pkg.substring("org.apache.rocketmq.".length());
            int idx = sub.indexOf('.');
            return idx != -1 ? sub.substring(0, idx) : sub;
        }
        // 🚀 针对 MyBatis: org.apache.ibatis.executor -> executor, org.apache.ibatis.mapping -> mapping
        if (pkg.startsWith("org.apache.ibatis.")) {
            String sub = pkg.substring("org.apache.ibatis.".length());
            int idx = sub.indexOf('.');
            return idx != -1 ? sub.substring(0, idx) : sub;
        }
        // 🚀 针对 Redisson: org.redisson.api.RMap -> api
        if (pkg.startsWith("org.redisson.")) {
            String sub = pkg.substring("org.redisson.".length());
            int idx = sub.indexOf('.');
            return idx != -1 ? sub.substring(0, idx) : sub;
        }
        // 🚀 针对 HikariCP: com.zaxxer.hikari.pool -> pool, com.zaxxer.hikari.util -> util
        if (pkg.startsWith("com.zaxxer.hikari.")) {
            String sub = pkg.substring("com.zaxxer.hikari.".length());
            int idx = sub.indexOf('.');
            return idx != -1 ? sub.substring(0, idx) : sub;
        }
        // 针对 Spring: org.springframework.core -> spring-core
        if (pkg.startsWith("org.springframework.")) {
            String sub = pkg.substring("org.springframework.".length());
            int dotIndex = sub.indexOf('.');
            return "spring-" + (dotIndex > 0 ? sub.substring(0, dotIndex) : sub);
        }
        return "other";
    }

    /**
     * 🚀 标签映射：将 ID 转换为中英双语对象
     */
    private static java.util.List<Map<String, String>> resolveBilingualTags(java.util.Set<String> tagIds) {
        java.util.List<Map<String, String>> result = new ArrayList<>();
        for (String id : tagIds) {
            String[] info = TAG_LIBRARY.getOrDefault(id, new String[]{id, id, "未知标签"});
            Map<String, String> tagObj = new LinkedHashMap<>();
            tagObj.put("id", id);
            tagObj.put("cn", info[0]);
            tagObj.put("en", info[1]);
            tagObj.put("desc", info[2]);
            result.add(tagObj);
        }
        return result;
    }

    /**
     * 🚀 方法级语义增强：使用完整方法提取（包含源码体/注释/调用链）
     */
    private static java.util.List<Map<String, Object>> resolveMethodsSemanticEnhanced(TypeDeclaration<?> type, List<String> fileLines) {
        java.util.List<Map<String, Object>> methods = new ArrayList<>();
        String address = (String) type.findCompilationUnit()
                .flatMap(cu -> cu.getPackageDeclaration())
                .map(pd -> pd.getNameAsString() + "." + type.getNameAsString())
                .orElse(type.getNameAsString());

        // 使用 extractMethodEnhanced 提取完整方法信息
        java.util.List<Map<String, String>> globalDeps = new ArrayList<>();
        type.getMethods().forEach(method -> {
            methodCount.incrementAndGet();
            Map<String, Object> m = extractMethodEnhanced(method, address + "#" + method.getNameAsString(),
                    method.getParameters(), fileLines, address, globalDeps);

            // 额外添加语义标签
            java.util.Set<String> semanticTags = new java.util.HashSet<>();
            int complexity = 1;
            complexity += method.findAll(com.github.javaparser.ast.stmt.IfStmt.class).size();
            complexity += method.findAll(com.github.javaparser.ast.stmt.ForEachStmt.class).size();
            complexity += method.findAll(com.github.javaparser.ast.stmt.WhileStmt.class).size();
            complexity += method.findAll(com.github.javaparser.ast.stmt.CatchClause.class).size();
            if (complexity > 10) semanticTags.add("HighComplexity");
            if (!method.findAll(MethodCallExpr.class).isEmpty()) semanticTags.add("InteractionHeavy");
            if (method.getModifiers().contains(Modifier.synchronizedModifier())) semanticTags.add("ThreadSafe");

            m.put("semantic_tags", resolveBilingualTags(semanticTags));
            m.put("complexity_score", complexity);
            methods.add(m);
        });
        return methods;
    }

    /**
     * 🚀 提取架构与行为标签 (AI Agent 核心索引)
     */
    private static java.util.Set<String> extractArchTags(TypeDeclaration<?> type, List<String> fileLines) {
        java.util.Set<String> tags = new java.util.HashSet<>();
        String className = type.getNameAsString().toLowerCase();
        
        // 1. 接口身份识别 (Interface Identity)
        if (type instanceof ClassOrInterfaceDeclaration) {
            ((ClassOrInterfaceDeclaration) type).getImplementedTypes().forEach(t -> {
                String iface = t.getNameAsString().toLowerCase();
                if (iface.contains("serializable")) tags.add("StateTransferable");
                if (iface.contains("listener") || iface.contains("observer")) tags.add("EventObserver");
                if (iface.contains("runnable") || iface.contains("callable")) tags.add("ExecutableTask");
                if (iface.contains("comparable")) tags.add("Sortable");
                if (iface.contains("cloneable")) tags.add("Cloneable");
                if (iface.contains("closeable") || iface.contains("autocloseable")) tags.add("ResourceManaged");
            });
        }

        // 2. 并发与线程安全 (Concurrency & Thread Safety)
        boolean hasSynchronized = type.getMethods().stream()
                .anyMatch(m -> m.getModifiers().contains(Modifier.synchronizedModifier()));
        if (hasSynchronized) tags.add("ThreadSafe");
        
        boolean hasVolatile = type.getFields().stream()
                .anyMatch(f -> f.getModifiers().contains(Modifier.volatileModifier()));
        if (hasVolatile) tags.add("ConcurrentState");

        // 3. 设计模式启发式 (Design Pattern Heuristics)
        if (className.endsWith("factory") || className.endsWith("builder")) tags.add("CreationalPattern");
        if (className.endsWith("adapter") || className.endsWith("wrapper")) tags.add("StructuralPattern");
        if (className.endsWith("strategy") || className.endsWith("handler")) tags.add("BehavioralPattern");
        if (className.endsWith("singleton")) tags.add("Singleton");

        // 4. 领域上下文 (Domain Context via Imports)
        // 简单通过包名或类名判断
        if (className.contains("jdbc") || className.contains("sql")) tags.add("JDBC-Intensive");
        if (className.contains("netty") || className.contains("nio")) tags.add("NIO-Based");
        if (className.contains("security") || className.contains("auth")) tags.add("SecurityRelated");

        return tags;
    }

    /**
     * 🚀 智能翻译与摘要：基于技术指令集的精准中英映射
     */
    private static String translateAndSummarize(String text) {
        if (text == null || text.isEmpty()) return "暂无描述";
        
        // 1. 术语精准替换 (优先使用 tech-instruction-set.json)
        String translated = text;
        // 按单词长度降序排列，优先匹配长词（如先匹配 synchronization 再匹配 sync）
        List<String> sortedKeys = techInstructionSet.keySet().stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .collect(Collectors.toList());

        for (String key : sortedKeys) {
            // 使用正则进行全字匹配，忽略大小写
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(key) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(translated);
            if (matcher.find()) {
                String cnTerm = techInstructionSet.get(key);
                // 格式：中文(英文原词)
                translated = matcher.replaceAll(cnTerm + "(" + key + ")");
            }
        }
        
        // 2. 提取第一句作为摘要
        int dotIndex = translated.indexOf('.');
        if (dotIndex > 0) {
            return translated.substring(0, dotIndex + 1);
        }
        return translated.length() > 80 ? translated.substring(0, 80) + "..." : translated;
    }

    /**
     * 🚀 提取组件角色标签 (基于类名后缀)
     */
    private static java.util.Set<String> extractComponentRole(TypeDeclaration<?> type) {
        java.util.Set<String> roles = new java.util.HashSet<>();
        String className = type.getNameAsString();
        
        if (namingTags == null) return roles;
        JsonArray rules = namingTags.getAsJsonObject("dimensions").getAsJsonArray("COMPONENT_ROLE");
        
        for (JsonElement e : rules) {
            JsonObject rule = e.getAsJsonObject();
            for (JsonElement suffix : rule.getAsJsonArray("suffixes")) {
                if (className.endsWith(suffix.getAsString())) {
                    roles.add(rule.get("tag").getAsString());
                    break;
                }
            }
        }
        return roles;
    }

    /**
     * 🚀 增强版方法解析：增加意图标签
     */
    private static List<Map<String, Object>> resolveMethodsEnhanced(TypeDeclaration<?> type, List<String> fileLines) {
        List<Map<String, Object>> methods = new ArrayList<>();
        if (namingTags == null || !namingTags.has("dimensions")) return methods;

        JsonArray intentRules = namingTags.getAsJsonObject("dimensions").getAsJsonArray("METHOD_INTENT");
        if (intentRules == null) return methods;

        for (MethodDeclaration method : type.getMethods()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", method.getNameAsString());
            m.put("return_type", method.getTypeAsString());
            m.put("modifiers", resolveMods(method.getModifiers()));
            m.put("parameters", resolveParametersInventory(method.getParameters()));
            m.put("line_start", method.getBegin().map(p -> p.line).orElse(0));
            m.put("description", translateAndSummarize(bruteForceComment(fileLines, method)));
            
            // 提取方法意图标签
            java.util.Set<String> intents = new java.util.HashSet<>();
            String mName = method.getNameAsString().toLowerCase();
            String returnType = method.getTypeAsString().toLowerCase();
            
            for (JsonElement e : intentRules) {
                JsonObject rule = e.getAsJsonObject();
                boolean matched = false;
                
                // 1. 前缀匹配
                if (rule.has("prefixes")) {
                    for (JsonElement prefix : rule.getAsJsonArray("prefixes")) {
                        if (mName.startsWith(prefix.getAsString().toLowerCase())) {
                            matched = true; break;
                        }
                    }
                }
                
                // 2. 返回值类型辅助校验 (如 isXxx 必须是 boolean)
                if (matched && rule.has("return_type_hints")) {
                    boolean typeMatch = false;
                    for (JsonElement hint : rule.getAsJsonArray("return_type_hints")) {
                        if (returnType.contains(hint.getAsString().toLowerCase())) {
                            typeMatch = true; break;
                        }
                    }
                    if (!typeMatch) matched = false;
                }

                if (matched) intents.add(rule.get("tag").getAsString());
            }
            
            // 迭代进化：记录未识别的高频动词
            if (intents.isEmpty()) {
                String prefix = mName.replaceAll("[^a-z]", "").split("[0-9]")[0];
                if (prefix.length() > 2) { 
                    unrecognizedMethodPrefixes.merge(prefix, 1, Integer::sum);
                }
            }
            
            m.put("intent_tags", intents);
            methods.add(m);
        }
        return methods;
    }

    /**
     * 🚀 增强版字段解析：增加语义标签
     */
    private static List<Map<String, Object>> resolveFieldsEnhanced(TypeDeclaration<?> type) {
        List<Map<String, Object>> fields = new ArrayList<>();
        if (namingTags == null || !namingTags.has("dimensions")) return fields;

        JsonArray fieldRules = namingTags.getAsJsonObject("dimensions").getAsJsonArray("FIELD_SEMANTIC");
        if (fieldRules == null) return fields;

        for (FieldDeclaration field : type.getFields()) {
            for (VariableDeclarator var : field.getVariables()) {
                Map<String, Object> f = new LinkedHashMap<>();
                String fName = var.getNameAsString();
                f.put("name", fName);
                f.put("type", var.getTypeAsString());
                f.put("modifiers", resolveMods(field.getModifiers()));
                
                // 提取字段语义标签
                java.util.Set<String> semantics = new java.util.HashSet<>();
                String lowerName = fName.toLowerCase();
                
                // 1. 前缀匹配
                for (JsonElement e : fieldRules) {
                    JsonObject rule = e.getAsJsonObject();
                    if (rule.has("prefixes")) {
                        for (JsonElement prefix : rule.getAsJsonArray("prefixes")) {
                            if (lowerName.startsWith(prefix.getAsString().toLowerCase())) {
                                semantics.add(rule.get("tag").getAsString());
                            }
                        }
                    }
                    // 2. 后缀匹配
                    if (rule.has("suffixes")) {
                        for (JsonElement suffix : rule.getAsJsonArray("suffixes")) {
                            if (fName.endsWith(suffix.getAsString())) {
                                semantics.add(rule.get("tag").getAsString());
                            }
                        }
                    }
                }
                f.put("semantic_tags", semantics);
                fields.add(f);
            }
        }
        return fields;
    }

    /**
     * 🚀 标签引擎：基于字典的动态打标
     */
    private static Map<String, Object> extractTagsFromDictionary(JsonObject dictionary, TypeDeclaration<?> type, String pkg) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (dictionary == null || !dictionary.has("dimensions")) return result;

        String className = type.getNameAsString();
        String lowerName = className.toLowerCase();
        String fullAddress = pkg + "." + className;

        JsonObject dimensions = dictionary.getAsJsonObject("dimensions");
        for (String dim : dimensions.keySet()) {
            JsonArray rules = dimensions.getAsJsonArray(dim);
            java.util.Set<String> matchedTags = new java.util.HashSet<>();
            
            for (JsonElement ruleElement : rules) {
                JsonObject rule = ruleElement.getAsJsonObject();
                boolean match = false;

                // 1. 关键词匹配 (类名或包名)
                if (rule.has("keywords")) {
                    for (JsonElement kw : rule.getAsJsonArray("keywords")) {
                        if (lowerName.contains(kw.getAsString().toLowerCase()) || 
                            pkg.toLowerCase().contains(kw.getAsString().toLowerCase())) {
                            match = true; break;
                        }
                    }
                }

                // 2. 包路径正则匹配
                if (!match && rule.has("package_patterns")) {
                    for (JsonElement pp : rule.getAsJsonArray("package_patterns")) {
                        if (pkg.contains(pp.getAsString())) {
                            match = true; break;
                        }
                    }
                }

                if (match) matchedTags.add(rule.get("tag").getAsString());
            }
            if (!matchedTags.isEmpty()) {
                result.put(dim, matchedTags);
            }
        }
        return result;
    }

    /**
     * 加载命名语义标签字典
     */
    private static void loadNamingTags() {
        try {
            java.net.URL resource = SourceUniversePro.class.getClassLoader().getResource("naming-tags.json");
            if (resource != null) {
                com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(
                        new java.io.InputStreamReader(resource.openStream(), StandardCharsets.UTF_8));
                reader.setStrictness(com.google.gson.Strictness.LENIENT);
                namingTags = JsonParser.parseReader(reader).getAsJsonObject();
                System.out.println("✅ 命名语义标签加载成功");
            } else {
                System.err.println("⚠️ naming-tags.json not found on classpath");
            }
        } catch (Exception e) {
            System.err.println("⚠️ 命名语义标签加载失败: " + e.getMessage());
        }
    }

    /**
     * 加载框架专属标签字典
     */
    private static void loadFrameworkTags() {
        try {
            java.net.URL resource = SourceUniversePro.class.getClassLoader().getResource("tag-dictionary.json");
            if (resource != null) {
                com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(
                        new java.io.InputStreamReader(resource.openStream(), StandardCharsets.UTF_8));
                reader.setStrictness(com.google.gson.Strictness.LENIENT);
                frameworkTags = JsonParser.parseReader(reader).getAsJsonObject();
                System.out.println("✅ 框架专属标签加载成功: " + frameworkTags.get("framework").getAsString());
            }
        } catch (Exception e) {
            System.err.println("⚠️ 框架专属标签加载失败: " + e.getMessage());
        }
    }

    /**
     * 加载代码案例库
     */
    private static void loadCodeExamples() {
        try {
            java.net.URL resource = SourceUniversePro.class.getClassLoader().getResource("code-examples.json");
            if (resource != null) {
                com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(
                        new java.io.InputStreamReader(resource.openStream(), StandardCharsets.UTF_8));
                reader.setStrictness(com.google.gson.Strictness.LENIENT);
                codeExamples = JsonParser.parseReader(reader).getAsJsonObject();
                System.out.println("✅ 代码案例库加载成功: " + codeExamples.size() + " 个案例");
            }
        } catch (Exception e) {
            System.err.println("⚠️ 代码案例库加载失败: " + e.getMessage());
        }
    }

    /**
     * 🚀 提取 AI 教学指南：包含调用约束与适用场景
     */
    private static Map<String, Object> extractAIGuidance(TypeDeclaration<?> type, List<String> fileLines) {
        Map<String, Object> guidance = new LinkedHashMap<>();
        List<String> constraints = new ArrayList<>();
        String rawComment = bruteForceComment(fileLines, type);

        // 1. 提取废弃标记
        if (type.getAnnotations().stream().anyMatch(a -> a.getNameAsString().contains("Deprecated"))) {
            constraints.add("⚠️ Deprecated: This component is outdated and should be avoided in new implementations.");
        }

        // 2. 提取关键约束关键词 (启发式)
        String lowerComment = rawComment.toLowerCase();
        if (lowerComment.contains("thread-safe") || lowerComment.contains("concurrent")) {
            constraints.add("✅ Thread-Safe: Can be safely shared across multiple threads.");
        } else if (lowerComment.contains("not thread-safe")) {
            constraints.add("❌ Not Thread-Safe: Do not share instances across threads without synchronization.");
        }

        if (lowerComment.contains("must be closed") || lowerComment.contains("autoCloseable")) {
            constraints.add("🔒 Resource Management: Must be explicitly closed or used in a try-with-resources block.");
        }

        // 3. 识别潜在场景 (基于类名和注释)
        String className = type.getNameAsString().toLowerCase();
        List<String> scenarios = new ArrayList<>();
        if (className.contains("lock") || className.contains("mutex")) scenarios.add("Distributed Synchronization");
        if (className.contains("cache") || className.contains("bucket")) scenarios.add("High-Performance Data Retrieval");
        if (className.contains("limit") || className.contains("rate")) scenarios.add("Traffic Control & Protection");
        
        guidance.put("constraints", constraints);
        guidance.put("recommended_scenarios", scenarios);
        return guidance;
    }

    /**
     * 🚀 逻辑推理引擎：基于 AST 的行为与架构推导 (带证据锚定)
     */
    private static Map<String, Object> performLogicalInference(TypeDeclaration<?> type, List<String> fileLines) {
        Map<String, Object> results = new LinkedHashMap<>();
        List<Map<String, Object>> evidenceList = new ArrayList<>();
        
        // 1. 状态管理推理
        boolean hasMutableState = false;
        for (FieldDeclaration field : type.getFields()) {
            if (!field.getModifiers().contains(Modifier.staticModifier()) || !field.getModifiers().contains(Modifier.finalModifier())) {
                hasMutableState = true;
                Map<String, Object> ev = new LinkedHashMap<>();
                ev.put("type", "MUTABLE_FIELD");
                ev.put("target", field.getVariable(0).getNameAsString());
                ev.put("line", field.getBegin().map(p -> p.line).orElse(0));
                evidenceList.add(ev);
                break;
            }
        }
        
        // 2. 接口身份推理
        if (type instanceof ClassOrInterfaceDeclaration) {
            for (ClassOrInterfaceType impl : ((ClassOrInterfaceDeclaration) type).getImplementedTypes()) {
                Map<String, Object> ev = new LinkedHashMap<>();
                ev.put("type", "INTERFACE_IMPL");
                ev.put("target", impl.getNameAsString());
                ev.put("line", impl.getBegin().map(p -> p.line).orElse(0));
                evidenceList.add(ev);
            }
        }

        results.put("is_stateful", hasMutableState);
        results.put("evidence_trail", evidenceList);
        return results;
    }

    /**
     * 🚀 提取域上下文：识别组件所属的业务领域
     */
    private static String extractDomainContext(String pkg) {
        String[] parts = pkg.split("\\.");
        // 简单启发式：取包名中倒数第二个或具有语义的部分作为域
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].matches(".*(security|payment|order|user|config|core).*")) {
                return parts[i];
            }
        }
        return "general";
    }

    /**
     * 🚀 提取调用图摘要：为 Agent 提供影响分析依据
     */
    private static Map<String, Object> extractCallGraphSummary(TypeDeclaration<?> type) {
        Map<String, Object> summary = new LinkedHashMap<>();
        java.util.Set<String> calledClasses = new java.util.HashSet<>();
        
        type.findAll(MethodCallExpr.class).forEach(call -> {
            call.getScope().ifPresent(scope -> {
                String scopeStr = scope.toString();
                if (!scopeStr.startsWith("this") && !scopeStr.startsWith("super")) {
                    calledClasses.add(scopeStr.split("\\.")[0]);
                }
            });
        });
        
        summary.put("external_dependencies", calledClasses);
        summary.put("complexity_score", type.getMethods().size() * 1.5); // 简化评分
        return summary;
    }

    /**
     * 🚀 提取 AST 语义画像：基于结构与行为的深度分析
     */
    private static Map<String, Object> extractSemanticProfile(TypeDeclaration<?> type, List<String> fileLines) {
        Map<String, Object> profile = new LinkedHashMap<>();
        
        // 1. 继承与实现关系 (Identity)
        if (type instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) type;
            List<String> interfaces = new ArrayList<>();
            cid.getImplementedTypes().forEach(t -> {
                try { interfaces.add(t.resolve().describe()); } catch (Exception e) { interfaces.add(t.getNameAsString()); }
            });
            profile.put("implements", interfaces);
            
            // 2. 关键接口语义打标
            java.util.Set<String> semanticTags = new java.util.HashSet<>();
            for (String iface : interfaces) {
                if (iface.contains("Lock")) semanticTags.add("DistributedLockProvider");
                if (iface.contains("Map")) semanticTags.add("DataStructureImpl");
                if (iface.contains("Listener")) semanticTags.add("EventObserver");
                if (iface.contains("Serializable")) semanticTags.add("StateTransferable");
            }
            profile.put("semantic_roles", semanticTags);
        }

        // 3. 方法级 AST 复杂度分析
        List<Map<String, Object>> methodProfiles = new ArrayList<>();
        for (MethodDeclaration method : type.getMethods()) {
            Map<String, Object> mp = new LinkedHashMap<>();
            mp.put("name", method.getNameAsString());
            
            // 计算圈复杂度 (Cyclomatic Complexity) 简化版
            int complexity = 1;
            complexity += method.findAll(com.github.javaparser.ast.stmt.IfStmt.class).size();
            complexity += method.findAll(com.github.javaparser.ast.stmt.ForEachStmt.class).size();
            complexity += method.findAll(com.github.javaparser.ast.stmt.WhileStmt.class).size();
            complexity += method.findAll(com.github.javaparser.ast.stmt.CatchClause.class).size();
            
            mp.put("complexity_score", complexity);
            mp.put("is_async", method.getTypeAsString().contains("Future") || 
                                method.getAnnotations().stream().anyMatch(a -> a.getNameAsString().contains("Async")));
            
            methodProfiles.add(mp);
        }
        profile.put("method_complexity", methodProfiles);
        
        return profile;
    }

    private static Map<String, Object> getEnhancementData(String address) {
        Map<String, Object> enhancement = new LinkedHashMap<>();
        if (codeExamples != null && codeExamples.has(address)) {
            JsonObject example = codeExamples.getAsJsonObject(address);
            enhancement.put("scenario_case", example.get("scenario_case").getAsString());
            enhancement.put("best_practice", example.get("best_practice").getAsString());
            enhancement.put("related_concepts", example.get("related_concepts").toString());
        } else {
            enhancement.put("scenario_case", "暂无典型案例，等待 AI Agent 补充...");
            enhancement.put("best_practice", "待分析");
            enhancement.put("related_concepts", "[]");
        }
        return enhancement;
    }

    /**
     * 🚀 核心功能：生成语义映射字典表
     */
    private static void generateSemanticDictionary(String frameworkName, Path outputDir) {
        System.out.println("\n📊 正在构建语义映射字典表...");
        
        // 1. 统计类名后缀 (Class Suffixes)
        Map<String, Integer> classSuffixes = new HashMap<>();
        for (String name : allClassNames) {
            String suffix = name.replaceAll("^[A-Z][a-z]+", ""); // 提取首单词后的部分
            if (suffix.length() > 2) {
                classSuffixes.merge(suffix, 1, Integer::sum);
            }
        }

        // 2. 统计方法名前缀 (Method Prefixes)
        Map<String, Integer> methodPrefixes = new HashMap<>();
        for (String name : allMethodNames) {
            String prefix = name.replaceAll("[A-Z].*", "").toLowerCase(); // 提取首单词
            if (prefix.length() > 2) {
                methodPrefixes.merge(prefix, 1, Integer::sum);
            }
        }

        // 3. 组装字典对象
        Map<String, Object> dictionary = new LinkedHashMap<>();
        dictionary.put("framework", frameworkName);
        dictionary.put("generated_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        dictionary.put("class_suffix_patterns", classSuffixes.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(50) // 取 Top 50
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
        dictionary.put("method_prefix_patterns", methodPrefixes.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(50) // 取 Top 50
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
        
        // 4. 输出字典文件
        try {
            String dictPath = outputDir.resolve(frameworkName + "_semantic_dictionary.json").toString();
            saveAsJson(dictionary, dictPath);
            System.out.println("✅ 语义字典已生成: " + dictPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 🚀 智能标签系统：基于方法名和特征的语义分类
     */
    private static java.util.Set<String> extractMethodTags(String methodName, String returnType) {
        java.util.Set<String> tags = new java.util.HashSet<>();
        if (methodName == null) return tags;
        
        String lowerName = methodName.toLowerCase();

        // 1. 行为维度 (CRUD)
        if (lowerName.matches(".*(get|find|select|query|list|search|fetch).*")) tags.add("Query");
        else if (lowerName.matches(".*(add|create|insert|save|register).*")) tags.add("Create");
        else if (lowerName.matches(".*(update|modify|edit|change|set).*")) tags.add("Update");
        else if (lowerName.matches(".*(delete|remove|drop|clear).*")) tags.add("Delete");

        // 2. 并发维度
        if (lowerName.matches(".*(sync|lock|unlock|mutex).*")) tags.add("Locking");
        else if (lowerName.matches(".*(async|future|callback|executor).*")) tags.add("Async");
        else if (lowerName.matches(".*(atomic|cas).*")) tags.add("Atomic");

        // 3. 架构角色
        if (lowerName.matches(".*(init|start|stop|destroy|close|open).*")) tags.add("Lifecycle");
        else if (lowerName.matches(".*(config|property|setting).*")) tags.add("Config");
        else if (lowerName.matches(".*(util|helper|convert|parse|format).*")) tags.add("Utility");

        // 4. 安全与校验
        if (lowerName.matches(".*(validate|check|verify|assert).*")) tags.add("Validation");
        else if (lowerName.matches(".*(error|catch|rollback|retry).*")) tags.add("ErrorHandling");

        return tags;
    }

    /**
     * 从项目路径或 pom.xml 中提取框架名称
     */
    private static String extractFrameworkName(String projectRoot) {
        try {
            Path pomPath = Paths.get(projectRoot).resolve("pom.xml");
            if (Files.exists(pomPath)) {
                String content = String.join("\n", Files.readAllLines(pomPath, StandardCharsets.UTF_8));
                
                // 策略 A: 从 <name> 标签提取（优先，因为更友好）
                Pattern namePattern = Pattern.compile("<name>([^<]+)</name>");
                Matcher nameMatcher = namePattern.matcher(content);
                if (nameMatcher.find()) {
                    String name = nameMatcher.group(1).trim();
                    // 移除版本占位符
                    String cleaned = name.replaceAll("\\$\\{.*?\\}", "").trim();
                    if (!cleaned.isEmpty()) return cleaned;
                }
                
                // 策略 B: 特殊项目硬编码映射 (确保显示名称专业)
                if (content.contains("<artifactId>dubbo-parent</artifactId>")) return "Apache Dubbo";
                if (content.contains("<artifactId>rocketmq-all</artifactId>")) return "Apache RocketMQ";
                if (content.contains("<artifactId>tomcat-embed-core</artifactId>")) return "Apache Tomcat";
                
                // 策略 B: 从根项目的 <artifactId> 提取（跳过 <parent> 块）
                // 先移除 parent 块
                String withoutParent = content.replaceAll("<parent>.*?</parent>", "");
                Pattern artifactPattern = Pattern.compile("<artifactId>([^<]+)</artifactId>");
                Matcher artifactMatcher = artifactPattern.matcher(withoutParent);
                if (artifactMatcher.find()) {
                    String artifactId = artifactMatcher.group(1).trim();
                    // 移除 -all, -parent 等后缀，转换为友好名称
                    String cleaned = artifactId.replaceAll("-(all|parent|core)$", "");
                    return toTitleCase(cleaned);
                }
            }
            
            // 策略 C: 从路径最后一段提取
            Path path = Paths.get(projectRoot);
            String folderName = path.getFileName().toString();
            return toTitleCase(folderName);
        } catch (Exception e) {
            return "Unknown Framework";
        }
    }

    /**
     * 将 kebab-case 转换为 Title Case
     * 例如: rocketmq-all -> Rocketmq All
     */
    private static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        
        String[] parts = input.split("-");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(" ");
            if (!parts[i].isEmpty()) {
                result.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1));
                }
            }
        }
        return result.toString();
    }

    /**
     * 自动从 pom.xml 提取版本号 (带最大深度保护)
     */
    private static String extractVersion(String projectRoot) {
        return extractVersionRecursive(projectRoot, 0);
    }

    private static String extractVersionRecursive(String projectRoot, int depth) {
        if (depth > 10) return "version-unresolved";

        try {
            Path currentPath = Paths.get(projectRoot);
            Path pomPath = currentPath.resolve("pom.xml");
            String version = fetchVersionFromPom(pomPath);

            if (version != null && !version.contains("$")) {
                return version;
            }

            // 向上递归查找
            Path parent = currentPath.getParent();
            if (parent != null && !parent.equals(currentPath)) {
                return extractVersionRecursive(parent.toString(), depth + 1);
            }

            return version != null ? version : "version-unresolved";
        } catch (Exception e) {
            return "version-error";
        }
    }

    /**
     * 从单个 pom.xml 文件中提取版本号 (优先读取 properties)
     */
    private static String fetchVersionFromPom(Path path) throws Exception {
        if (!Files.exists(path)) return null;

        String content = String.join("\n", Files.readAllLines(path, StandardCharsets.UTF_8));
        
        // 策略 A: 尝试从 <properties> 中提取 revision 或 version
        Pattern propPattern = Pattern.compile("<properties>.*?<revision>(.*?)</revision>.*?</properties>", Pattern.DOTALL);
        Matcher propMatcher = propPattern.matcher(content);
        if (propMatcher.find()) {
            return propMatcher.group(1).trim();
        }

        // 策略 B: 提取根项目的 <version> (通常在 artifactId 之后)
        // 排除掉 dependencies 里的 version
        String[] lines = content.split("\n");
        boolean inProperties = false;
        boolean inDependencies = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("<properties>")) inProperties = true;
            if (trimmed.endsWith("</properties>")) inProperties = false;
            if (trimmed.startsWith("<dependencies>")) inDependencies = true;
            if (trimmed.endsWith("</dependencies>")) inDependencies = false;

            if (!inProperties && !inDependencies && trimmed.matches("<version>.*</version>")) {
                return trimmed.replaceAll("<version>|</version>", "").trim();
            }
        }
        
        return null;
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar glossary-java-source-analyzer.jar [OPTIONS]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --sourceRoot <path>        Path to the Java project root directory to scan");
        System.out.println("  --outputDir <path>         Output directory for JSON results (default: ./dev-ops/output)");
        System.out.println("  --artifactName <name>      Artifact name override (default: auto-detected from pom.xml)");
        System.out.println("  --version <version>        Version string (default: auto-detected from pom.xml)");
        System.out.println("  --internalPkgPrefix <prefix> Internal package prefix (default: java)");
        System.out.println("  --help                     Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar glossary.jar --sourceRoot /path/to/project");
        System.out.println("  java -jar glossary.jar --sourceRoot /path/to/project --outputDir /tmp/output --version 2.0");
    }

    private static void registerSubModules(CombinedTypeSolver solver, String root) throws IOException {
        Files.walk(Paths.get(root))
                .filter(p -> Files.isDirectory(p) && p.toString().endsWith("src" + File.separator + "main" + File.separator + "java"))
                .forEach(src -> solver.add(new JavaParserTypeSolver(src.toFile())));
    }

    private static List<Map<String, Object>> extractAnnos(NodeWithAnnotations<?> n, String target) {
        return n.getAnnotations().stream().map(a -> {
            Map<String, Object> am = new LinkedHashMap<>();
            am.put("name", a.getNameAsString());
            am.put("target", target);
            return am;
        }).collect(Collectors.toList());
    }

    private static String getKind(TypeDeclaration<?> t) {
        if (t instanceof ClassOrInterfaceDeclaration) return ((ClassOrInterfaceDeclaration) t).isInterface() ? "INTERFACE" : "CLASS";
        if (t instanceof EnumDeclaration) return "ENUM";
        return "TYPE";
    }

    private static void saveAsJson(Object obj, String fileName) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        try (FileWriter fw = new FileWriter(fileName)) { 
            gson.toJson(obj, fw); 
        }
        System.out.println("✅ 数字化资产已保存在: " + fileName);
    }

    // ======================== 辅助打印工具 ========================
    
    private static void printLine(char c, int count) {
        for (int i = 0; i < count; i++) System.out.print(c);
        System.out.println();
    }

    private static void printReport() {
        printLine('=', 80);
        System.out.println("📊 资产解剖报告 (Summary Report)");
        printLine('-', 80);
        System.out.printf("🏛️  类资产总计: %d\n", classCount.get());
        System.out.printf("⚙️  行为(方法): %d\n", methodCount.get());
        System.out.printf("📦 状态(字段): %d\n", fieldCount.get());
        System.out.printf("📝 标注覆盖率: %.2f%%\n", (commentFound.get() * 100.0 / Math.max(classCount.get(), 1)));
        printLine('-', 80);
        
        // 🚀 打印模式分析报告
        printPatternAnalysis();
        
        printLine('=', 80);
    }

    /**
     * 🚀 模式分析：统计类名和方法名的高频特征，辅助设计标签体系
     */
    private static void printPatternAnalysis() {
        System.out.println("\n🚀 [迭代进化建议] 发现以下高频未识别模式，建议纳入标签库:");
        printTopPatterns("方法前缀 (Method Prefixes)", unrecognizedMethodPrefixes, 10);
        printTopPatterns("类后缀 (Class Suffixes)", unrecognizedClassSuffixes, 10);
    }

    private static void printTopPatterns(String type, Map<String, Integer> patterns, int limit) {
        if (patterns.isEmpty()) return;
        System.out.println("\n📌 [" + type + " Top " + limit + "]");
        patterns.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)
            .forEach(e -> System.out.printf("   - %-20s : %d 次\n", e.getKey(), e.getValue()));
    }
}