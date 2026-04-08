package cn.dolphinmind.glossary.java.analyze.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Generates SARIF (Static Analysis Results Interchange Format) output.
 * This is the industry standard format supported by:
 * - GitHub Code Scanning
 * - Azure DevOps
 * - Visual Studio / VS Code
 * - JetBrains IDEs
 * - SonarQube import
 *
 * SARIF 2.1.0 specification: https://docs.oasis-open.org/sarif/sarif/v2.1.0/
 */
public class SarifGenerator {

    private final Map<String, Object> scanResult;
    private final String frameworkName;
    private final String version;
    private final String scanDate;

    public SarifGenerator(Map<String, Object> scanResult) {
        this.scanResult = scanResult;
        this.frameworkName = (String) scanResult.getOrDefault("framework", "Unknown");
        this.version = (String) scanResult.getOrDefault("version", "unknown");
        this.scanDate = (String) scanResult.getOrDefault("scan_date", "unknown");
    }

    /**
     * Generate SARIF JSON and write to file.
     */
    public void generate(String outputPath) throws IOException {
        Map<String, Object> sarif = buildSarif();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        try (java.io.OutputStreamWriter fw = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(outputPath), StandardCharsets.UTF_8)) {
            gson.toJson(sarif, fw);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSarif() {
        List<Map<String, Object>> qualityIssues =
                (List<Map<String, Object>>) scanResult.getOrDefault("quality_issues", Collections.emptyList());

        // Build runs array
        List<Map<String, Object>> runs = new ArrayList<>();
        Map<String, Object> run = new LinkedHashMap<>();

        // Tool information
        Map<String, Object> tool = new LinkedHashMap<>();
        Map<String, Object> driver = new LinkedHashMap<>();
        driver.put("name", "Java Source Analyzer");
        driver.put("version", version);
        driver.put("informationUri", "https://github.com/java-source-analyzer");

        // Rules
        List<Map<String, Object>> rules = new ArrayList<>();
        Set<String> seenRules = new HashSet<>();

        for (Map<String, Object> issue : qualityIssues) {
            String ruleKey = (String) issue.getOrDefault("rule_key", "");
            if (seenRules.contains(ruleKey)) continue;
            seenRules.add(ruleKey);

            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("id", ruleKey);
            rule.put("name", issue.getOrDefault("rule_name", ruleKey));
            rule.put("shortDescription", new LinkedHashMap<String, String>() {{
                put("text", (String) issue.getOrDefault("message", ""));
            }});

            String severity = (String) issue.getOrDefault("severity", "INFO");
            String sarifLevel = toSarifLevel(severity);
            rule.put("defaultConfiguration", new LinkedHashMap<String, String>() {{
                put("level", sarifLevel);
            }});

            rules.add(rule);
        }

        driver.put("rules", rules);
        tool.put("driver", driver);
        run.put("tool", tool);

        // Results
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> issue : qualityIssues) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ruleId", issue.getOrDefault("rule_key", ""));

            String severity = (String) issue.getOrDefault("severity", "INFO");
            result.put("level", toSarifLevel(severity));

            // Message
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("text", issue.getOrDefault("message", ""));
            result.put("message", message);

            // Locations
            String filePath = (String) issue.getOrDefault("file", "");
            int line = ((Number) issue.getOrDefault("line", 0)).intValue();

            if (!filePath.isEmpty() && line > 0) {
                Map<String, Object> location = new LinkedHashMap<>();
                Map<String, Object> physicalLocation = new LinkedHashMap<>();

                Map<String, Object> artifactLocation = new LinkedHashMap<>();
                artifactLocation.put("uri", filePath);
                physicalLocation.put("artifactLocation", artifactLocation);

                Map<String, Object> region = new LinkedHashMap<>();
                region.put("startLine", line);
                physicalLocation.put("region", region);

                location.put("physicalLocation", physicalLocation);
                result.put("locations", Collections.singletonList(location));
            }

            results.add(result);
        }

        run.put("results", results);
        runs.add(run);

        // Top-level SARIF
        Map<String, Object> sarif = new LinkedHashMap<>();
        sarif.put("$schema", "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json");
        sarif.put("version", "2.1.0");
        sarif.put("runs", runs);

        return sarif;
    }

    private String toSarifLevel(String severity) {
        switch (severity) {
            case "CRITICAL": return "error";
            case "MAJOR": return "warning";
            case "MINOR": return "note";
            default: return "none";
        }
    }
}
