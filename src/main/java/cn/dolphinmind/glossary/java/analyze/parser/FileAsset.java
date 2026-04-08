package cn.dolphinmind.glossary.java.analyze.parser;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified project asset model - represents any parsed file asset
 */
public class FileAsset {

    public enum AssetType {
        JAVA_SOURCE,
        MAVEN_POM,
        YAML_CONFIG,
        PROPERTIES_CONFIG,
        SQL_SCRIPT,
        MYBATIS_MAPPER,
        DOCKERFILE,
        DOCKER_COMPOSE,
        SHELL_SCRIPT,
        LOG_CONFIG,
        MARKDOWN_DOC,
        SPRING_XML,
        GRADLE_SCRIPT,
        UNKNOWN
    }

    private String path;
    private String moduleName;
    private AssetType assetType;
    private String fileName;
    private Map<String, Object> metadata;

    public FileAsset(String path, AssetType assetType) {
        this.path = path;
        this.assetType = assetType;
        this.metadata = new LinkedHashMap<>();
        this.fileName = path.substring(path.lastIndexOf('/') + 1);
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Map<String, Object> getMetadata() { return metadata; }

    public void putMeta(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMeta(String key) {
        return metadata.get(key);
    }

    /**
     * Convert to JSON-compatible Map for output
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", path);
        result.put("module", moduleName != null ? moduleName : "default");
        result.put("asset_type", assetType.name());
        result.put("file_name", fileName);
        result.putAll(metadata);
        return result;
    }
}
