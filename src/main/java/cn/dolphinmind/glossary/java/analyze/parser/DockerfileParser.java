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
 * Parser for Dockerfile
 */
public class DockerfileParser implements FileParser {

    @Override
    public boolean supports(Path file) {
        String name = file.getFileName().toString();
        return name.equals("Dockerfile") || name.startsWith("Dockerfile.");
    }

    @Override
    public List<FileAsset> parse(Path file, String projectRoot) {
        List<FileAsset> assets = new ArrayList<>();
        try {
            String content = String.join("\n", Files.readAllLines(file, StandardCharsets.UTF_8));

            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot).toString(),
                    FileAsset.AssetType.DOCKERFILE
            );

            // Extract base image
            String baseImage = extractInstruction(content, "FROM");
            asset.putMeta("base_image", baseImage);

            // Extract WORKDIR
            String workdir = extractInstruction(content, "WORKDIR");
            asset.putMeta("workdir", workdir);

            // Extract EXPOSE ports
            List<String> ports = extractPorts(content);
            asset.putMeta("exposed_ports", ports);

            // Extract ENV variables
            Map<String, String> envVars = extractEnvVars(content);
            asset.putMeta("environment_variables", envVars);

            // Extract COPY/ADD commands
            List<String> copyCommands = extractCopyCommands(content);
            asset.putMeta("copy_commands", copyCommands);

            // Extract ENTRYPOINT/CMD
            String entrypoint = extractInstruction(content, "ENTRYPOINT");
            String cmd = extractInstruction(content, "CMD");
            asset.putMeta("entrypoint", entrypoint);
            asset.putMeta("cmd", cmd);

            // Extract JAR path if present
            String jarPath = extractJarPath(content);
            asset.putMeta("jar_path", jarPath);

            // Extract multi-stage builds
            List<String> stages = extractBuildStages(content);
            asset.putMeta("build_stages", stages);
            asset.putMeta("is_multi_stage", stages.size() > 1);

            asset.putMeta("line_count", content.split("\n").length);
            asset.putMeta("content_preview", content.length() > 500 ? content.substring(0, 500) + "..." : content);

            assets.add(asset);
        } catch (Exception e) {
            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot).toString(),
                    FileAsset.AssetType.DOCKERFILE
            );
            asset.putMeta("error", e.getMessage());
            assets.add(asset);
        }
        return assets;
    }

    @Override
    public FileAsset.AssetType getAssetType() {
        return FileAsset.AssetType.DOCKERFILE;
    }

    private String extractInstruction(String content, String instruction) {
        Pattern pattern = Pattern.compile("^" + instruction + "\\s+(.+)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim().replaceAll("^\"|\"$", "");
        }
        return "";
    }

    private List<String> extractPorts(String content) {
        List<String> ports = new ArrayList<>();
        Pattern pattern = Pattern.compile("^EXPOSE\\s+(.+)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String portLine = matcher.group(1).trim();
            for (String port : portLine.split("\\s+")) {
                ports.add(port);
            }
        }
        return ports;
    }

    private Map<String, String> extractEnvVars(String content) {
        Map<String, String> envVars = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("^ENV\\s+(\\w+)=(.+)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            envVars.put(matcher.group(1), matcher.group(2).trim());
        }
        return envVars;
    }

    private List<String> extractCopyCommands(String content) {
        List<String> commands = new ArrayList<>();
        Pattern pattern = Pattern.compile("^(?:COPY|ADD)\\s+(.+)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            commands.add(matcher.group(1).trim());
        }
        return commands;
    }

    private String extractJarPath(String content) {
        Pattern pattern = Pattern.compile("(\\S+\\.jar)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private List<String> extractBuildStages(String content) {
        List<String> stages = new ArrayList<>();
        Pattern pattern = Pattern.compile("^FROM\\s+\\S+(?:\\s+AS\\s+(\\w+))?", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String stageName = matcher.group(1);
            stages.add(stageName != null ? stageName : "stage-" + (stages.size() + 1));
        }
        return stages;
    }
}
