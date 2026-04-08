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
 * Parser for shell scripts (.sh) - startup, deployment, build scripts
 */
public class ShellScriptParser implements FileParser {

    @Override
    public boolean supports(Path file) {
        return file.getFileName().toString().toLowerCase().endsWith(".sh");
    }

    @Override
    public List<FileAsset> parse(Path file, String projectRoot) {
        List<FileAsset> assets = new ArrayList<>();
        try {
            String content = String.join("\n", Files.readAllLines(file, StandardCharsets.UTF_8));

            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot),
                    FileAsset.AssetType.SHELL_SCRIPT
            );

            // Extract shebang
            if (content.startsWith("#!")) {
                String shebang = content.split("\n")[0].trim();
                asset.putMeta("shebang", shebang);
            }

            // Extract environment variables
            Map<String, String> envVars = extractEnvVars(content);
            asset.putMeta("environment_variables", envVars);

            // Extract java commands (java -jar, mvn, etc.)
            List<String> javaCommands = extractJavaCommands(content);
            asset.putMeta("java_commands", javaCommands);
            asset.putMeta("java_command_count", javaCommands.size());

            // Extract startup parameters
            List<String> startupParams = extractStartupParams(content);
            asset.putMeta("startup_params", startupParams);

            // Detect script purpose
            String purpose = detectPurpose(content);
            asset.putMeta("purpose", purpose);

            // Detect Spring Boot profile activation
            List<String> profiles = extractSpringProfiles(content);
            asset.putMeta("spring_profiles", profiles);

            asset.putMeta("line_count", content.split("\n").length);
            asset.putMeta("content_preview", content.length() > 500 ? content.substring(0, 500) + "..." : content);

            assets.add(asset);
        } catch (Exception e) {
            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot),
                    FileAsset.AssetType.SHELL_SCRIPT
            );
            asset.putMeta("error", e.getMessage());
            assets.add(asset);
        }
        return assets;
    }

    @Override
    public FileAsset.AssetType getAssetType() {
        return FileAsset.AssetType.SHELL_SCRIPT;
    }

    private Map<String, String> extractEnvVars(String content) {
        Map<String, String> envVars = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("^\\s*(?:export\\s+)?([A-Z_][A-Z0-9_]*)=(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            envVars.put(matcher.group(1), matcher.group(2).trim());
        }
        return envVars;
    }

    private List<String> extractJavaCommands(String content) {
        List<String> commands = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?:^|\\s)(java\\s+-jar|java\\s+-cp|mvn\\s+\\w+)(.+?)(?:\\s&&|\\s\\||\\s;|$)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            commands.add(matcher.group(1) + matcher.group(2));
        }
        return commands;
    }

    private List<String> extractStartupParams(String content) {
        List<String> params = new ArrayList<>();
        Pattern pattern = Pattern.compile("--([a-zA-Z0-9.-]+)=(\\S+)");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            params.add("--" + matcher.group(1) + "=" + matcher.group(2));
        }
        return params;
    }

    private String detectPurpose(String content) {
        String lower = content.toLowerCase();
        if (lower.contains("start") || lower.contains("run") || lower.contains("launch")) return "startup";
        if (lower.contains("deploy") || lower.contains("publish") || lower.contains("release")) return "deployment";
        if (lower.contains("build") || lower.contains("compile") || lower.contains("package")) return "build";
        if (lower.contains("init") || lower.contains("setup") || lower.contains("install")) return "initialization";
        if (lower.contains("stop") || lower.contains("shutdown") || lower.contains("kill")) return "shutdown";
        return "utility";
    }

    private List<String> extractSpringProfiles(String content) {
        List<String> profiles = new ArrayList<>();
        Pattern pattern = Pattern.compile("--spring\\.profiles\\.active=(\\S+)");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String[] parts = matcher.group(1).split(",");
            for (String p : parts) profiles.add(p);
        }

        // Also check SPRING_PROFILES_ACTIVE env var
        Pattern envPattern = Pattern.compile("SPRING_PROFILES_ACTIVE=(\\S+)");
        Matcher envMatcher = envPattern.matcher(content);
        while (envMatcher.find()) {
            String[] parts = envMatcher.group(1).split(",");
            for (String p : parts) {
                if (!profiles.contains(p)) profiles.add(p);
            }
        }
        return profiles;
    }
}
