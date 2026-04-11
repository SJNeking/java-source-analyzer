package cn.dolphinmind.glossary.java.analyze;

import cn.dolphinmind.glossary.java.analyze.config.RulesConfig;
import cn.dolphinmind.glossary.java.analyze.orchestrate.AnalysisConfig;
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

    // 🚀 迭代进化：未识别模式收集器
    private static Map<String, Integer> unrecognizedMethodPrefixes = new HashMap<>();
    private static Map<String, Integer> unrecognizedClassSuffixes = new HashMap<>();

    // 🚀 项目专属词汇表 (Project-Specific Glossary)
    private static Map<String, String> projectGlossary = new HashMap<>();
    private static String currentProjectRoot = "";

    /**

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

        // 3. Start WebSocket server if enabled
        cn.dolphinmind.glossary.java.analyze.realtime.AnalysisWebSocketServer wsServer = null;
        if (config.isWebsocketEnabled()) {
            wsServer = new cn.dolphinmind.glossary.java.analyze.realtime.AnalysisWebSocketServer(config.getWebsocketPort());
            wsServer.start();
            System.out.println("🔌 实时分析 WebSocket 已启动: ws://localhost:" + config.getWebsocketPort());
        }

        try {
            // 4. Run analysis with WebSocket support
            runAnalysis(config, translator, wsServer);
        } finally {
            // 5. Stop WebSocket server
            if (wsServer != null) {
                wsServer.stop();
            }
        }
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
                case "--websocket":
                    config.setWebsocketEnabled(true);
                    if (i + 1 < args.length && args[i + 1].matches("\\d+")) {
                        config.setWebsocketPort(Integer.parseInt(args[++i]));
                    }
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
        runAnalysis(config, translator, null);
    }

    /**
     * Run the full analysis pipeline with optional WebSocket server for real-time progress.
     * @param wsServer Optional WebSocket server for broadcasting progress events
     */
    public static void runAnalysis(AnalysisConfig config, SemanticTranslator translator,
            cn.dolphinmind.glossary.java.analyze.realtime.AnalysisWebSocketServer wsServer) throws Exception {
        // Load dictionaries
        loadTagDictionary();
        initTagLibrary();

        RulesConfig rulesConfig = RulesConfig.load(config.getRulesConfigPath());
        translator.loadProjectGlossary(config.getSourceRoot());

        // Initialize extracted services
        cn.dolphinmind.glossary.java.analyze.translate.CommentAnalysisService commentService =
                new cn.dolphinmind.glossary.java.analyze.translate.CommentAnalysisService();
        cn.dolphinmind.glossary.java.analyze.translate.SemanticEnrichmentService semanticService =
                new cn.dolphinmind.glossary.java.analyze.translate.SemanticEnrichmentService(translator);
        cn.dolphinmind.glossary.java.analyze.extractor.MethodCallAnalyzer methodAnalyzer =
                new cn.dolphinmind.glossary.java.analyze.extractor.MethodCallAnalyzer();

        // Create JavaAssetExtractor with legacy bridge for not-yet-migrated methods
        // Use holder pattern to avoid forward reference issue with anonymous inner class
        final cn.dolphinmind.glossary.java.analyze.extractor.JavaAssetExtractor[] assetExtractorRef = new cn.dolphinmind.glossary.java.analyze.extractor.JavaAssetExtractor[1];

        cn.dolphinmind.glossary.java.analyze.extractor.JavaAssetExtractor.LegacyBridge legacyBridge =
                new cn.dolphinmind.glossary.java.analyze.extractor.JavaAssetExtractor.LegacyBridge() {
                    public String getKind(TypeDeclaration<?> t) { return getKind(t); }
                    public Map<String, Object> extractMethodEnhanced(CallableDeclaration<?> d, String baseAddr,
                            NodeList<Parameter> params, List<String> fileLines, String classAddr, List<Map<String, String>> globalDeps) {
                        return assetExtractorRef[0].extractMethodEnhanced(d, baseAddr, params, fileLines, classAddr, globalDeps);
                    }
                    public List<Map<String, Object>> resolveMethodsEnhanced(TypeDeclaration<?> t, List<String> fl) {
                        return resolveMethodsEnhanced(t, fl);
                    }
                    public List<Map<String, Object>> resolveFieldsEnhanced(TypeDeclaration<?> t) {
                        return resolveFieldsEnhanced(t);
                    }
                    public List<Map<String, Object>> resolveFieldsMatrix(List<String> l, TypeDeclaration<?> t) {
                        return resolveFieldsMatrix(l, t);
                    }
                    public List<Map<String, Object>> resolveConstructorsAligned(List<String> l, TypeDeclaration<?> t, String a) {
                        return resolveConstructorsAligned(l, t, a);
                    }
                    public List<Map<String, Object>> resolveMethodsAligned(List<String> l, TypeDeclaration<?> t, String a) {
                        return resolveMethodsAligned(l, t, a);
                    }
                    public Map<String, Object> getEnhancementData(String a) { return getEnhancementData(a); }
                    public Map<String, Object> extractCallGraphSummary(TypeDeclaration<?> t) { return extractCallGraphSummary(t); }
                    public int calculateClassLOC(TypeDeclaration<?> t, List<String> fl) { return calculateClassLOC(t, fl); }
                    public int calculateClassComplexity(TypeDeclaration<?> t, List<String> fl) { return calculateClassComplexity(t, fl); }
                    public int calculateInheritanceDepth(TypeDeclaration<?> t) { return calculateInheritanceDepth(t); }
                    public List<Map<String, Object>> extractAnnos(TypeDeclaration<?> t, String a) { return extractAnnos(t, a); }
                    public String extractNodeSource(List<String> fl, Node n, boolean ib) { return extractNodeSource(fl, n, ib); }
                    public List<String> extractMethodTags(String mn, String rt) { return extractMethodTags(mn, rt); }
                    public List<Map<String, Object>> resolveParametersInventory(NodeList<Parameter> p) { return resolveParametersInventory(p); }
                    public void trackUnrecognizedSuffix(String s) { unrecognizedClassSuffixes.merge(s, 1, Integer::sum); }
                    public void trackMethodName(String n) { allMethodNames.add(n); }
                };

        cn.dolphinmind.glossary.java.analyze.extractor.JavaAssetExtractor assetExtractor =
                new cn.dolphinmind.glossary.java.analyze.extractor.JavaAssetExtractor(
                        commentService, semanticService, methodAnalyzer,
                        classCount, methodCount, fieldCount,
                        seenDependencies,
                        legacyBridge);
        assetExtractorRef[0] = assetExtractor;

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
        rootContainer.put("schema_version", "1.0.0");
        rootContainer.put("analyzer_version", "2.0.0");
        rootContainer.put("framework", frameworkName);
        rootContainer.put("version", detectedVersion);
        rootContainer.put("scan_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        List<Map<String, Object>> globalLibrary = new ArrayList<>();
        Map<String, List<Map<String, Object>>> moduleLibrary = new LinkedHashMap<>();
        List<Map<String, String>> globalDependencies = new ArrayList<>();
        Map<String, Object> projectAssets = new LinkedHashMap<>();

        // Parallel Java source scanning
        System.out.println("🚀 正在全量遍历代码，提取语义词汇...");

        // Count total files first for progress tracking
        cn.dolphinmind.glossary.java.analyze.scanner.ProjectScanner ps =
                new cn.dolphinmind.glossary.java.analyze.scanner.ProjectScanner(Paths.get(config.getSourceRoot()));
        List<Path> modules = ps.detectModules();
        System.out.println("📦 检测到 " + modules.size() + " 个模块");

        // Count total Java files across all modules
        int totalJavaFiles = 0;
        for (Path moduleRoot : modules) {
            try {
                totalJavaFiles += (int) Files.walk(moduleRoot)
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> {
                            String pathStr = p.toString();
                            return (pathStr.contains("java" + File.separator) || pathStr.contains("src")) &&
                                   !pathStr.contains("test") && !pathStr.contains("target");
                        }).count();
            } catch (Exception ignored) {}
        }

        // Emit scan start event
        if (wsServer != null) {
            wsServer.broadcast(cn.dolphinmind.glossary.java.analyze.realtime.AnalysisProgressEvent.scanStart(totalJavaFiles));
        }

        java.util.concurrent.ForkJoinPool forkJoinPool = new java.util.concurrent.ForkJoinPool(
                Math.min(Runtime.getRuntime().availableProcessors(), 8));

        java.util.concurrent.atomic.AtomicInteger filesScanned = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger filesFailed = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger filesSkipped = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger totalFilesRef = new java.util.concurrent.atomic.AtomicInteger(totalJavaFiles);

        // Load incremental cache with version checking
        Path cacheDir = config.getCacheDir();
        cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.CacheData cache =
                cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.load(cacheDir,
                        SemanticTranslator.getAnalyzerVersion());

        // Track changed files for cache merge
        Set<String> changedFilePaths = new java.util.HashSet<>();
        Set<String> allScannedFilePaths = new java.util.HashSet<>();

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

                // Track all scanned files
                for (Path f : javaFiles) {
                    allScannedFilePaths.add(moduleRoot.relativize(f).toString());
                }

                System.out.println("    📄 找到 " + javaFiles.size() + " 个 Java 文件");

                List<Path> changedFiles = cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.findChangedFiles(
                        moduleRoot, cache, javaFiles);
                filesSkipped.addAndGet(javaFiles.size() - changedFiles.size());

                // Track changed files
                for (Path cf : changedFiles) {
                    changedFilePaths.add(moduleRoot.relativize(cf).toString());
                }

                if (changedFiles.isEmpty()) {
                    System.out.println("    ✅ 无变更，跳过 " + javaFiles.size() + " 个文件");
                    continue;
                }

                System.out.println("    📄 变更文件: " + changedFiles.size() + ", 跳过: " + (javaFiles.size() - changedFiles.size()));

                // Track per-file assets for cache update
                Map<String, List<Map<String, Object>>> fileAssetsMap = new java.util.concurrent.ConcurrentHashMap<>();

                forkJoinPool.submit(() ->
                    changedFiles.parallelStream().forEach(path -> {
                        try {
                            filesScanned.incrementAndGet();
                            List<String> fileLines = Files.readAllLines(path, StandardCharsets.UTF_8);
                            CompilationUnit cu = StaticJavaParser.parse(path);
                            String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("default");

                            List<Map<String, Object>> fileAssets = new ArrayList<>();

                            cu.getTypes().forEach(type -> {
                                classCount.incrementAndGet();
                                String className = type.getNameAsString();
                                allClassNames.add(className);

                                String cnName = translator.translateIdentifier(className);
                                projectGlossary.put(className.toLowerCase(), cnName);

                                Map<String, Object> classAsset = assetExtractor.processType(type, pkg, null, fileLines, ctx, globalDependencies);
                                if (!classAsset.getOrDefault("description", "").toString().isEmpty()) commentFound.incrementAndGet();

                                classAsset.put("module", moduleName);
                                classAsset.put("import_dependencies", extractImportDependencies(cu));
                                classAsset.put("annotation_params", extractAnnotationParams(type));

                                synchronized (fileAssets) {
                                    fileAssets.add(classAsset);
                                }
                                synchronized (globalLibrary) {
                                    globalLibrary.add(classAsset);
                                }
                                synchronized (moduleLibrary) {
                                    moduleLibrary.computeIfAbsent(moduleName, k -> new ArrayList<>()).add(classAsset);
                                }

                                extractDependencies((String) classAsset.get("address"), classAsset, globalDependencies);
                            });

                            // Store per-file assets for cache update
                            String relPath = moduleRoot.relativize(path).toString();
                            fileAssetsMap.put(relPath, fileAssets);

                            // Update cache with full assets
                            String hash = cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.computeFileHash(path);
                            cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.updateCacheEntry(
                                    cache, relPath, hash, fileAssets);

                            // Emit file scanned event
                            if (wsServer != null) {
                                wsServer.broadcast(cn.dolphinmind.glossary.java.analyze.realtime.AnalysisProgressEvent.fileScanned(
                                        path.getFileName().toString(),
                                        filesScanned.get(),
                                        totalFilesRef.get()));
                            }

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

        // Handle deleted files: remove from cache
        Set<String> deletedPaths = new java.util.HashSet<>();
        for (String cachedPath : cache.getFiles().keySet()) {
            if (!allScannedFilePaths.contains(cachedPath)) {
                deletedPaths.add(cachedPath);
            }
        }
        if (!deletedPaths.isEmpty()) {
            int removed = cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.removeDeletedEntries(cache, deletedPaths);
            System.out.println("🗑️ 移除 " + removed + " 个已删除文件的缓存");
        }

        // Merge cached assets with newly scanned assets
        if (!cache.getFiles().isEmpty()) {
            List<Map<String, Object>> mergedAssets = cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.mergeCachedAssets(
                    cache, new ArrayList<>(globalLibrary), changedFilePaths);

            // Replace globalLibrary with merged assets (deduplicate by address)
            Map<String, Map<String, Object>> assetMap = new LinkedHashMap<>();
            for (Map<String, Object> asset : mergedAssets) {
                String address = (String) asset.get("address");
                if (address != null) {
                    assetMap.put(address, asset); // new assets overwrite cached ones
                }
            }
            globalLibrary.clear();
            globalLibrary.addAll(assetMap.values());

            int cachedCount = cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.getTotalAssetCount(cache);
            int newCount = globalLibrary.size();
            System.out.println("✅ 缓存合并: " + cachedCount + " 缓存资产, " + newCount + " 去重后资产");
        }

        forkJoinPool.shutdown();
        System.out.println("✅ 扫描完成: " + filesScanned.get() + " 文件, " + filesFailed.get() + " 失败, " + filesSkipped.get() + " 跳过");

        // Save incremental cache
        try {
            cache.setScanDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
            cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.save(cache, cacheDir);
            System.out.println("💾 增量缓存已保存: " + cache.getFiles().size() + " 文件记录, " +
                    cn.dolphinmind.glossary.java.analyze.incremental.IncrementalCache.getTotalAssetCount(cache) + " 缓存类");
        } catch (Exception e) {
            System.err.println("⚠️ 保存增量缓存失败: " + e.getMessage());
        }

        // Scan non-Java assets
        if (wsServer != null) {
            wsServer.broadcast(cn.dolphinmind.glossary.java.analyze.realtime.AnalysisProgressEvent.assetsStart());
        }
        projectAssets = scanProjectFiles(Paths.get(config.getSourceRoot()));
        if (wsServer != null) {
            wsServer.broadcast(cn.dolphinmind.glossary.java.analyze.realtime.AnalysisProgressEvent.assetsComplete());
        }

        // Cross-file relations
        if (wsServer != null) {
            wsServer.broadcast(cn.dolphinmind.glossary.java.analyze.realtime.AnalysisProgressEvent.relationsStart());
        }
        cn.dolphinmind.glossary.java.analyze.relation.RelationEngine relationEngine =
                new cn.dolphinmind.glossary.java.analyze.relation.RelationEngine();
        Map<String, Object> relationData = new LinkedHashMap<>();
        relationData.put("assets", globalLibrary);
        List<cn.dolphinmind.glossary.java.analyze.relation.AssetRelation> relations =
                relationEngine.discoverRelations(relationData, projectAssets);
        rootContainer.put("cross_file_relations", relationEngine.toMap());
        if (wsServer != null) {
            int relationCount = relationEngine.toMap().containsKey("total_relations") ?
                    (int) relationEngine.toMap().get("total_relations") : 0;
            wsServer.broadcast(cn.dolphinmind.glossary.java.analyze.realtime.AnalysisProgressEvent.relationsComplete(relationCount));
        }

        // Quality analysis
        System.out.println("\n🔍 正在执行静态代码质量分析...");
        if (wsServer != null) {
            wsServer.broadcast(cn.dolphinmind.glossary.java.analyze.realtime.AnalysisProgressEvent.qualityStart());
        }
        List<Map<String, Object>> qualityIssues = runQualityAnalysis(globalLibrary, rulesConfig, config.getSourceRoot(), globalDependencies);
        rootContainer.put("quality_issues", qualityIssues);
        Map<String, Object> qualitySummary = qualityIssues.isEmpty() ? Collections.emptyMap() : buildQualitySummary(qualityIssues);
        rootContainer.put("quality_summary", qualitySummary);
        if (wsServer != null) {
            wsServer.broadcast(cn.dolphinmind.glossary.java.analyze.realtime.AnalysisProgressEvent.qualityComplete(qualityIssues.size()));
        }

        // Project type detection
        cn.dolphinmind.glossary.java.analyze.scanner.ProjectTypeDetector typeDetector =
                new cn.dolphinmind.glossary.java.analyze.scanner.ProjectTypeDetector();
        Map<String, Object> projectTypeInfo = typeDetector.detect(
                Paths.get(config.getSourceRoot()), projectAssets, relationData);
        rootContainer.put("project_type", projectTypeInfo);

        // Spring Analysis: Extract API endpoints and bean dependencies
        boolean isSpringProject = "SPRING_BOOT".equals(projectTypeInfo.get("primary_type")) ||
                ((List<String>) projectTypeInfo.getOrDefault("all_types", Collections.emptyList())).contains("SPRING_BOOT");
        if (isSpringProject) {
            System.out.println("\n🌱 正在分析 Spring 框架...");
            try {
                cn.dolphinmind.glossary.java.analyze.spring.SpringAnalyzer springAnalyzer =
                        new cn.dolphinmind.glossary.java.analyze.spring.SpringAnalyzer();
                Map<String, Object> springData = springAnalyzer.analyze(config.getSourceRoot());
                rootContainer.put("spring_analysis", springData);
                System.out.println("✅ Spring 分析完成");
            } catch (Exception e) {
                System.err.println("⚠️ Spring 分析失败: " + e.getMessage());
            }
        }

        // Architecture Layer Analysis
        System.out.println("\n🏗️ 正在分析架构分层...");
        try {
            cn.dolphinmind.glossary.java.analyze.arch.ArchitectureLayerAnalyzer layerAnalyzer =
                    new cn.dolphinmind.glossary.java.analyze.arch.ArchitectureLayerAnalyzer();
            Map<String, Object> layerData = layerAnalyzer.analyze(globalLibrary, globalDependencies);
            rootContainer.put("architecture_layers", layerData);

            int violationCount = ((List<?>) layerData.getOrDefault("violations", Collections.emptyList())).size();
            System.out.println("✅ 架构分层分析完成: " + violationCount + " 个违规");
        } catch (Exception e) {
            System.err.println("⚠️ 架构分层分析失败: " + e.getMessage());
        }

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

        // 📊 JArchitect 对标：代码指标计算
        System.out.println("\n📊 正在计算代码指标 (JArchitect-style)...");
        if (globalLibrary.isEmpty()) {
            System.out.println("  ⚠️ 增量扫描跳过了所有文件，代码指标不可用");
            System.out.println("  提示: 删除 .universe/cache 或修改源码可获得完整指标");
        } else {
            System.out.println("  分析 " + globalLibrary.size() + " 个类...");
            try {
                cn.dolphinmind.glossary.java.analyze.metrics.CodeMetricsCalculator metricsCalc =
                        new cn.dolphinmind.glossary.java.analyze.metrics.CodeMetricsCalculator();
                cn.dolphinmind.glossary.java.analyze.metrics.CodeMetricsCalculator.ProjectMetrics projectMetrics =
                        metricsCalc.calculateProjectMetrics(globalLibrary, null, null);
                metricsCalc.printMetricsSummary(projectMetrics);
                rootContainer.put("code_metrics", projectMetrics.toMap());
            } catch (Exception e) {
                System.err.println("⚠️ 代码指标计算失败: " + e.getMessage());
            }
        }

        // 🔗 JArchitect 对标：依赖图生成
        System.out.println("\n🔗 正在生成依赖图 (JArchitect-style)...");
        if (!globalLibrary.isEmpty()) {
            try {
                cn.dolphinmind.glossary.java.analyze.metrics.DependencyGraphGenerator depGraphGen =
                        new cn.dolphinmind.glossary.java.analyze.metrics.DependencyGraphGenerator();
                cn.dolphinmind.glossary.java.analyze.metrics.DependencyGraphGenerator.PackageDependencyGraph pkgGraph =
                        depGraphGen.generatePackageGraph(globalLibrary);
                @SuppressWarnings("unchecked")
                List<Object> edges = (List<Object>) pkgGraph.toMap().get("edges");
                System.out.println("  包依赖: " + (edges != null ? edges.size() : 0) + " 条边");
                rootContainer.put("dependency_graph", new LinkedHashMap<String, Object>() {{
                    put("package_graph", pkgGraph.toMap());
                }});
            } catch (Exception e) {
                System.err.println("⚠️ 依赖图生成失败: " + e.getMessage());
            }
        } else {
            System.out.println("  ⚠️ 增量扫描跳过了所有文件，依赖图不可用");
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
            compressedSummary.put("schema_version", "1.0.0");
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

        // Emit analysis complete event
        if (wsServer != null) {
            wsServer.broadcast(cn.dolphinmind.glossary.java.analyze.realtime.AnalysisProgressEvent.analysisComplete(
                    globalLibrary.size(), filesScanned.get()));
        }
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
     * Uses RuleRegistry for centralized rule management instead of manual registration.
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> runQualityAnalysis(
            List<Map<String, Object>> globalLibrary, RulesConfig rulesConfig,
            String projectRoot, List<Map<String, String>> globalDependencies) {
        List<Map<String, Object>> issues = new ArrayList<>();
        cn.dolphinmind.glossary.java.analyze.quality.RuleEngine engine =
                new cn.dolphinmind.glossary.java.analyze.quality.RuleEngine();

        // Register all rules via RuleRegistry (replaces 450+ lines of manual reg.accept())
        cn.dolphinmind.glossary.java.analyze.quality.RuleRegistry.registerAll(engine, rulesConfig);

        // Run rules
        List<cn.dolphinmind.glossary.java.analyze.quality.QualityIssue> ruleIssues = engine.run(globalLibrary);

        // Duplicate code detection
        cn.dolphinmind.glossary.java.analyze.quality.DuplicateCodeDetector duplicateDetector =
                new cn.dolphinmind.glossary.java.analyze.quality.DuplicateCodeDetector();
        ruleIssues.addAll(duplicateDetector.findDuplicates(globalLibrary));

        // Cross-method taint (needs special handling with dependencies)
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
        return pkg;
    }

    /**
     * 核心功能：生成语义映射字典表
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

    // =====================================================================
    // 🚀 JArchitect 对标：代码指标计算方法
    // =====================================================================

    /**
     * Calculate Lines of Code (LOC) for a class.
     * Counts non-empty, non-comment-only lines in the class body.
     */
    private static int calculateClassLOC(TypeDeclaration<?> type, List<String> fileLines) {
        if (!type.getBegin().isPresent() || !type.getEnd().isPresent()) return 0;
        int start = type.getBegin().get().line - 1; // 0-indexed
        int end = Math.min(type.getEnd().get().line, fileLines.size());
        int loc = 0;
        for (int i = start; i < end && i < fileLines.size(); i++) {
            String line = fileLines.get(i).trim();
            if (!line.isEmpty() && !line.startsWith("//") && !line.startsWith("/*") && !line.startsWith("*")) {
                loc++;
            }
        }
        return loc;
    }


    /**
     * Calculate approximate cyclomatic complexity for a class.
     * Sum of decision points across all methods: if/else/for/while/switch/case/&&/||
     */
    private static double calculateClassComplexity(TypeDeclaration<?> type, List<String> fileLines) {
        double totalComplexity = 0;
        int methodCount = 0;

        for (com.github.javaparser.ast.body.MethodDeclaration method : type.getMethods()) {
            if (!method.getBody().isPresent()) continue;
            String body = method.getBody().get().toString();
            double complexity = 1; // Base complexity

            // Count decision points
            complexity += countPatternOccurrences(body, "\\bif\\s*\\(");
            complexity += countPatternOccurrences(body, "\\belse\\s+if\\s*\\(");
            complexity += countPatternOccurrences(body, "\\bfor\\s*\\(");
            complexity += countPatternOccurrences(body, "\\bwhile\\s*\\(");
            complexity += countPatternOccurrences(body, "\\bcase\\s+");
            complexity += countPatternOccurrences(body, "\\bcatch\\s*\\(");
            complexity += countPatternOccurrences(body, "&&");
            complexity += countPatternOccurrences(body, "\\|\\|");
            complexity += countPatternOccurrences(body, "\\?[^?]"); // Ternary

            totalComplexity += complexity;
            methodCount++;
        }

        return methodCount > 0 ? Math.round(totalComplexity * 100.0) / 100.0 : 1.0;
    }

    private static int countPatternOccurrences(String text, String regex) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    /**
     * Calculate inheritance depth for a class.
     * Count how many superclasses it extends.
     */
    private static int calculateInheritanceDepth(TypeDeclaration<?> type) {
        if (!(type instanceof ClassOrInterfaceDeclaration)) return 0;
        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) type;
        int depth = 0;

        // Count extended classes
        NodeList<ClassOrInterfaceType> extended = classDecl.getExtendedTypes();
        if (!extended.isEmpty()) depth++;

        return depth;
    }
}