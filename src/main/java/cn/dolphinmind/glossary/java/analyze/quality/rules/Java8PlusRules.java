package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Java 8+ Features Rules
 *
 * Detects opportunities to use modern Java features:
 * - Stream API usage
 * - Optional usage
 * - Lambda expressions
 * - Method references
 * - CompletableFuture
 * - Default methods
 * - String.join
 * - Collectors
 * - forEach
 * - try-with-resources
 */
public final class Java8PlusRules {
    private Java8PlusRules() {}

    /**
     * RSPEC-13001: Use Stream API instead of loop
     */
    public static class UseStreamApi extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-13001"; }
        public String getName() { return "Use Stream API instead of manual loops"; }
        public String getCategory() { return "JAVA8_PLUS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("List<") && body.contains("for (") &&
                (body.contains(".add(") || body.contains(".remove(") || body.contains(".filter("))) {
                if (!body.contains(".stream(") && !body.contains(".parallelStream(")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Manual loop on collection", "use Stream API"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-13002: Use Optional instead of null check
     */
    public static class UseOptional extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-13002"; }
        public String getName() { return "Use Optional instead of null checks"; }
        public String getCategory() { return "JAVA8_PLUS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("return null") && (body.contains("if") && body.contains("== null"))) {
                if (!body.contains("Optional.") && !body.contains("Optional<")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Null check pattern", "use Optional"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-13003: Use method reference instead of lambda
     */
    public static class UseMethodReference extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-13003"; }
        public String getName() { return "Use method reference instead of lambda"; }
        public String getCategory() { return "JAVA8_PLUS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("x -> x.") || body.contains("e -> e.") || body.contains("s -> s.")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Lambda can be replaced with method reference", "use ::"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-13004: Use String.join
     */
    public static class UseStringJoin extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-13004"; }
        public String getName() { return "Use String.join()"; }
        public String getCategory() { return "JAVA8_PLUS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("StringBuilder") && body.contains(".append(") && body.contains("\", \"")) {
                if (!body.contains("String.join") && !body.contains("Collectors.joining")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Manual string join", "use String.join()"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-13005: Use forEach instead of for loop
     */
    public static class UseForEach extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-13005"; }
        public String getName() { return "Use forEach() instead of for loop"; }
        public String getCategory() { return "JAVA8_PLUS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("for (") && body.contains(".get(") && body.contains(".size()")) {
                if (!body.contains(".forEach(") && !body.contains(".iterator(")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Index-based loop", "use forEach or iterator"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-13006: Use Collectors.toMap
     */
    public static class UseCollectorsToMap extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-13006"; }
        public String getName() { return "Use Collectors.toMap()"; }
        public String getCategory() { return "JAVA8_PLUS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new HashMap<>()") && body.contains("for (") && body.contains(".put(")) {
                if (!body.contains("Collectors.toMap") && !body.contains(".stream(")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Manual map construction", "use Collectors.toMap()"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-13007: Use try-with-resources
     */
    public static class UseTryWithResources extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-13007"; }
        public String getName() { return "Use try-with-resources"; }
        public String getCategory() { return "JAVA8_PLUS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if ((body.contains("InputStream") || body.contains("OutputStream") ||
                 body.contains("Reader") || body.contains("Writer") || body.contains("Connection")) &&
                body.contains("new ") && body.contains("catch") && !body.contains("try (")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Resource not using try-with-resources", "use try-with-resources"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-13008: Use Optional.map
     */
    public static class UseOptionalMap extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-13008"; }
        public String getName() { return "Use Optional.map()"; }
        public String getCategory() { return "JAVA8_PLUS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("Optional.") && body.contains(".isPresent()") && body.contains(".get()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "isPresent/get pattern", "use Optional.map()"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-13009: Use Arrays.asList instead of manual list creation
     */
    public static class UseArraysAsList extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-13009"; }
        public String getName() { return "Use Arrays.asList() or List.of()"; }
        public String getCategory() { return "JAVA8_PLUS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new ArrayList<>()") && body.contains(".add(")) {
                if (body.split("\\.add\\(").length <= 5) {
                    if (!body.contains("Arrays.asList") && !body.contains("List.of")) {
                        issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                            "Manual list creation", "use Arrays.asList or List.of"));
                    }
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-13010: Use CompletableFuture for async
     */
    public static class UseCompletableFuture extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-13010"; }
        public String getName() { return "Use CompletableFuture for async operations"; }
        public String getCategory() { return "JAVA8_PLUS"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new Thread(") || body.contains("ExecutorService")) {
                if (!body.contains("CompletableFuture") && !body.contains(".thenApply(") && !body.contains(".thenAccept(")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Manual thread management", "use CompletableFuture"));
                }
            }
            return issues;
        }
    }
}
