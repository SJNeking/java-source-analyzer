package cn.dolphinmind.glossary.java.analyze.parser;

import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for application.yml, application.properties, and bootstrap.yml config files
 */
public class ConfigFileParser implements FileParser {

    @Override
    public boolean supports(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        // Support environment-specific configs: application-dev.yml, application-prod.yml, etc.
        return name.matches("application.*\\.(yml|yaml|properties)") ||
               name.matches("bootstrap.*\\.(yml|yaml)");
    }

    @Override
    public List<FileAsset> parse(Path file, String projectRoot) {
        List<FileAsset> assets = new ArrayList<>();
        try {
            String content = String.join("\n", Files.readAllLines(file, StandardCharsets.UTF_8));
            String fileName = file.getFileName().toString();
            boolean isProperties = fileName.endsWith(".properties");

            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot),
                    isProperties ? FileAsset.AssetType.PROPERTIES_CONFIG : FileAsset.AssetType.YAML_CONFIG
            );

            // Extract key configurations
            Map<String, String> configItems = isProperties
                    ? extractPropertiesConfig(content)
                    : extractYamlConfig(content);

            asset.putMeta("config_items", configItems);
            asset.putMeta("server_port", configItems.getOrDefault("server.port", ""));
            asset.putMeta("datasource_url", configItems.getOrDefault("spring.datasource.url", ""));
            asset.putMeta("datasource_driver", configItems.getOrDefault("spring.datasource.driver-class-name", ""));
            asset.putMeta("redis_host", configItems.getOrDefault("spring.redis.host",
                    configItems.getOrDefault("spring.redis.cluster.nodes", "")));
            asset.putMeta("active_profiles", configItems.getOrDefault("spring.profiles.active", ""));
            asset.putMeta("application_name", configItems.getOrDefault("spring.application.name", ""));
            asset.putMeta("log_level", configItems.getOrDefault("logging.level.root", ""));
            asset.putMeta("line_count", content.split("\n").length);

            // Detect middleware
            List<String> middleware = detectMiddleware(configItems);
            asset.putMeta("middleware", middleware);

            assets.add(asset);
        } catch (Exception e) {
            String fileName = file.getFileName().toString();
            boolean isProperties = fileName.endsWith(".properties");
            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot),
                    isProperties ? FileAsset.AssetType.PROPERTIES_CONFIG : FileAsset.AssetType.YAML_CONFIG
            );
            asset.putMeta("error", e.getMessage());
            assets.add(asset);
        }
        return assets;
    }

    @Override
    public FileAsset.AssetType getAssetType() {
        return FileAsset.AssetType.YAML_CONFIG;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractYamlConfig(String content) {
        Map<String, String> config = new LinkedHashMap<>();
        try {
            Yaml yaml = new Yaml();
            Object obj = yaml.load(new StringReader(content));
            if (obj instanceof Map) {
                flattenYaml((Map<String, Object>) obj, config, "");
            }
        } catch (Exception e) {
            // Fallback to line-by-line if SnakeYAML fails
            config.put("_parse_error", e.getMessage());
            fallbackYamlParse(content, config);
        }
        return config;
    }

    @SuppressWarnings("unchecked")
    private void flattenYaml(Map<String, Object> map, Map<String, String> result, String prefix) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flattenYaml((Map<String, Object>) value, result, key);
            } else if (value != null) {
                result.put(key, value.toString());
            }
        }
    }

    private void fallbackYamlParse(String content, Map<String, String> config) {
        String[] lines = content.split("\n");
        String currentPrefix = "";

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) continue;

            int indent = line.length() - line.trim().length();
            if (indent == 0) currentPrefix = "";

            int colonIdx = trimmed.indexOf(':');
            if (colonIdx > 0) {
                String key = trimmed.substring(0, colonIdx).trim();
                String value = trimmed.substring(colonIdx + 1).trim();
                String fullKey = currentPrefix.isEmpty() ? key : currentPrefix + "." + key;

                if (!value.isEmpty() && !value.startsWith("#")) {
                    int commentIdx = value.indexOf('#');
                    if (commentIdx > 0) value = value.substring(0, commentIdx).trim();
                    config.put(fullKey, value);
                    if (indent == 0) currentPrefix = key;
                }
            }
        }
    }

    private Map<String, String> extractPropertiesConfig(String content) {
        Map<String, String> config = new LinkedHashMap<>();
        String[] lines = content.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) continue;

            int eqIdx = trimmed.indexOf('=');
            if (eqIdx > 0) {
                String key = trimmed.substring(0, eqIdx).trim();
                String value = trimmed.substring(eqIdx + 1).trim();
                config.put(key, value);
            }
        }
        return config;
    }

    private List<String> detectMiddleware(Map<String, String> config) {
        List<String> middleware = new ArrayList<>();
        String keys = String.join(" ", config.keySet());

        if (keys.contains("spring.datasource") || keys.contains("spring.jpa")) {
            middleware.add("Database (JDBC/JPA)");
        }
        if (keys.contains("spring.redis")) {
            middleware.add("Redis");
        }
        if (keys.contains("spring.rabbitmq") || keys.contains("spring.kafka")) {
            middleware.add("Message Queue");
        }
        if (keys.contains("spring.cloud.nacos") || keys.contains("spring.cloud.consul") ||
            keys.contains("spring.cloud.eureka")) {
            middleware.add("Service Registry");
        }
        if (keys.contains("spring.cloud.config") || keys.contains("spring.cloud.nacos.config")) {
            middleware.add("Config Center");
        }
        if (keys.contains("spring.kafka")) {
            middleware.add("Kafka");
        }
        if (keys.contains("spring.rabbitmq")) {
            middleware.add("RabbitMQ");
        }
        if (keys.contains("spring.elasticsearch")) {
            middleware.add("Elasticsearch");
        }
        if (keys.contains("spring.mongodb") || keys.contains("spring.data.mongodb")) {
            middleware.add("MongoDB");
        }

        return middleware;
    }
}
