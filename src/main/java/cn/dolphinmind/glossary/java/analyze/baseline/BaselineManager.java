package cn.dolphinmind.glossary.java.analyze.baseline;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Baseline system for false positive marking.
 *
 * Users can mark specific issues as:
 * - FALSE_POSITIVE: The rule is incorrectly flagging this code
 * - WONT_FIX: The issue is valid but won't be fixed (design decision)
 *
 * Baseline is stored in .universe/baseline.json
 *
 * Format:
 * {
 *   "issues": {
 *     "rule_key|class_name|method_name|message_prefix": {
 *       "status": "FALSE_POSITIVE",
 *       "reason": "This is intentional for performance reasons",
 *       "marked_by": "user",
 *       "marked_date": "2026-04-09"
 *     }
 *   }
 * }
 */
public class BaselineManager {

    public enum BaselineStatus {
        FALSE_POSITIVE,
        WONT_FIX
    }

    public static class BaselineEntry {
        private String status;  // FALSE_POSITIVE or WONT_FIX
        private String reason;
        private String markedBy;
        private String markedDate;

        public BaselineEntry() {}
        public BaselineEntry(String status, String reason) {
            this.status = status;
            this.reason = reason;
            this.markedBy = "user";
            this.markedDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getMarkedBy() { return markedBy; }
        public void setMarkedBy(String markedBy) { this.markedBy = markedBy; }
        public String getMarkedDate() { return markedDate; }
        public void setMarkedDate(String markedDate) { this.markedDate = markedDate; }
    }

    public static class BaselineData {
        private final Map<String, BaselineEntry> issues = new LinkedHashMap<>();

        public Map<String, BaselineEntry> getIssues() { return issues; }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Load baseline from project's .universe directory.
     */
    public static BaselineData load(Path projectRoot) {
        Path baselineFile = projectRoot.resolve(".universe").resolve("baseline.json");
        if (Files.exists(baselineFile)) {
            try {
                String content = new String(Files.readAllBytes(baselineFile), StandardCharsets.UTF_8);
                return GSON.fromJson(content, BaselineData.class);
            } catch (Exception e) {
                return new BaselineData();
            }
        }
        return new BaselineData();
    }

    /**
     * Save baseline to project's .universe directory.
     */
    public static void save(BaselineData baseline, Path projectRoot) throws IOException {
        Path baselineDir = projectRoot.resolve(".universe");
        Files.createDirectories(baselineDir);
        Path baselineFile = baselineDir.resolve("baseline.json");
        String json = GSON.toJson(baseline);
        Files.write(baselineFile, json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate issue key for baseline lookup.
     */
    public static String issueKey(String ruleKey, String className, String methodName, String message) {
        String msgPrefix = message.length() > 50 ? message.substring(0, 50) : message;
        return ruleKey + "|" + className + "|" + methodName + "|" + msgPrefix;
    }

    /**
     * Filter out baseline-marked issues from the results.
     * Returns list of non-baseline issues.
     */
    public static List<Map<String, Object>> filterBaseline(
            List<Map<String, Object>> issues, BaselineData baseline) {
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (Map<String, Object> issue : issues) {
            String ruleKey = (String) issue.getOrDefault("rule_key", "");
            String className = (String) issue.getOrDefault("class", "");
            String methodName = (String) issue.getOrDefault("method", "");
            String message = (String) issue.getOrDefault("message", "");

            String key = issueKey(ruleKey, className, methodName, message);
            if (!baseline.getIssues().containsKey(key)) {
                filtered.add(issue);
            }
        }

        return filtered;
    }

    /**
     * Add a baseline entry to mark an issue.
     */
    public static void markIssue(BaselineData baseline, String ruleKey, String className,
                                  String methodName, String message, String status, String reason) {
        String key = issueKey(ruleKey, className, methodName, message);
        BaselineEntry entry = new BaselineEntry(status, reason);
        baseline.getIssues().put(key, entry);
    }
}
