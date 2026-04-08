package cn.dolphinmind.glossary.java.analyze.quality;

import java.util.*;

/**
 * Detects duplicated code blocks using token-based analysis
 */
public class DuplicateCodeDetector {

    private static final int MIN_TOKEN_SEQUENCE = 10; // minimum tokens to consider as duplicate

    /**
     * Find duplicated code blocks across all class assets
     */
    public List<QualityIssue> findDuplicates(List<Map<String, Object>> classAssets) {
        List<QualityIssue> issues = new ArrayList<>();
        Map<String, List<TokenLocation>> tokenIndex = new HashMap<>();

        for (Map<String, Object> classAsset : classAssets) {
            String className = (String) classAsset.getOrDefault("address", "");
            String filePath = (String) classAsset.getOrDefault("source_file", "");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> methods = (List<Map<String, Object>>)
                    classAsset.getOrDefault("methods_full", Collections.emptyList());

            for (Map<String, Object> method : methods) {
                String bodyCode = (String) method.getOrDefault("body_code", "");
                String methodName = (String) method.get("name");
                int line = (int) method.getOrDefault("line_start", 0);

                if (bodyCode == null || bodyCode.isEmpty()) continue;

                List<String> tokens = tokenize(bodyCode);
                for (int i = 0; i <= tokens.size() - MIN_TOKEN_SEQUENCE; i++) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = i; j < i + MIN_TOKEN_SEQUENCE && j < tokens.size(); j++) {
                        sb.append(tokens.get(j)).append(" ");
                    }
                    String tokenSeq = sb.toString().trim();

                    TokenLocation loc = new TokenLocation(className, filePath, methodName, line, i);
                    tokenIndex.computeIfAbsent(tokenSeq, k -> new ArrayList<>()).add(loc);
                }
            }
        }

        // Find duplicates
        Set<String> reported = new HashSet<>();
        for (Map.Entry<String, List<TokenLocation>> entry : tokenIndex.entrySet()) {
            if (entry.getValue().size() >= 2) {
                String key = entry.getValue().get(0).className + "." + entry.getValue().get(0).methodName;
                if (reported.contains(key)) continue;
                reported.add(key);

                TokenLocation first = entry.getValue().get(0);
                issues.add(new QualityIssue(
                        "RSPEC-888", "Duplicated code block", Severity.MAJOR, "CODE_SMELL",
                        first.filePath, first.className, first.methodName, first.line,
                        "Duplicated code block found in " + entry.getValue().size() + " locations",
                        entry.getValue().size() + " occurrences"
                ));
            }
        }

        return issues;
    }

    private List<String> tokenize(String code) {
        List<String> tokens = new ArrayList<>();
        // Simple tokenizer: split on non-identifier chars, keep keywords
        String[] parts = code.replaceAll("[^a-zA-Z0-9_]", " ").split("\\s+");
        // Filter out common noise
        Set<String> noise = new HashSet<>(Arrays.asList("", " ", "return", "if", "else", "for", "while", "new", "this", "null", "true", "false"));
        for (String p : parts) {
            if (p.length() > 1 && !noise.contains(p.toLowerCase())) {
                tokens.add(p.toLowerCase());
            }
        }
        return tokens;
    }

    private static class TokenLocation {
        final String className, filePath, methodName;
        final int line, tokenOffset;

        TokenLocation(String className, String filePath, String methodName, int line, int tokenOffset) {
            this.className = className;
            this.filePath = filePath;
            this.methodName = methodName;
            this.line = line;
            this.tokenOffset = tokenOffset;
        }
    }
}
