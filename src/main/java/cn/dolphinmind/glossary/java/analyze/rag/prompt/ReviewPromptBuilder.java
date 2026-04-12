package cn.dolphinmind.glossary.java.analyze.rag.prompt;

import cn.dolphinmind.glossary.java.analyze.rag.model.RagSlice;
import java.util.*;

/**
 * 审查提示词构建器
 * 
 * 构建系统提示词和用户提示词, 引导 LLM 输出结构化审查结果
 */
public class ReviewPromptBuilder {

    /**
     * 系统提示词 — 定义角色、格式、约束
     */
    public static String buildSystemPrompt() {
        return "You are an expert Java code review assistant. " +
               "Analyze the provided code slices and identify potential issues. " +
               "Your response MUST be valid JSON with the following structure:\n\n" +
               "{\n" +
               "  \"issues\": [\n" +
               "    {\n" +
               "      \"ruleKey\": \"AI_RULE_001\",\n" +
               "      \"ruleName\": \"Short description of the issue\",\n" +
               "      \"severity\": \"CRITICAL|MAJOR|MINOR|INFO\",\n" +
               "      \"category\": \"BUG|CODE_SMELL|SECURITY|DESIGN|PERFORMANCE\",\n" +
               "      \"filePath\": \"relative/file/path.java\",\n" +
               "      \"className\": \"com.example.ClassName\",\n" +
               "      \"methodName\": \"methodName\",\n" +
               "      \"line\": 42,\n" +
               "      \"message\": \"Clear description of the problem\",\n" +
               "      \"confidence\": 0.85,\n" +
               "      \"aiSuggestion\": \"How to fix this issue\",\n" +
               "      \"aiFixedCode\": \"```java\\n// fixed code here\\n```\",\n" +
               "      \"aiReasoning\": \"Why this is a problem and why the fix works\"\n" +
               "    }\n" +
               "  ]\n" +
               "}\n\n" +
               "Rules:\n" +
               "- Only report real issues. Do not report false positives.\n" +
               "- If no issues found, return {\"issues\": []}\n" +
               "- confidence must be between 0.0 and 1.0\n" +
               "- severity must be one of: CRITICAL, MAJOR, MINOR, INFO\n" +
               "- Only respond with valid JSON. No markdown, no explanation.";
    }

    /**
     * 用户提示词 — 提供代码上下文和具体审查要求
     */
    public static String buildUserPrompt(List<RagSlice> slices, String additionalContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("Review the following Java code slices:\n\n");

        for (int i = 0; i < slices.size(); i++) {
            RagSlice slice = slices.get(i);
            sb.append("=== Slice ").append(i + 1).append(" ===\n");
            if (slice.getClassName() != null) {
                sb.append("Class: ").append(slice.getClassName()).append("\n");
            }
            if (slice.getMethodName() != null) {
                sb.append("Method: ").append(slice.getMethodName()).append("\n");
            }
            sb.append("File: ").append(slice.getFilePath()).append("\n");
            sb.append("Lines: ").append(slice.getStartLine()).append("-").append(slice.getEndLine()).append("\n");

            Map<String, Object> meta = slice.getMetadata();
            if (meta.containsKey("issueRule")) {
                sb.append("Static Analysis Rule: ").append(meta.get("issueRule")).append("\n");
            }
            if (meta.containsKey("issueMessage")) {
                sb.append("Static Analysis Message: ").append(meta.get("issueMessage")).append("\n");
            }
            if (meta.containsKey("issueSeverity")) {
                sb.append("Static Analysis Severity: ").append(meta.get("issueSeverity")).append("\n");
            }

            sb.append("\n```java\n").append(slice.getCode()).append("\n```\n\n");
        }

        if (additionalContext != null && !additionalContext.isEmpty()) {
            sb.append("\nAdditional context:\n").append(additionalContext).append("\n");
        }

        sb.append("\nPlease analyze these code slices and report any issues found. " +
                  "Respond with JSON only.");

        return sb.toString();
    }

    /**
     * 构建 RAG 检索查询
     */
    public static String buildRetrievalQuery(String issueDescription) {
        if (issueDescription == null || issueDescription.isEmpty()) return "";

        // 提取关键词
        return issueDescription
                .replaceAll("[^a-zA-Z0-9_\\s]", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    /**
     * 构建代码摘要查询
     */
    public static String buildSummaryQuery(String className, String methodName) {
        StringBuilder sb = new StringBuilder();
        if (className != null) {
            String shortName = className.contains(".") ? className.substring(className.lastIndexOf('.') + 1) : className;
            sb.append(shortName).append(" ");
        }
        if (methodName != null) {
            sb.append(methodName);
        }
        return sb.toString().trim();
    }
}
