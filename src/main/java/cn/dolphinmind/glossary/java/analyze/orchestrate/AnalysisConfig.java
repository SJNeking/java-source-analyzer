package cn.dolphinmind.glossary.java.analyze.orchestrate;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Analysis configuration: holds all user-provided and derived settings.
 *
 * Eliminates hardcoded paths and magic values from the main analysis pipeline.
 */
public class AnalysisConfig {

    // User-provided
    private String sourceRoot;
    private String outputDir;
    private String artifactName;
    private String version;
    private String internalPkgPrefix;
    private String rulesConfigPath;

    // WebSocket / Real-time streaming
    private boolean websocketEnabled = false;
    private int websocketPort = 8887;

    // Derived
    private String frameworkName;
    private String detectedVersion;

    // Defaults
    private static final String DEFAULT_INTERNAL_PKG_PREFIX = "java";
    private static final String DEFAULT_OUTPUT_DIR = "analysis-output";

    public AnalysisConfig() {}

    // --- Getters / Setters ---

    public String getSourceRoot() { return sourceRoot; }
    public void setSourceRoot(String sourceRoot) { this.sourceRoot = sourceRoot; }
    public Path getSourceRootPath() { return Paths.get(sourceRoot); }

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
    public Path getOutputDirPath() { return Paths.get(outputDir); }

    public String getArtifactName() { return artifactName; }
    public void setArtifactName(String artifactName) { this.artifactName = artifactName; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getInternalPkgPrefix() {
        return internalPkgPrefix != null ? internalPkgPrefix : DEFAULT_INTERNAL_PKG_PREFIX;
    }
    public void setInternalPkgPrefix(String internalPkgPrefix) { this.internalPkgPrefix = internalPkgPrefix; }

    public String getRulesConfigPath() { return rulesConfigPath; }
    public void setRulesConfigPath(String rulesConfigPath) { this.rulesConfigPath = rulesConfigPath; }

    public boolean isWebsocketEnabled() { return websocketEnabled; }
    public void setWebsocketEnabled(boolean websocketEnabled) { this.websocketEnabled = websocketEnabled; }
    public int getWebsocketPort() { return websocketPort; }
    public void setWebsocketPort(int websocketPort) { this.websocketPort = websocketPort; }

    public String getFrameworkName() { return frameworkName; }
    public void setFrameworkName(String frameworkName) { this.frameworkName = frameworkName; }

    public String getDetectedVersion() { return detectedVersion; }
    public void setDetectedVersion(String detectedVersion) { this.detectedVersion = detectedVersion; }

    public String getEffectiveVersion() {
        return version != null ? version : (detectedVersion != null ? detectedVersion : "unknown");
    }

    /**
     * Apply default values for missing configuration.
     */
    public void applyDefaults() {
        if (outputDir == null || outputDir.isEmpty()) {
            outputDir = System.getProperty("user.dir") + java.io.File.separator + DEFAULT_OUTPUT_DIR;
            System.out.println("⚠️  未指定 --outputDir，使用默认路径: " + outputDir);
        }
    }

    /**
     * Get cache directory for incremental analysis.
     */
    public Path getCacheDir() {
        return getSourceRootPath().resolve(".universe").resolve("cache");
    }

}
