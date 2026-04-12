package cn.dolphinmind.glossary.java.analyze.config;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Application Configuration Loader
 * 
 * Loads configuration from application.yml with environment variable override support.
 * Does not require Spring Boot — uses SnakeYAML directly.
 */
public class AppConfig {

    private static final Logger logger = Logger.getLogger(AppConfig.class.getName());
    private static volatile AppConfig instance;
    
    private final Map<String, Object> config;

    private AppConfig() {
        this.config = loadConfig();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadConfig() {
        Yaml yaml = new Yaml();
        InputStream is = getClass().getClassLoader().getResourceAsStream("application.yml");
        if (is == null) {
            logger.warning("application.yml not found, using defaults");
            return new HashMap<>();
        }
        try {
            return (Map<String, Object>) yaml.load(is);
        } catch (Exception e) {
            logger.warning("Failed to load application.yml: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }

    /**
     * Get a configuration value by dot-separated path.
     * Supports environment variable substitution via ${VAR_NAME:default}.
     */
    @SuppressWarnings("unchecked")
    public Object get(String path) {
        Map<String, Object> current = config;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            Object val = current.get(parts[i]);
            if (!(val instanceof Map)) return null;
            current = (Map<String, Object>) val;
        }
        return current.get(parts[parts.length - 1]);
    }

    public String getString(String path, String defaultValue) {
        Object val = get(path);
        if (val == null) return defaultValue;
        String str = val.toString();
        // Substitute environment variables: ${VAR:default}
        return substituteEnvVars(str, defaultValue);
    }

    public int getInt(String path, int defaultValue) {
        Object val = get(path);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        Object val = get(path);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val.toString());
    }

    // ---- Typed convenience methods ----

    public String getDatabaseUrl() {
        return getString("database.url", "jdbc:postgresql://localhost:15432/codeguardian");
    }
    public String getDatabaseUser() {
        return getString("database.username", "codeguardian");
    }
    public String getDatabasePassword() {
        return getString("database.password", "");
    }

    public String getRedisHost() {
        return getString("redis.host", "localhost");
    }
    public int getRedisPort() {
        return getInt("redis.port", 16379);
    }
    public String getRedisPassword() {
        return getString("redis.password", "");
    }

    public String getMinioEndpoint() {
        return getString("minio.endpoint", "http://localhost:19000");
    }
    public String getMinioAccessKey() {
        return getString("minio.access-key", "minioadmin");
    }
    public String getMinioSecretKey() {
        return getString("minio.secret-key", "minioadmin_secret");
    }
    public String getMinioBucket() {
        return getString("minio.bucket", "codeguardian");
    }

    public double getConfidenceThreshold() {
        Object val = get("analysis.confidence-threshold");
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.5;
    }

    private String substituteEnvVars(String value, String defaultValue) {
        // Replace ${VAR:default} with env var or default value
        int start = value.indexOf("${");
        if (start < 0) return value;
        
        int end = value.indexOf("}", start);
        if (end < 0) return value;
        
        String expr = value.substring(start + 2, end);
        String[] parts = expr.split(":", 2);
        String varName = parts[0];
        String varDefault = parts.length > 1 ? parts[1] : defaultValue;
        
        String envVal = System.getenv(varName);
        if (envVal == null) envVal = System.getProperty(varName, varDefault);
        
        String before = value.substring(0, start);
        String after = value.substring(end + 1);
        return before + envVal + after;
    }
}
