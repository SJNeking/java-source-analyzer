package cn.dolphinmind.glossary.java.analyze;

import java.util.*;

public class ScannerContext {
    private final String projectRoot;
    private final String internalPackagePrefix;
    private final String version;
    private final Set<String> stateKeywords = new HashSet<>(Arrays.asList(
            "int", "long", "short", "byte", "float", "double", "boolean", "char",
            "string", "java.util", "java.lang", "atomic", "concurrent", "collection"
    ));

    public ScannerContext(String projectRoot, String internalPackagePrefix, String version) {
        this.projectRoot = projectRoot;
        this.internalPackagePrefix = internalPackagePrefix;
        this.version = version;
    }

    /**
     * 严格的角色分类逻辑
     */
    public String classify(String fullType) {
        if (fullType == null || fullType.isEmpty()) return "UNKNOWN";

        String lowType = fullType.toLowerCase();
        // 1. 判定为状态 (基本类型及标准库)
        if (stateKeywords.stream().anyMatch(lowType::contains)) {
            return "INTERNAL_STATE";
        }
        // 2. 判定为内部组件 (严格匹配包前缀)
        if (fullType.startsWith(internalPackagePrefix)) {
            return "INTERNAL_COMPONENT";
        }
        // 3. 其余判定为外部服务
        return "EXTERNAL_SERVICE";
    }

    public String getProjectRoot() { return projectRoot; }
    public String getVersion() { return version; }
    public String getInternalPackagePrefix() { return internalPackagePrefix; }
}