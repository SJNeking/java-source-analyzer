package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Spring Boot Rules - P5 Priority
 *
 * Detects Spring Boot specific anti-patterns:
 * - Missing @RestController
 * - Redundant @Autowired
 * - Transactional on private methods
 * - Generic Exception handling
 */
public final class SpringBootRules {
    private SpringBootRules() {}

    // =====================================================================
    // Controller/Web
    // =====================================================================

    /**
     * RSPEC-3001: @Controller should be @RestController
     * Detects @Controller used without @ResponseBody.
     */
    public static class MissingRestController extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3001"; }
        public String getName() { return "@Controller should be @RestController if returning data"; }
        public String getCategory() { return "SPRING_BOOT"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Check if class has @Controller but not @RestController or @ResponseBody
            // Simplified check based on body content if @Controller is present
            if (body.contains("@Controller") && !body.contains("@RestController") && !body.contains("@ResponseBody")) {
                // This is a class-level check, but we can flag methods that return data
                if (body.contains("return") && !body.contains("void")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "@Controller returns data without @ResponseBody", "missing @ResponseBody"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-3002: @RequestMapping without method
     * Detects @RequestMapping used without specifying HTTP method.
     */
    public static class RequestMappingWithoutMethod extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3002"; }
        public String getName() { return "@RequestMapping should specify HTTP method"; }
        public String getCategory() { return "SPRING_BOOT"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("@RequestMapping") && !body.contains("method =") &&
                !body.contains("@GetMapping") && !body.contains("@PostMapping")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "@RequestMapping without HTTP method", "ambiguous mapping"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-3003: Generic Exception in @ExceptionHandler
     * Detects handling generic Exception.
     */
    public static class GenericExceptionHandler extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3003"; }
        public String getName() { return "@ExceptionHandler should not handle generic Exception"; }
        public String getCategory() { return "SPRING_BOOT"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("@ExceptionHandler") && body.contains("Exception.class") && !body.contains(".class")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Generic Exception handler", "handle specific exceptions"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Dependency Injection
    // =====================================================================

    /**
     * RSPEC-3004: Redundant @Autowired on single constructor
     * Detects @Autowired on constructor when it's the only one.
     */
    public static class RedundantAutowired extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3004"; }
        public String getName() { return "@Autowired is redundant on single constructor"; }
        public String getCategory() { return "SPRING_BOOT"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // If this is a constructor
            if (name != null && name.equals("init")) { // Simplified check, real check needs class context
                // Check if it has @Autowired
                if (body.contains("@Autowired")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Redundant @Autowired on constructor", "Spring 4.3+ infers constructor injection"));
                }
            }
            return issues;
        }
    }

    // =====================================================================
    // Transaction Management
    // =====================================================================

    /**
     * RSPEC-3005: @Transactional on private method
     * Detects @Transactional on private methods (ignored by Spring).
     */
    public static class TransactionalOnPrivate extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3005"; }
        public String getName() { return "@Transactional should not be used on private methods"; }
        public String getCategory() { return "SPRING_BOOT"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            @SuppressWarnings("unchecked")
            List<String> mods = (List<String>) m.getOrDefault("modifiers", Collections.emptyList());

            if (body.contains("@Transactional") && mods.contains("private")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "@Transactional on private method (ignored by proxy)", "private transactional"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Configuration
    // =====================================================================

    /**
     * RSPEC-3006: @Configuration class should be final
     * Detects non-final configuration classes.
     */
    public static class ConfigurationNotFinal extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3006"; }
        public String getName() { return "@Configuration classes should be final"; }
        public String getCategory() { return "SPRING_BOOT"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("@Configuration") && !body.contains("final class")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "@Configuration class not final", "proxy issues"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-3007: Missing @ConditionalOnProperty
     * Detects configuration classes without conditional loading.
     */
    public static class MissingConditionalOnProperty extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3007"; }
        public String getName() { return "Configuration should be conditional"; }
        public String getCategory() { return "SPRING_BOOT"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("@Configuration") && !body.contains("@ConditionalOnProperty")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Configuration without @ConditionalOnProperty", "always loaded"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Web Parameters
    // =====================================================================

    /**
     * RSPEC-3008: @PathVariable missing in method signature
     * Detects URL params not mapped to @PathVariable.
     */
    public static class MissingPathVariable extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3008"; }
        public String getName() { return "URL path variables should be mapped"; }
        public String getCategory() { return "SPRING_BOOT"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Check for {var} in mapping but no @PathVariable
            if (body.contains("{") && body.contains("}") && body.contains("@RequestMapping") || body.contains("@GetMapping")) {
                if (!body.contains("@PathVariable")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "URL path variable not mapped", "missing @PathVariable"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-3009: @RequestParam required missing
     * Detects optional request params without required=false.
     */
    public static class MissingRequestParamRequired extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3009"; }
        public String getName() { return "Optional request params should specify required=false"; }
        public String getCategory() { return "SPRING_BOOT"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("@RequestParam") && body.contains("Optional<") && !body.contains("required = false")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Optional param without required=false", "explicit optional"));
            }
            return issues;
        }
    }

    // =====================================================================
    // Bean Management
    // =====================================================================

    /**
     * RSPEC-3010: @Bean without name
     * Detects beans without explicit names.
     */
    public static class BeanWithoutName extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3010"; }
        public String getName() { return "@Bean should have explicit name"; }
        public String getCategory() { return "SPRING_BOOT"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("@Bean") && !body.contains("@Bean(\"") && !body.contains("@Bean(name")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "@Bean without explicit name", "implicit bean name"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-3011: Service interface implementation
     * Detects @Service on interface (should be on implementation).
     */
    public static class ServiceOnInterface extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3011"; }
        public String getName() { return "@Service should not be used on interfaces"; }
        public String getCategory() { return "SPRING_BOOT"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // This is a class-level check, but we can check if the method belongs to an interface
            // Simplified: check if class name implies interface (ends with Interface) or has interface methods
            // This is a heuristic.
            return issues; // Requires class-level context for accuracy
        }
    }

    /**
     * RSPEC-3012: ComponentScan default package
     * Detects ComponentScan without basePackages.
     */
    public static class DefaultComponentScan extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-3012"; }
        public String getName() { return "@ComponentScan should specify basePackages"; }
        public String getCategory() { return "SPRING_BOOT"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            if (body.contains("@ComponentScan") && !body.contains("basePackages") && !body.contains("basePackageClasses")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "ComponentScan without basePackages", "default scan"));
            }
            return issues;
        }
    }
}
