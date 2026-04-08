package cn.dolphinmind.glossary.java.analyze.translate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/**
 * Semantic translation service: handles dictionary loading and identifier translation.
 *
 * Extracted from SourceUniversePro to reduce class size and improve maintainability.
 */
public class SemanticTranslator {

    private static final String ANALYZER_VERSION = "1.0-M5";

    // Dictionaries
    private JsonObject tagDictionary;
    private final Map<String, String> techInstructionSet = new HashMap<>();
    private final Map<String, String> globalBaseDictionary = new HashMap<>();
    private final Map<String, String[]> tagLibrary = new HashMap<>();
    private final Map<String, String> techTermMap = new HashMap<>();
    private final Map<String, String> projectGlossary = new HashMap<>();

    private String currentProjectRoot = "";

    public SemanticTranslator() {
        initTechTermMap();
    }

    /**
     * Get the current analyzer version for cache invalidation.
     */
    public static String getAnalyzerVersion() {
        return ANALYZER_VERSION;
    }

    /**
     * Load all dictionaries from classpath and project directories.
     */
    public void loadDictionaries() {
        loadTagDictionary();
        initTagLibrary();
    }

    /**
     * Load project-specific glossary for incremental learning.
     */
    public void loadProjectGlossary(String projectRoot) {
        currentProjectRoot = projectRoot;
        File glossaryFile = new File(projectRoot + "/.universe/tech-glossary.json");
        if (glossaryFile.exists()) {
            try {
                JsonObject obj = JsonParser.parseReader(new FileReader(glossaryFile)).getAsJsonObject();
                for (String key : obj.keySet()) {
                    projectGlossary.put(key.toLowerCase(), obj.get(key).getAsString());
                }
            } catch (Exception e) {
                System.err.println("⚠️ 加载项目字典失败: " + e.getMessage());
            }
        }
    }

    /**
     * Save project-specific glossary for incremental learning.
     */
    public void saveProjectGlossary() {
        if (projectGlossary.isEmpty()) return;
        try {
            File dir = new File(currentProjectRoot + "/.universe");
            if (!dir.exists()) dir.mkdirs();

            JsonObject obj = new JsonObject();
            for (Map.Entry<String, String> entry : projectGlossary.entrySet()) {
                obj.addProperty(entry.getKey(), entry.getValue());
            }

            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            try (FileWriter fw = new FileWriter(currentProjectRoot + "/.universe/tech-glossary.json")) {
                gson.toJson(obj, fw);
            }
        } catch (Exception e) {
            System.err.println("⚠️ 保存项目字典失败: " + e.getMessage());
        }
    }

    /**
     * Translate an identifier to Chinese with bilingual annotations.
     */
    public String translateIdentifier(String name) {
        if (name == null || name.isEmpty()) return "";

        // 1. Try direct match
        String directMatch = lookupTerm(name);
        if (directMatch != null) return directMatch;

        // 2. Split camelCase and translate each part
        List<String> tokens = splitCamelCase(name);
        StringBuilder result = new StringBuilder();

        for (String token : tokens) {
            if (token.length() <= 1 && !token.matches("[IOA]")) {
                result.append(token);
                continue;
            }

            String cn = lookupTerm(token);
            if (cn == null) {
                cn = inferBySuffix(token);
            }
            result.append(cn != null ? cn : token);
        }

        return result.toString();
    }

    /**
     * Get the project glossary for external access.
     */
    public Map<String, String> getProjectGlossary() {
        return Collections.unmodifiableMap(projectGlossary);
    }

    // ---- Private Methods ----

    private void loadTagDictionary() {
        try {
            java.net.URL resource = SemanticTranslator.class.getClassLoader().getResource("tag-dictionary.json");
            if (resource != null) {
                com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(
                        new java.io.InputStreamReader(resource.openStream(), StandardCharsets.UTF_8));
                reader.setStrictness(com.google.gson.Strictness.LENIENT);
                tagDictionary = JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception e) {
            System.err.println("⚠️ 动态标签字典加载失败: " + e.getMessage());
        }

        // Load tech instruction set
        try {
            java.net.URL resource = SemanticTranslator.class.getClassLoader().getResource("tech-instruction-set.json");
            if (resource != null) {
                JsonObject dictObj = JsonParser.parseReader(
                        new java.io.InputStreamReader(resource.openStream(), StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : dictObj.keySet()) {
                    techInstructionSet.put(key.toLowerCase(), dictObj.get(key).getAsString());
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ 技术指令集加载失败: " + e.getMessage());
        }

        // Load global base dictionary
        try {
            java.net.URL resource = SemanticTranslator.class.getClassLoader().getResource("cleaned-english-chinese-mapping.json");
            if (resource != null) {
                JsonObject dictObj = JsonParser.parseReader(
                        new java.io.InputStreamReader(resource.openStream(), StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : dictObj.keySet()) {
                    globalBaseDictionary.put(key.toLowerCase(), dictObj.get(key).getAsString());
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ 全局基础字典加载失败: " + e.getMessage());
        }
    }

    private void initTagLibrary() {
        if (tagDictionary == null || !tagDictionary.has("tags")) return;
        JsonObject tags = tagDictionary.getAsJsonObject("tags");
        for (String id : tags.keySet()) {
            JsonObject tag = tags.getAsJsonObject(id);
            tagLibrary.put(id, new String[]{
                tag.get("cn").getAsString(),
                tag.get("en").getAsString(),
                tag.get("desc").getAsString()
            });
        }
    }

    private void initTechTermMap() {
        techTermMap.put("Asynchronous", "异步(Asynchronous)");
        techTermMap.put("Synchronization", "同步(Synchronization)");
        techTermMap.put("Serialization", "序列化(Serialization)");
        techTermMap.put("Concurrency", "并发(Concurrency)");
        techTermMap.put("Transaction", "事务(Transaction)");
        techTermMap.put("Configuration", "配置(Configuration)");
        techTermMap.put("Exception", "异常(Exception)");
        techTermMap.put("Implementation", "实现(Implementation)");
        techTermMap.put("Abstract", "抽象(Abstract)");
        techTermMap.put("Interface", "接口(Interface)");
        techTermMap.put("Factory", "工厂(Factory)");
        techTermMap.put("Proxy", "代理(Proxy)");
        techTermMap.put("Adapter", "适配器(Adapter)");
        techTermMap.put("Strategy", "策略(Strategy)");
        techTermMap.put("Observer", "观察者(Observer)");
        techTermMap.put("Singleton", "单例(Singleton)");
        techTermMap.put("Registry", "注册中心(Registry)");
        techTermMap.put("Protocol", "协议(Protocol)");
        techTermMap.put("Transport", "传输层(Transport)");
        techTermMap.put("Endpoint", "端点(Endpoint)");
        techTermMap.put("Channel", "通道(Channel)");
        techTermMap.put("Pipeline", "管道(Pipeline)");
        techTermMap.put("Handler", "处理器(Handler)");
        techTermMap.put("Codec", "编解码器(Codec)");
        techTermMap.put("Persistence", "持久化(Persistence)");
        techTermMap.put("Repository", "仓储(Repository)");
        techTermMap.put("Connection Pool", "连接池(Connection Pool)");
        techTermMap.put("Cache", "缓存(Cache)");
        techTermMap.put("Eviction", "驱逐(Eviction)");
        techTermMap.put("Sharding", "分片(Sharding)");
        techTermMap.put("Authentication", "认证(Authentication)");
        techTermMap.put("Authorization", "授权(Authorization)");
        techTermMap.put("Encryption", "加密(Encryption)");
        techTermMap.put("Validation", "校验(Validation)");
        techTermMap.put("Initialization", "初始化(Initialization)");
        techTermMap.put("Invocation", "调用(Invocation)");
        techTermMap.put("Reflection", "反射(Reflection)");
        techTermMap.put("Annotation", "注解(Annotation)");
        techTermMap.put("Metrics", "指标(Metrics)");
        techTermMap.put("Tracker", "追踪器(Tracker)");
        techTermMap.put("Bag", "并发容器(Bag)");
        techTermMap.put("Pool", "池(Pool)");
        techTermMap.put("DataSource", "数据源(DataSource)");
        techTermMap.put("Statement", "语句(Statement)");
        techTermMap.put("ResultSet", "结果集(ResultSet)");
        techTermMap.put("Callable", "可调用(Callable)");
        techTermMap.put("PreparedStatement", "预编译语句(PreparedStatement)");
        techTermMap.put("MetaData", "元数据(MetaData)");
        techTermMap.put("Histogram", "直方图(Histogram)");
        techTermMap.put("Prometheus", "普罗米修斯监控(Prometheus)");
        techTermMap.put("Micrometer", "微计量(Micrometer)");
        techTermMap.put("JNDI", "Java命名与目录接口(JNDI)");
        techTermMap.put("MXBean", "管理扩展Bean(MXBean)");
        techTermMap.put("Concurrent", "并发的(Concurrent)");
        techTermMap.put("FastList", "快速列表(FastList)");
        techTermMap.put("Isolation", "隔离(Isolation)");
        techTermMap.put("Leak", "泄漏(Leak)");
        techTermMap.put("Suspend", "挂起(Suspend)");
        techTermMap.put("Resume", "恢复(Resume)");
        techTermMap.put("Override", "重写/覆盖(Override)");
        techTermMap.put("Util", "工具(Util)");
        techTermMap.put("Provider", "提供者(Provider)");
        techTermMap.put("Stats", "统计信息(Stats)");
        techTermMap.put("Clock", "时钟(Clock)");
        techTermMap.put("Source", "源(Source)");
        techTermMap.put("Property", "属性(Property)");
    }

    private String lookupTerm(String term) {
        if (term.isEmpty()) return null;
        String key = term.toLowerCase();

        // Level 1: Project glossary
        if (projectGlossary.containsKey(key)) {
            return projectGlossary.get(key);
        }

        // Level 2: Tech instruction set
        if (techInstructionSet.containsKey(key)) {
            return techInstructionSet.get(key);
        }

        // Level 3: Global base dictionary (only for tech root words)
        if (isTechRootWord(term) && globalBaseDictionary.containsKey(key)) {
            return globalBaseDictionary.get(key);
        }

        return null;
    }

    private boolean isTechRootWord(String word) {
        String w = word.toLowerCase();
        return w.length() > 3 && (w.contains("net") || w.contains("sys") || w.contains("data") ||
                                  w.contains("code") || w.contains("log") || w.contains("file") ||
                                  w.contains("time") || w.contains("user") || w.contains("config"));
    }

    private List<String> splitCamelCase(String input) {
        if (input == null || input.isEmpty()) return Collections.emptyList();
        String[] parts = input.split("(?=[A-Z])");
        List<String> result = new ArrayList<String>();
        for (String part : parts) {
            if (!part.isEmpty()) result.add(part);
        }
        return result;
    }

    private String inferBySuffix(String token) {
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
}
