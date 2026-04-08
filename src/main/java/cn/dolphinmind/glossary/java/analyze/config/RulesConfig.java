package cn.dolphinmind.glossary.java.analyze.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Rules configuration system.
 *
 * Loads rule settings from a JSON file that controls:
 * - Whether a rule is enabled
 * - Rule severity override
 * - Rule threshold override
 *
 * Default rules.json location:
 * 1. --rules-config <path> CLI argument
 * 2. ./rules.json (current directory)
 * 3. ~/.java-source-analyzer/rules.json (user home)
 * 4. Built-in defaults (hardcoded)
 *
 * Example rules.json:
 * {
 *   "rules": {
 *     "RSPEC-159": { "enabled": false },
 *     "RSPEC-138": { "threshold": 50 },
 *     "RSPEC-3776-CFG": { "threshold": 10, "severity": "CRITICAL" }
 *   },
 *   "quality_gate": {
 *     "max_critical": 0,
 *     "max_major": 5,
 *     "max_total": 50,
 *     "max_debt_ratio_pct": 10.0
 *   }
 * }
 */
public class RulesConfig {

    public static class RuleSetting {
        private boolean enabled = true;
        private String severity = null;  // Override severity
        private Integer threshold = null; // Override threshold

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public Integer getThreshold() { return threshold; }
        public void setThreshold(Integer threshold) { this.threshold = threshold; }
    }

    public static class QualityGateConfig {
        private int maxCritical = 0;
        private int maxMajor = 0;
        private int maxTotal = 10;
        private double maxDebtRatioPct = 5.0;

        public int getMaxCritical() { return maxCritical; }
        public void setMaxCritical(int maxCritical) { this.maxCritical = maxCritical; }
        public int getMaxMajor() { return maxMajor; }
        public void setMaxMajor(int maxMajor) { this.maxMajor = maxMajor; }
        public int getMaxTotal() { return maxTotal; }
        public void setMaxTotal(int maxTotal) { this.maxTotal = maxTotal; }
        public double getMaxDebtRatioPct() { return maxDebtRatioPct; }
        public void setMaxDebtRatioPct(double maxDebtRatioPct) { this.maxDebtRatioPct = maxDebtRatioPct; }
    }

    private final Map<String, RuleSetting> ruleSettings = new LinkedHashMap<>();
    private QualityGateConfig qualityGate = new QualityGateConfig();

    /**
     * Load rules configuration from file or use defaults.
     */
    public static RulesConfig load(String cliPath) {
        RulesConfig config = new RulesConfig();

        // Try to load from file
        Path rulesPath = null;
        if (cliPath != null && !cliPath.isEmpty()) {
            rulesPath = Paths.get(cliPath);
        } else if (Files.exists(Paths.get("rules.json"))) {
            rulesPath = Paths.get("rules.json");
        } else {
            String userHome = System.getProperty("user.home");
            Path homeConfig = Paths.get(userHome, ".java-source-analyzer", "rules.json");
            if (Files.exists(homeConfig)) {
                rulesPath = homeConfig;
            }
        }

        if (rulesPath != null && Files.exists(rulesPath)) {
            try {
                String content = new String(Files.readAllBytes(rulesPath), StandardCharsets.UTF_8);
                Gson gson = new Gson();
                Map<String, Object> json = gson.fromJson(content, Map.class);

                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> rules = (Map<String, Map<String, Object>>) json.get("rules");
                if (rules != null) {
                    for (Map.Entry<String, Map<String, Object>> entry : rules.entrySet()) {
                        RuleSetting setting = new RuleSetting();
                        Map<String, Object> map = entry.getValue();
                        if (map.containsKey("enabled")) setting.setEnabled((Boolean) map.get("enabled"));
                        if (map.containsKey("severity")) setting.setSeverity((String) map.get("severity"));
                        if (map.containsKey("threshold")) setting.setThreshold(((Number) map.get("threshold")).intValue());
                        config.ruleSettings.put(entry.getKey(), setting);
                    }
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> qg = (Map<String, Object>) json.get("quality_gate");
                if (qg != null) {
                    if (qg.containsKey("max_critical")) config.qualityGate.setMaxCritical(((Number) qg.get("max_critical")).intValue());
                    if (qg.containsKey("max_major")) config.qualityGate.setMaxMajor(((Number) qg.get("max_major")).intValue());
                    if (qg.containsKey("max_total")) config.qualityGate.setMaxTotal(((Number) qg.get("max_total")).intValue());
                    if (qg.containsKey("max_debt_ratio_pct")) config.qualityGate.setMaxDebtRatioPct(((Number) qg.get("max_debt_ratio_pct")).doubleValue());
                }

                System.out.println("✅ Rules config loaded from: " + rulesPath);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to load rules config: " + e.getMessage());
            }
        } else {
            System.out.println("ℹ️  No rules.json found - using defaults");
        }

        return config;
    }

    /**
     * Check if a rule is enabled.
     */
    public boolean isRuleEnabled(String ruleKey) {
        RuleSetting setting = ruleSettings.get(ruleKey);
        return setting == null || setting.isEnabled();
    }

    /**
     * Get effective severity for a rule.
     */
    public String getEffectiveSeverity(String ruleKey, String defaultSeverity) {
        RuleSetting setting = ruleSettings.get(ruleKey);
        if (setting != null && setting.getSeverity() != null) {
            return setting.getSeverity();
        }
        return defaultSeverity;
    }

    /**
     * Get effective threshold for a rule.
     */
    public int getEffectiveThreshold(String ruleKey, int defaultThreshold) {
        RuleSetting setting = ruleSettings.get(ruleKey);
        if (setting != null && setting.getThreshold() != null) {
            return setting.getThreshold();
        }
        return defaultThreshold;
    }

    /**
     * Get quality gate configuration.
     */
    public QualityGateConfig getQualityGate() {
        return qualityGate;
    }

    /**
     * Get all rule settings.
     */
    public Map<String, RuleSetting> getRuleSettings() {
        return Collections.unmodifiableMap(ruleSettings);
    }
}
