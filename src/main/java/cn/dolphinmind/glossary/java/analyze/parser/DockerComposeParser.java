package cn.dolphinmind.glossary.java.analyze.parser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for docker-compose.yml / docker-compose.yaml files
 */
public class DockerComposeParser implements FileParser {

    @Override
    public boolean supports(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.equals("docker-compose.yml") || name.equals("docker-compose.yaml");
    }

    @Override
    public List<FileAsset> parse(Path file, String projectRoot) {
        List<FileAsset> assets = new ArrayList<>();
        try {
            String content = String.join("\n", Files.readAllLines(file, StandardCharsets.UTF_8));

            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot),
                    FileAsset.AssetType.UNKNOWN // Will categorize as docker-compose
            );
            asset.putMeta("asset_type", "DOCKER_COMPOSE");

            // Extract services
            List<Map<String, Object>> services = extractServices(content);
            asset.putMeta("services", services);
            asset.putMeta("service_count", services.size());

            // Extract networks
            List<String> networks = extractNetworks(content);
            asset.putMeta("networks", networks);

            // Extract volumes
            List<String> volumes = extractVolumes(content);
            asset.putMeta("volumes", volumes);

            // Detect middleware stack
            List<String> middleware = detectMiddleware(services);
            asset.putMeta("middleware", middleware);

            asset.putMeta("line_count", content.split("\n").length);
            asset.putMeta("content_preview", content.length() > 500 ? content.substring(0, 500) + "..." : content);

            assets.add(asset);
        } catch (Exception e) {
            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot),
                    FileAsset.AssetType.UNKNOWN
            );
            asset.putMeta("asset_type", "DOCKER_COMPOSE");
            asset.putMeta("error", e.getMessage());
            assets.add(asset);
        }
        return assets;
    }

    @Override
    public FileAsset.AssetType getAssetType() {
        return FileAsset.AssetType.UNKNOWN;
    }

    private List<Map<String, Object>> extractServices(String content) {
        List<Map<String, Object>> services = new ArrayList<>();
        String[] lines = content.split("\n");
        boolean inServices = false;
        String currentService = null;
        Map<String, String> currentServiceData = new LinkedHashMap<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("services:")) {
                inServices = true;
                continue;
            }
            if (!inServices) continue;

            // Top-level key (not indented) means end of services
            if (!line.startsWith(" ") && !line.startsWith("\t") && !trimmed.isEmpty() && !trimmed.startsWith("#")) {
                if (currentService != null) {
                    services.add(new LinkedHashMap<>(currentServiceData));
                    currentServiceData.clear();
                    currentService = null;
                }
                inServices = false;
                continue;
            }

            // Service name (2 spaces indent)
            if (line.startsWith("  ") && !line.startsWith("    ") && trimmed.endsWith(":")) {
                if (currentService != null) {
                    services.add(new LinkedHashMap<>(currentServiceData));
                    currentServiceData.clear();
                }
                currentService = trimmed.substring(0, trimmed.length() - 1);
                continue;
            }

            // Service property (4+ spaces indent)
            if (currentService != null && line.startsWith("    ") && trimmed.contains(":")) {
                int colonIdx = trimmed.indexOf(':');
                String key = trimmed.substring(0, colonIdx).trim();
                String value = trimmed.substring(colonIdx + 1).trim();
                currentServiceData.put(key, value);
            }
        }

        // Last service
        if (currentService != null && !currentServiceData.isEmpty()) {
            services.add(new LinkedHashMap<>(currentServiceData));
        }

        return services;
    }

    private List<String> extractNetworks(String content) {
        List<String> networks = new ArrayList<>();
        Pattern pattern = Pattern.compile("^networks:\\s*$", Pattern.MULTILINE);
        if (pattern.matcher(content).find()) {
            // Simple extraction - just note presence
            networks.add("defined");
        }
        return networks;
    }

    private List<String> extractVolumes(String content) {
        List<String> volumes = new ArrayList<>();
        Pattern pattern = Pattern.compile("^volumes:\\s*$", Pattern.MULTILINE);
        if (pattern.matcher(content).find()) {
            volumes.add("defined");
        }
        return volumes;
    }

    private List<String> detectMiddleware(List<Map<String, Object>> services) {
        List<String> middleware = new ArrayList<>();
        for (Map<String, Object> service : services) {
            String image = (String) service.getOrDefault("image", "");
            if (image.contains("mysql") || image.contains("postgres") || image.contains("mariadb")) {
                middleware.add("Database");
            } else if (image.contains("redis")) {
                middleware.add("Redis");
            } else if (image.contains("rabbitmq")) {
                middleware.add("RabbitMQ");
            } else if (image.contains("kafka")) {
                middleware.add("Kafka");
            } else if (image.contains("elasticsearch")) {
                middleware.add("Elasticsearch");
            } else if (image.contains("mongodb") || image.contains("mongo")) {
                middleware.add("MongoDB");
            } else if (image.contains("nacos") || image.contains("consul") || image.contains("eureka")) {
                middleware.add("Service Registry");
            }
        }
        return middleware;
    }
}
