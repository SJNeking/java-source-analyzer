package cn.dolphinmind.glossary.java.analyze.unified;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

/**
 * 结果合并器 CLI 工具
 * 
 * 用法:
 *   java -cp analyzer.jar cn.dolphinmind.glossary.java.analyze.unified.ResultMerger \
 *     --static static-results.json \
 *     --ai ai-review.json \
 *     --output unified-report.json
 * 
 * 功能:
 *   1. 读取静态分析结果 (Java Source Analyzer 输出)
 *   2. 读取 AI 审查结果 (CodeGuardian AI 输出, 可选)
 *   3. 按 (filePath:line) 坐标对齐合并
 *   4. 输出 unified-report.json (可直接喂给前端)
 */
public class ResultMerger {

    private static final Logger logger = Logger.getLogger(ResultMerger.class.getName());

    private String staticInputPath;
    private String aiInputPath;
    private String outputPath;
    private String projectName;
    private String commitSha;
    private String branch;
    private String aiModel;
    private String sourceRoot;
    private double confidenceThreshold = 0.5;

    public static void main(String[] args) {
        ResultMerger merger = new ResultMerger();
        try {
            merger.parseArgs(args);
            merger.validate();
            UnifiedReport report = merger.merge();
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            String json = gson.toJson(report.toMap());
            if (merger.outputPath != null) {
                PrintWriter pw = new PrintWriter(merger.outputPath, "UTF-8");
                pw.write(json);
                pw.close();
                System.out.println("OK: " + merger.outputPath);
            } else {
                System.out.println(json);
            }
            printSummary(report);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    public UnifiedReport merge() throws IOException {
        long startTime = System.currentTimeMillis();

        JsonObject staticData = readJson(staticInputPath);
        List<UnifiedIssue> staticIssues = extractStaticIssues(staticData);
        System.out.println("Static issues: " + staticIssues.size());

        // Generate RAG context if source root provided
        Map<String, Object> ragContext = null;
        if (sourceRoot != null) {
            try {
                Gson gson2 = new Gson();
                Map<String, Object> staticMap = gson2.fromJson(staticData.toString(), Map.class);
                cn.dolphinmind.glossary.java.analyze.slicing.CodeSlicer slicer =
                    new cn.dolphinmind.glossary.java.analyze.slicing.CodeSlicer();
                ragContext = slicer.buildRagContext(java.nio.file.Paths.get(sourceRoot), staticMap);
                System.out.println("RAG context: " + ragContext.get("totalSlices") + " slices, "
                        + ragContext.get("totalEstimatedTokens") + " tokens");
            } catch (Exception e) {
                System.err.println("Warning: RAG context generation failed: " + e.getMessage());
            }
        }

        List<UnifiedIssue> aiIssues = new ArrayList<UnifiedIssue>();
        JsonObject aiData = null;
        if (aiInputPath != null) {
            aiData = readJson(aiInputPath);
            aiIssues = extractAiIssues(aiData);
            System.out.println("AI issues: " + aiIssues.size());
        }

        UnifiedReport report;
        if (!aiIssues.isEmpty()) {
            report = UnifiedReport.merge(staticIssues, aiIssues);
        } else {
            report = UnifiedReport.fromStaticAnalysis(staticIssues);
        }

        report.setProjectName(projectName != null ? projectName :
                getStr(staticData, "framework", "unknown"));
        report.setProjectVersion(getStr(staticData, "version", "0.0.0"));
        report.setCommitSha(commitSha != null ? commitSha : "unknown");
        report.setBranch(branch != null ? branch : "unknown");
        report.setAnalysisDurationMs(System.currentTimeMillis() - startTime);
        report.setRagContext(ragContext);

        UnifiedReport.EngineInfo se =
                new UnifiedReport.EngineInfo("Java Source Analyzer", "1.0.0");
        report.setStaticEngine(se);

        if (!aiIssues.isEmpty() && aiData != null) {
            UnifiedReport.EngineInfo ae =
                    new UnifiedReport.EngineInfo("CodeGuardian AI",
                            getStr(aiData, "version", "1.0.0"));
            ae.setModelUsed(aiModel != null ? aiModel : getStr(aiData, "model", "unknown"));
            report.setAiEngine(ae);
        }
        return report;
    }

    private List<UnifiedIssue> extractStaticIssues(JsonObject data) {
        List<UnifiedIssue> issues = new ArrayList<UnifiedIssue>();
        if (!data.has("quality_issues")) return issues;
        JsonArray arr = data.getAsJsonArray("quality_issues");
        for (int i = 0; i < arr.size(); i++) {
            JsonObject m = arr.get(i).getAsJsonObject();
            String filePath = getStr(m, "file", "");
            if (filePath.startsWith("/")) {
                int idx = filePath.indexOf("/src/main/java/");
                if (idx > 0) filePath = filePath.substring(idx + 1);
            }
            String methodName = getStr(m, "method");
            issues.add(UnifiedIssue.builder()
                    .source(IssueSource.STATIC)
                    .ruleKey(getStr(m, "rule_key", ""))
                    .ruleName(getStr(m, "rule_name", ""))
                    .severity(getStr(m, "severity", "MINOR"))
                    .category(getStr(m, "category", "CODE_SMELL"))
                    .filePath(filePath)
                    .className(getStr(m, "class", ""))
                    .methodName(methodName)
                    .line(getInt(m, "line", 0))
                    .message(getStr(m, "message", ""))
                    .confidence(1.0)
                    .build());
        }
        return issues;
    }

    private List<UnifiedIssue> extractAiIssues(JsonObject data) {
        List<UnifiedIssue> issues = new ArrayList<UnifiedIssue>();
        JsonArray arr = null;
        if (data.has("issues")) arr = data.getAsJsonArray("issues");
        else if (data.has("review_results")) arr = data.getAsJsonArray("review_results");
        if (arr == null) return issues;
        for (int i = 0; i < arr.size(); i++) {
            JsonObject m = arr.get(i).getAsJsonObject();
            String filePath = getStr(m, "filePath", getStr(m, "file_path", ""));
            if (filePath.startsWith("/")) {
                int idx = filePath.indexOf("/src/main/java/");
                if (idx > 0) filePath = filePath.substring(idx + 1);
            }
            Double confidence = getDouble(m, "confidence");
            boolean autoFiltered = confidence != null && confidence < confidenceThreshold;
            issues.add(UnifiedIssue.builder()
                    .source(IssueSource.AI)
                    .ruleKey(getStr(m, "ruleKey", getStr(m, "rule_key", "AI_GENERAL")))
                    .ruleName(getStr(m, "ruleName", getStr(m, "rule_name", "AI Review")))
                    .severity(getStr(m, "severity", "MINOR"))
                    .category(getStr(m, "category", "DESIGN"))
                    .filePath(filePath)
                    .className(getStr(m, "className", getStr(m, "class_name")))
                    .methodName(getStr(m, "methodName", getStr(m, "method_name")))
                    .line(getInt(m, "line", getInt(m, "line_number", 0)))
                    .message(getStr(m, "message", ""))
                    .confidence(confidence)
                    .aiSuggestion(getStr(m, "aiSuggestion", getStr(m, "suggestion")))
                    .aiFixedCode(getStr(m, "aiFixedCode", getStr(m, "fixed_code")))
                    .aiReasoning(getStr(m, "aiReasoning", getStr(m, "reasoning")))
                    .aiModel(aiModel)
                    .autoFiltered(autoFiltered)
                    .build());
        }
        return issues;
    }

    private JsonObject readJson(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return JsonParser.parseString(sb.toString()).getAsJsonObject();
        } finally {
            br.close();
        }
    }

    private static void printSummary(UnifiedReport report) {
        UnifiedReport.Summary s = report.getSummary();
        if (s == null) return;
        System.out.println();
        System.out.println("=== Merge Report Summary ===");
        System.out.println("Project: " + report.getProjectName());
        System.out.println("Branch:  " + report.getBranch());
        System.out.println("Total issues:     " + s.totalIssues);
        System.out.println("Active issues:    " + s.activeIssues);
        System.out.println("Auto-filtered:    " + s.autoFilteredIssues);
        System.out.println("Static only:      " + s.staticOnlyIssues);
        System.out.println("AI only:          " + s.aiOnlyIssues);
        System.out.println("Merged:           " + s.mergedIssues);
        System.out.println("CRITICAL: " + s.criticalCount);
        System.out.println("MAJOR:    " + s.majorCount);
        System.out.println("MINOR:    " + s.minorCount);
        System.out.println("INFO:     " + s.infoCount);
        if (s.aiHighConfidenceRate > 0) {
            System.out.println("AI avg confidence: " + s.aiAverageConfidence);
            System.out.println("AI high conf rate: " + s.aiHighConfidenceRate + "%");
        }
        System.out.println("===========================");
    }

    private void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--static".equals(arg) && i + 1 < args.length) {
                staticInputPath = args[++i];
            } else if ("--ai".equals(arg) && i + 1 < args.length) {
                aiInputPath = args[++i];
            } else if ("--output".equals(arg) && i + 1 < args.length) {
                outputPath = args[++i];
            } else if ("-o".equals(arg) && i + 1 < args.length) {
                outputPath = args[++i];
            } else if ("--project".equals(arg) && i + 1 < args.length) {
                projectName = args[++i];
            } else if ("--commit".equals(arg) && i + 1 < args.length) {
                commitSha = args[++i];
            } else if ("--branch".equals(arg) && i + 1 < args.length) {
                branch = args[++i];
            } else if ("--sourceRoot".equals(arg) && i + 1 < args.length) {
                sourceRoot = args[++i];
            } else if ("--model".equals(arg) && i + 1 < args.length) {
                aiModel = args[++i];
            } else if ("--threshold".equals(arg) && i + 1 < args.length) {
                confidenceThreshold = Double.parseDouble(args[++i]);
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printUsage();
                System.exit(0);
            }
        }
    }

    private void validate() {
        if (staticInputPath == null) throw new IllegalArgumentException("--static required");
        if (!new java.io.File(staticInputPath).exists())
            throw new IllegalArgumentException("File not found: " + staticInputPath);
        if (aiInputPath != null && !new java.io.File(aiInputPath).exists())
            throw new IllegalArgumentException("File not found: " + aiInputPath);
    }

    private static void printUsage() {
        System.out.println("Usage: java ResultMerger [options]");
        System.out.println("Required: --static <file>       Static analysis JSON");
        System.out.println("Optional: --ai <file>           AI review JSON");
        System.out.println("          --output, -o <f>      Output path (default: stdout)");
        System.out.println("          --project <name>      Project name");
        System.out.println("          --commit <sha>        Git commit SHA");
        System.out.println("          --branch <name>       Git branch");
        System.out.println("          --sourceRoot <path>   Java source root (for RAG slicing)");
        System.out.println("          --model <name>        AI model name");
        System.out.println("          --threshold <n>       Confidence threshold (default 0.5)");
        System.out.println("          --help, -h            Show help");
    }

    private static String getStr(JsonObject m, String key) {
        return m.has(key) && !m.get(key).isJsonNull() ? m.get(key).getAsString() : null;
    }
    private static String getStr(JsonObject m, String key, String def) {
        String v = getStr(m, key); return v != null ? v : def;
    }
    private static int getInt(JsonObject m, String key, int def) {
        return m.has(key) ? m.get(key).getAsInt() : def;
    }
    private static Double getDouble(JsonObject m, String key) {
        return m.has(key) && !m.get(key).isJsonNull() ? m.get(key).getAsDouble() : null;
    }
}
