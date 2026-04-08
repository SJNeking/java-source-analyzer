package cn.dolphinmind.glossary.java.analyze.scanner;

import java.nio.file.Path;
import java.util.*;

/**
 * Auto-detects the type of Java project being scanned
 */
public class ProjectTypeDetector {

    public enum ProjectType {
        SPRING_BOOT,
        SPRING_CLOUD,
        MYBATIS,
        DUBBO,
        ROCKETMQ,
        PLAIN_JAVA_SE,
        UNKNOWN
    }

    private final Set<ProjectType> detectedTypes = new LinkedHashSet<>();
    private final List<String> evidence = new ArrayList<>();

    /**
     * Detect project type based on file presence and content analysis
     */
    public Map<String, Object> detect(Path projectRoot, Map<String, Object> projectAssets,
                                       Map<String, Object> javaAssets) {
        detectBuildSystem(projectRoot);
        detectFrameworks(projectAssets, javaAssets);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("primary_type", detectedTypes.isEmpty() ? "UNKNOWN" : detectedTypes.iterator().next().name());
        result.put("all_types", new ArrayList<>(detectedTypes));
        result.put("evidence", evidence);

        return result;
    }

    private void detectBuildSystem(Path projectRoot) {
        if (projectRoot.resolve("pom.xml").toFile().exists()) {
            evidence.add("pom.xml found → Maven project");
        }
        if (projectRoot.resolve("build.gradle").toFile().exists() ||
            projectRoot.resolve("settings.gradle").toFile().exists()) {
            evidence.add("build.gradle/settings.gradle found → Gradle project");
        }
    }

    private void detectFrameworks(Map<String, Object> projectAssets, Map<String, Object> javaAssets) {
        // Spring Boot detection
        if (hasConfigFile(projectAssets, "application.yml") ||
            hasConfigFile(projectAssets, "application.properties")) {
            detectedTypes.add(ProjectType.SPRING_BOOT);
            evidence.add("application.yml/properties found → Spring Boot");
        }

        // Spring Cloud detection
        if (hasConfigFile(projectAssets, "bootstrap.yml") ||
            hasConfigFile(projectAssets, "bootstrap.properties")) {
            detectedTypes.add(ProjectType.SPRING_CLOUD);
            evidence.add("bootstrap.yml/properties found → Spring Cloud");
        }

        // MyBatis detection
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mappers = (List<Map<String, Object>>)
                projectAssets.getOrDefault("mybatis_mapper", Collections.emptyList());
        if (!mappers.isEmpty()) {
            detectedTypes.add(ProjectType.MYBATIS);
            evidence.add(mappers.size() + " MyBatis mapper XML files found");
        }

        // Dubbo detection
        if (hasDependencyInPom(projectAssets, "dubbo")) {
            detectedTypes.add(ProjectType.DUBBO);
            evidence.add("Apache Dubbo dependency in pom.xml");
        }

        // RocketMQ detection
        if (hasDependencyInPom(projectAssets, "rocketmq")) {
            detectedTypes.add(ProjectType.ROCKETMQ);
            evidence.add("Apache RocketMQ dependency in pom.xml");
        }

        // Check Java packages for framework indicators
        if (javaAssets != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> assets = (List<Map<String, Object>>)
                    javaAssets.getOrDefault("assets", Collections.emptyList());
            for (Map<String, Object> asset : assets) {
                String address = (String) asset.get("address");
                if (address != null) {
                    if (address.contains("springframework.boot")) {
                        detectedTypes.add(ProjectType.SPRING_BOOT);
                    }
                    if (address.contains("springframework.cloud")) {
                        detectedTypes.add(ProjectType.SPRING_CLOUD);
                    }
                    if (address.contains("apache.dubbo")) {
                        detectedTypes.add(ProjectType.DUBBO);
                    }
                    if (address.contains("apache.rocketmq")) {
                        detectedTypes.add(ProjectType.ROCKETMQ);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean hasConfigFile(Map<String, Object> projectAssets, String fileName) {
        List<Map<String, Object>> yamlConfigs = (List<Map<String, Object>>)
                projectAssets.getOrDefault("yaml_config", Collections.emptyList());
        for (Map<String, Object> config : yamlConfigs) {
            if (fileName.equals(config.get("file_name"))) return true;
        }

        List<Map<String, Object>> propsConfigs = (List<Map<String, Object>>)
                projectAssets.getOrDefault("properties_config", Collections.emptyList());
        for (Map<String, Object> config : propsConfigs) {
            if (fileName.equals(config.get("file_name"))) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean hasDependencyInPom(Map<String, Object> projectAssets, String keyword) {
        List<Map<String, Object>> pomAssets = (List<Map<String, Object>>)
                projectAssets.getOrDefault("maven_pom", Collections.emptyList());
        for (Map<String, Object> pom : pomAssets) {
            List<Map<String, String>> deps = (List<Map<String, String>>)
                    pom.getOrDefault("dependencies", Collections.emptyList());
            for (Map<String, String> dep : deps) {
                String artifactId = dep.getOrDefault("artifactId", "").toLowerCase();
                String groupId = dep.getOrDefault("groupId", "").toLowerCase();
                if (artifactId.contains(keyword) || groupId.contains(keyword)) return true;
            }
        }
        return false;
    }
}
