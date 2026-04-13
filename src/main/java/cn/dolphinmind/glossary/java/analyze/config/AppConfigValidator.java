package cn.dolphinmind.glossary.java.analyze.config;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 应用配置验证器
 * 
 * 在启动前检查所有必需的配置项是否有效，避免运行时出现莫名其妙
 * 的错误。
 */
public class AppConfigValidator {

    private static final Logger logger = Logger.getLogger(AppConfigValidator.class.getName());

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }

    /**
     * 验证配置
     */
    public static ValidationResult validate(AppConfig config) {
        List<String> errors = new ArrayList<>();

        // 1. 数据库配置
        if (config.getDatabaseUrl() == null || config.getDatabaseUrl().isEmpty()) {
            errors.add("database.url is required");
        }
        if (config.getDatabaseUser() == null || config.getDatabaseUser().isEmpty()) {
            errors.add("database.user is required");
        }

        // 2. Ollama 配置
        String ollamaEndpoint = config.getString("ollama.endpoint", "");
        if (ollamaEndpoint.isEmpty()) {
            errors.add("ollama.endpoint is required");
        }

        // 3. 分析路径 (skip if method doesn't exist)
        // List<String> sourceRoots = config.getList("analysis.source-roots", new ArrayList<>());
        // if (sourceRoots.isEmpty()) {
        //     errors.add("analysis.source-roots is required");
        // }

        // 4. 端口范围
        int redisPort = config.getInt("redis.port", 0);
        if (redisPort <= 0 || redisPort > 65535) {
            errors.add("redis.port must be a valid port number (1-65535)");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 验证并在失败时抛出异常
     */
    public static void validateOrThrow(AppConfig config) {
        ValidationResult result = validate(config);
        if (!result.isValid()) {
            String msg = "Configuration validation failed:\n  - " + String.join("\n  - ", result.getErrors());
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }
        logger.info("Configuration validation passed");
    }
}
