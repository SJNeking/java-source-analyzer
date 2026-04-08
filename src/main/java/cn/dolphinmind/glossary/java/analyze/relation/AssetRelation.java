package cn.dolphinmind.glossary.java.analyze.relation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a cross-file relationship between two assets
 */
public class AssetRelation {

    public enum RelationType {
        JAVA_TO_MAPPER,      // Java interface -> MyBatis Mapper XML
        CONFIG_TO_CLASS,     // Config key -> @Value/@ConfigurationProperties class
        SQL_TO_ENTITY,       // SQL table -> Java Entity class
        POM_TO_USAGE,        // POM dependency -> actual imported classes
        DOCKER_TO_JAR,       // Dockerfile -> startup JAR
        SHELL_TO_PROFILE,    // Shell script -> startup command/profile
        CONTROLLER_TO_SERVICE,// Controller -> Service layer
        SERVICE_TO_REPOSITORY // Service -> Repository/Mapper
    }

    private final String sourcePath;
    private final String sourceAsset;
    private final String targetPath;
    private final String targetAsset;
    private final RelationType type;
    private final String evidence;
    private final double confidence;

    public AssetRelation(String sourcePath, String sourceAsset, String targetPath,
                         String targetAsset, RelationType type, String evidence, double confidence) {
        this.sourcePath = sourcePath;
        this.sourceAsset = sourceAsset;
        this.targetPath = targetPath;
        this.targetAsset = targetAsset;
        this.type = type;
        this.evidence = evidence;
        this.confidence = confidence;
    }

    public String getSourcePath() { return sourcePath; }
    public String getSourceAsset() { return sourceAsset; }
    public String getTargetPath() { return targetPath; }
    public String getTargetAsset() { return targetAsset; }
    public RelationType getType() { return type; }
    public String getEvidence() { return evidence; }
    public double getConfidence() { return confidence; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("source_path", sourcePath);
        map.put("source_asset", sourceAsset);
        map.put("target_path", targetPath);
        map.put("target_asset", targetAsset);
        map.put("relation_type", type.name());
        map.put("evidence", evidence);
        map.put("confidence", confidence);
        return map;
    }
}
