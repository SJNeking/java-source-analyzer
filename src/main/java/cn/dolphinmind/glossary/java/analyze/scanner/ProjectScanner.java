package cn.dolphinmind.glossary.java.analyze.scanner;

import cn.dolphinmind.glossary.java.analyze.parser.FileAsset;
import cn.dolphinmind.glossary.java.analyze.parser.ParserRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scans an entire Java project directory and dispatches files to appropriate parsers
 */
public class ProjectScanner {

    private static final Set<String> EXCLUDED_DIRS = new HashSet<>(Arrays.asList(
            "target", ".git", ".idea", ".vscode", "node_modules", "dist", "build",
            ".gradle", ".mvn", "out", "bin"
    ));

    private static final Set<String> EXCLUDED_FILE_PATTERNS = new HashSet<>(Arrays.asList(
            "*.class", "*.jar", "*.war", "*.zip", "*.tar.gz", "*.so", "*.dll",
            "*.png", "*.jpg", "*.gif", "*.ico", "*.svg"
    ));

    // Performance controls
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final Set<String> WHITELIST_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".java", ".xml", ".yml", ".yaml", ".properties", ".sql", ".md",
            ".sh", ".gradle", ".json", ".conf", ".cfg", ".ini", ".txt"
    ));

    private final Path projectRoot;
    private final List<FileAsset> allAssets = new ArrayList<>();
    private final Map<FileAsset.AssetType, List<FileAsset>> assetsByType = new LinkedHashMap<>();
    private final List<String> scanErrors = new ArrayList<>();

    public ProjectScanner(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    /**
     * Detect multi-module Maven project structure.
     * Returns a list of module root directories.
     * If this is a single-module project, returns just the project root.
     */
    public List<Path> detectModules() {
        List<Path> modules = new ArrayList<>();
        Path rootPom = projectRoot.resolve("pom.xml");

        if (!Files.exists(rootPom)) {
            // No pom.xml - not a Maven project
            modules.add(projectRoot);
            return modules;
        }

        try {
            // Check if root pom has <modules> section
            String content = new String(Files.readAllBytes(rootPom));
            boolean hasModules = content.contains("<modules>");

            if (hasModules) {
                // Find all subdirectories with pom.xml
                try (java.util.stream.Stream<Path> walk = Files.walk(projectRoot, 3)) {
                    walk.filter(Files::isDirectory)
                        .filter(dir -> !isExcluded(dir))
                        .filter(dir -> Files.exists(dir.resolve("pom.xml")))
                        .filter(dir -> !dir.equals(projectRoot))
                        .forEach(modules::add);
                }

                if (modules.isEmpty()) {
                    // No sub-modules found, treat as single module
                    modules.add(projectRoot);
                }
            } else {
                // No modules section - single module project
                modules.add(projectRoot);
            }
        } catch (Exception e) {
            modules.add(projectRoot);
        }

        return modules;
    }

    /**
     * Scan a single module
     */
    public void scanModule(Path moduleRoot) throws IOException {
        Files.walk(moduleRoot)
                .filter(path -> !isExcluded(path))
                .filter(Files::isRegularFile)
                .forEach(this::processFileForModule);

        // Group assets by type
        for (FileAsset asset : allAssets) {
            assetsByType.computeIfAbsent(asset.getAssetType(), k -> new ArrayList<>()).add(asset);
        }
    }

    private void processFileForModule(Path path) {
        try {
            // Check file size
            long fileSize = Files.size(path);
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                scanErrors.add("Skipped large file: " + projectRoot.relativize(path) + " (" + fileSize + " bytes)");
                return;
            }

            // Check extension whitelist
            String fileName = path.getFileName().toString().toLowerCase();
            boolean hasWhitelistedExt = WHITELIST_EXTENSIONS.stream()
                    .anyMatch(ext -> fileName.endsWith(ext));
            if (!hasWhitelistedExt) {
                return; // Silently skip non-whitelisted files
            }

            cn.dolphinmind.glossary.java.analyze.parser.FileParser parser = ParserRegistry.findParser(path);
            if (parser != null) {
                List<FileAsset> assets = parser.parse(path, projectRoot.toString());
                allAssets.addAll(assets);
            }
        } catch (Exception e) {
            scanErrors.add("Failed to parse: " + projectRoot.relativize(path) + " - " + e.getMessage());
        }
    }

    /**
     * Get all parsed assets
     */
    public List<FileAsset> getAllAssets() {
        return Collections.unmodifiableList(allAssets);
    }

    /**
     * Get assets grouped by type
     */
    public Map<FileAsset.AssetType, List<FileAsset>> getAssetsByType() {
        return Collections.unmodifiableMap(assetsByType);
    }

    /**
     * Get scan errors
     */
    public List<String> getScanErrors() {
        return Collections.unmodifiableList(scanErrors);
    }

    /**
     * Get scan summary
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_files_scanned", allAssets.size());
        summary.put("asset_types", assetsByType.keySet().stream()
                .map(Enum::name)
                .collect(Collectors.toList()));

        for (Map.Entry<FileAsset.AssetType, List<FileAsset>> entry : assetsByType.entrySet()) {
            summary.put(entry.getKey().name().toLowerCase() + "_count", entry.getValue().size());
        }

        summary.put("errors", scanErrors.size());
        return summary;
    }

    private void processFile(Path path) {
        try {
            // Check file size
            long fileSize = Files.size(path);
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                scanErrors.add("Skipped large file: " + projectRoot.relativize(path) + " (" + fileSize + " bytes)");
                return;
            }

            // Check extension whitelist
            String fileName = path.getFileName().toString().toLowerCase();
            boolean hasWhitelistedExt = WHITELIST_EXTENSIONS.stream()
                    .anyMatch(ext -> fileName.endsWith(ext));
            if (!hasWhitelistedExt) {
                return; // Silently skip non-whitelisted files
            }

            cn.dolphinmind.glossary.java.analyze.parser.FileParser parser = ParserRegistry.findParser(path);
            if (parser != null) {
                List<FileAsset> assets = parser.parse(path, projectRoot.toString());
                allAssets.addAll(assets);
            }
        } catch (Exception e) {
            scanErrors.add("Failed to parse: " + projectRoot.relativize(path) + " - " + e.getMessage());
        }
    }

    private boolean isExcluded(Path path) {
        String pathStr = path.toString();
        String sep = java.io.File.separator;

        // Check excluded directories - match any path component
        String[] pathParts = pathStr.split(java.util.regex.Pattern.quote(sep));
        for (String part : pathParts) {
            if (EXCLUDED_DIRS.contains(part)) {
                return true;
            }
        }

        // Check excluded file patterns
        String fileName = path.getFileName().toString().toLowerCase();
        for (String pattern : EXCLUDED_FILE_PATTERNS) {
            if (fileName.endsWith(pattern.replace("*", ""))) {
                return true;
            }
        }

        return false;
    }
}
